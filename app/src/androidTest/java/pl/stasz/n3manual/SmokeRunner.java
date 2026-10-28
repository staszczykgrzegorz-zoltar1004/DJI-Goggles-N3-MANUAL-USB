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
            check("1.0.0-beta1".equals(pkg.versionName),"exact public release version");
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
