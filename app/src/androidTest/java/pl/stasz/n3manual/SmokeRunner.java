package pl.stasz.n3manual;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.graphics.Bitmap;
import android.widget.*;
import java.lang.reflect.Field;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** QA-only APK: not packaged in the public application. Tests the signed release. */
public final class SmokeRunner extends Instrumentation {
    private int passed;
    private Object field(Object target,String name) throws Exception {
        Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(target);
    }
    private void put(Object target,String name,Object value) throws Exception {
        Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(target,value);
    }
    private void check(boolean result,String name) {
        if(!result)throw new AssertionError(name);passed++;
        Bundle b=new Bundle();b.putString("stream","PASS "+name+"\n");sendStatus(0,b);
    }
    private void ui(Runnable action) {runOnMainSync(action);waitForIdleSync();}
    @Override public void onCreate(Bundle args) {super.onCreate(args);start();}
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            ApplicationInfo info=getTargetContext().getApplicationInfo();
            check((info.flags&ApplicationInfo.FLAG_DEBUGGABLE)==0,"public APK is not debuggable");
            PackageInfo pkg=getTargetContext().getPackageManager().getPackageInfo(info.packageName,PackageManager.GET_PERMISSIONS);
            check("1.0.0-beta2".equals(pkg.versionName),"exact public release version");
            boolean internet=false;
            if(pkg.requestedPermissions!=null)for(String p:pkg.requestedPermissions)if(p.equals("android.permission.INTERNET"))internet=true;
            check(!internet,"no Internet permission");
            MainActivity activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            check(!((Button)field(activity,"prepare")).isEnabled(),"restore disabled without goggles");
            check(!((Button)field(activity,"confirm")).isEnabled(),"confirm disabled without prepared screen");
            check(!((Button)field(activity,"stop")).isEnabled(),"cancel disabled while idle");
            check(!((CheckBox)field(activity,"stationary")).isChecked(),"bench confirmation is never preselected");
            check(((java.util.List<?>)field(activity,"journal")).size()==1,"launch sends no USB operation");
            check(!((boolean)field(activity,"busy")),"no background work at launch");
            AtomicBoolean cancelled=(AtomicBoolean)field(activity,"cancel");
            ui(()->{
                try {
                    put(activity,"ready",true);
                    put(activity,"prepared",new ManualWizard.Prepared(1,2,new HashMap<>(),false));
                    callActivityOnStop(activity);
                }catch(Exception e){throw new RuntimeException(e);}
            });
            check(cancelled.get(),"backgrounding cancels operation");
            check(field(activity,"prepared")==null,"backgrounding invalidates prepared Confirm");
            check(!((Button)field(activity,"confirm")).isEnabled(),"backgrounding disables stale Confirm button");
            ui(()->{callActivityOnStart(activity);callActivityOnResume(activity);});
            check(!((boolean)field(activity,"ready")),"resume does not reuse stale USB readiness");
            ui(()->{
                try {
                    put(activity,"permissionPending",true);
                    ((BroadcastReceiver)field(activity,"receiver")).onReceive(getTargetContext(),new Intent("pl.stasz.n3manual.USB_PERMISSION"));
                }catch(Exception e){throw new RuntimeException(e);}
            });
            check(!((boolean)field(activity,"permissionPending")),"permission denial clears pending request");
            check(!((Button)field(activity,"prepare")).isEnabled(),"permission denial cannot enable navigation");
            ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);
            ui(activity::recreate);
            MainActivity recreated=(MainActivity)monitor.waitForActivityWithTimeout(10000);
            check(recreated!=null&&recreated!=activity,"Android activity recreation succeeds");
            waitForIdleSync();
            check(!((Button)field(recreated,"confirm")).isEnabled(),"recreation cannot retain Confirm");
            check(!((CheckBox)field(recreated,"stationary")).isChecked(),"recreation does not silently retain bench checkbox");
            for(String tag:new String[]{"en","pl"}) {
                android.content.res.Configuration cfg=new android.content.res.Configuration(getTargetContext().getResources().getConfiguration());
                cfg.setLocale(java.util.Locale.forLanguageTag(tag));
                Context localized=getTargetContext().createConfigurationContext(cfg);
                check(localized.getString(R.string.connect).equals(tag.equals("pl")?"Połącz i sprawdź USB":"Connect and test USB"),"localized main button "+tag);
                check(localized.getString(R.string.help_body).contains(tag.equals("pl")?"śmigłami":"propellers"),"localized full help "+tag);
                check(localized.getString(R.string.error_no_permission).contains(tag.equals("pl")?"Brak":"permission"),"localized error "+tag);
            }
            MainActivity active=recreated;
            for(String mode:new String[]{"pl","en","system"}) {
                ActivityMonitor languageMonitor=addMonitor(MainActivity.class.getName(),null,false);
                MainActivity previous=active;
                ui(()->{
                    try {
                        put(previous,"ready",true);
                        put(previous,"prepared",new ManualWizard.Prepared(1,2,new HashMap<>(),false));
                        AppLanguage.set(previous,mode);
                    }catch(Exception e){throw new RuntimeException(e);}
                });
                active=(MainActivity)languageMonitor.waitForActivityWithTimeout(15000);
                check(active!=null&&active!=previous,"language recreates activity "+mode);
                waitForIdleSync();
                check(AppLanguage.choice(active).equals(mode),"language choice persisted "+mode);
                check(field(active,"prepared")==null&&!((Button)field(active,"confirm")).isEnabled(),"language clears stale Confirm "+mode);
                String expected="pl".equals(mode)?"pl":"en";
                if("system".equals(mode))expected="pl".equals(android.content.res.Resources.getSystem().getConfiguration().locale.getLanguage())?"pl":"en";
                check(active.getResources().getConfiguration().locale.getLanguage().equals(expected),"selected UI language "+mode);
            }
            android.content.res.Resources systemResources=android.content.res.Resources.getSystem();
            android.content.res.Configuration originalSystem=new android.content.res.Configuration(systemResources.getConfiguration());
            try {
                for(String systemTag:new String[]{"pl","en","de"}) {
                    android.content.res.Configuration simulated=new android.content.res.Configuration(originalSystem);
                    java.util.Locale primary=java.util.Locale.forLanguageTag(systemTag);
                    simulated.setLocale(primary);
                    if(Build.VERSION.SDK_INT>=24)simulated.setLocales(new android.os.LocaleList(primary));
                    systemResources.updateConfiguration(simulated,systemResources.getDisplayMetrics());
                    Context automatic=AppLanguage.wrap(getTargetContext());
                    String expected=systemTag.equals("pl")?"pl":"en";
                    check(automatic.getResources().getConfiguration().locale.getLanguage().equals(expected),"automatic primary language "+systemTag);
                    check(automatic.getString(R.string.connect).equals(expected.equals("pl")?"Połącz i sprawdź USB":"Connect and test USB"),"automatic resources "+systemTag);
                }
            } finally {systemResources.updateConfiguration(originalSystem,systemResources.getDisplayMetrics());}
            CoreTests.checks=0;CoreTests.main(new String[0]);
            check(CoreTests.checks>=87,"87 production protocol/wizard assertions on device runtime");
            Bitmap screenshot=getUiAutomation().takeScreenshot();
            check(screenshot!=null&&screenshot.getWidth()>0,"UI screenshot available");
            if(screenshot!=null) {
                File file=new File(getTargetContext().getExternalFilesDir(null),"qa-screen-api"+Build.VERSION.SDK_INT+".png");
                try(FileOutputStream output=new FileOutputStream(file)){screenshot.compress(Bitmap.CompressFormat.PNG,100,output);}
                result.putString("screenshot",file.getAbsolutePath());screenshot.recycle();
            }
            result.putInt("passed",passed);result.putInt("core_assertions",CoreTests.checks);
            result.putString("stream","N3_QA_PASS api="+Build.VERSION.SDK_INT+" checks="+passed+" core="+CoreTests.checks+"\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable error) {
            result.putString("stream","N3_QA_FAIL "+error.toString()+"\n");
            result.putString("failure",android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);
        }
    }
}
