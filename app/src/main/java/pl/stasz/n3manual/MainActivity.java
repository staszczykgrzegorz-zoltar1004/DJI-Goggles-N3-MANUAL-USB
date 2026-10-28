package pl.stasz.n3manual;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.usb.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

public final class MainActivity extends Activity {
    private static final String PERMISSION="pl.stasz.n3manual.USB_PERMISSION";
    private static final int BG=0xff0c1720,CARD=0xff162630,INK=0xffedf4f7,MUTED=0xffa9bbc5,MINT=0xff65dec1;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancel=new AtomicBoolean();
    private final List<Button> controls=new ArrayList<>();
    private final List<String> journal=new ArrayList<>();
    private UsbManager manager;
    private UsbDevice selected;
    private UsbAccessory selectedAccessory;
    private boolean ready,busy,permissionPending,alive=true,foreground;
    private long usbGeneration;
    private ManualWizard.Prepared prepared;
    private TextView connection,status;
    private CheckBox stationary;
    private Button connect,prepare,confirm,stop,language;
    private LinearLayout content;
    private interface Job { Object run(GuiSession ui) throws Exception; }

    private final BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override public void onReceive(Context context,Intent intent) {
            if(PERMISSION.equals(intent.getAction())) {
                boolean wanted=permissionPending; permissionPending=false;
                if(wanted&&hasUsbPermission()) probe();
                else if(wanted) status.setText(getString(R.string.permission_denied));
            } else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                UsbDevice detached=intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if(selected!=null&&detached!=null&&selected.getDeviceId()==detached.getDeviceId()) {
                    usbGeneration++;cancel.set(true);prepared=null;ready=false;selected=null;
                    connection.setText(getString(R.string.goggles_detached));status.setText(getString(R.string.usb_cancelled));updateControls();
                }
            } else if(UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(intent.getAction())) {
                usbGeneration++;cancel.set(true);prepared=null;ready=false;selectedAccessory=null;
                connection.setText(getString(R.string.dji_detached));status.setText(getString(R.string.reconnect));updateControls();
            }
        }
    };
    @Override protected void attachBaseContext(Context base) {super.attachBaseContext(AppLanguage.wrap(base));}
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);manager=(UsbManager)getSystemService(USB_SERVICE);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20),dp(18),dp(20),dp(24));scroll.addView(content);
        content.setOnApplyWindowInsetsListener((view,insets)-> {
            view.setPadding(dp(20)+insets.getSystemWindowInsetLeft(),dp(18)+insets.getSystemWindowInsetTop(),
                dp(20)+insets.getSystemWindowInsetRight(),dp(24)+insets.getSystemWindowInsetBottom());return insets;
        });
        setContentView(scroll);
        text(content,"N3 MANUAL",13,MINT,true);
        text(content,getString(R.string.title),27,INK,true);
        text(content,"DJI Goggles N3 · Neo · FPV RC3",14,MUTED,false);
        language=button(content,getString(R.string.language_button),false);
        language.setOnClickListener(v->chooseLanguage());
        LinearLayout usb=card();
        connection=text(usb,getString(R.string.connect_hint),17,INK,true);
        connect=button(usb,getString(R.string.connect),true);connect.setOnClickListener(v->connect());
        Button usbState=button(usb,getString(R.string.usb_state),false);usbState.setOnClickListener(v->diagnostics());
        text(usb,getString(R.string.usb_hint),14,MUTED,false);
        LinearLayout restore=card();
        text(restore,getString(R.string.restore),20,INK,true);
        stationary=new CheckBox(this);stationary.setText(getString(R.string.bench));
        stationary.setTextColor(INK);stationary.setTextSize(14);stationary.setButtonTintList(ColorStateList.valueOf(MINT));restore.addView(stationary);
        prepare=button(restore,getString(R.string.prepare),true);
        prepare.setOnClickListener(v->{
            if(!stationary.isChecked()) {status.setText(getString(R.string.bench_required));return;}
            prepared=null;start("prepare",ManualWizard::prepare);
        });
        text(restore,getString(R.string.confirm_hint),14,MUTED,false);
        confirm=button(restore,getString(R.string.confirm),false);
        confirm.setOnClickListener(v->{
            if(!stationary.isChecked()||prepared==null)return;
            ManualWizard.Prepared state=prepared;prepared=null;
            start("confirm",ui->{ManualWizard.confirm(ui,state);return null;});
        });
        status=text(restore,getString(R.string.initial_status),15,INK,false);
        stop=button(restore,getString(R.string.cancel),false);stop.setOnClickListener(v->{cancel.set(true);prepared=null;status.setText(getString(R.string.cancelling));});
        LinearLayout arrows=card();text(arrows,getString(R.string.remote),19,INK,true);
        LinearLayout row=new LinearLayout(this);arrows.addView(row);
        key(row,getString(R.string.left),"left");key(row,getString(R.string.up),"up");key(row,getString(R.string.right),"right");
        row=new LinearLayout(this);arrows.addView(row);
        key(row,getString(R.string.back),"back");key(row,getString(R.string.down),"down");key(row,getString(R.string.ok),"ok");
        LinearLayout extra=card();
        Button help=button(extra,getString(R.string.help),false);help.setOnClickListener(v->help());
        Button diagnostics=button(extra,getString(R.string.diagnostics),false);diagnostics.setOnClickListener(v->diagnostics());
        Button about=button(extra,getString(R.string.about),false);about.setOnClickListener(v->about());
        text(content,getString(R.string.footer,BuildConfig.VERSION_NAME),12,MUTED,false);
        IntentFilter filter=new IntentFilter(PERMISSION);filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerUsbReceiver(filter);
        log("N3 Manual "+BuildConfig.VERSION_NAME+"; Android "+Build.VERSION.RELEASE+"; "+Build.MODEL);
        refresh();
    }
    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerUsbReceiver(IntentFilter filter) {
        if(Build.VERSION.SDK_INT>=33) registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);
        // Before API33 the export flags are unavailable; the private signature
        // permission restricts delivery instead. The PendingIntent runs as this app.
        else registerReceiver(receiver,filter,"pl.stasz.n3manual.USB_INTERNAL",null);
    }
    @Override protected void onNewIntent(Intent intent) {super.onNewIntent(intent);setIntent(intent);if(!busy)refresh();}
    @Override protected void onStart() {super.onStart();foreground=true;}
    @Override protected void onResume() {super.onResume();if(connection!=null&&!busy)refresh();}
    @Override protected void onStop() {foreground=false;cancel.set(true);prepared=null;updateControls();super.onStop();}
    @Override protected void onDestroy() {alive=false;cancel.set(true);worker.shutdown();unregisterReceiver(receiver);super.onDestroy();}
    private void refresh() {
        if(manager==null) {ready=false;prepared=null;connection.setText(getString(R.string.no_usb_service));updateControls();return;}
        List<UsbDevice> found=new ArrayList<>();for(UsbDevice d:manager.getDeviceList().values())if(UsbLink.candidate(d))found.add(d);
        List<UsbAccessory> accessories=new ArrayList<>();UsbAccessory[] all=manager.getAccessoryList();
        if(all!=null)for(UsbAccessory a:all)if(AccessoryLink.candidate(a))accessories.add(a);
        if(found.size()!=1) {
            selected=null;
            if(found.isEmpty()&&accessories.size()==1) {
                UsbAccessory a=accessories.get(0);
                if(selectedAccessory==null||!selectedAccessory.equals(a)){usbGeneration++;ready=false;prepared=null;}
                selectedAccessory=a;
                connection.setText(ready?getString(R.string.dji_connected):getString(R.string.dji_detected));
            } else {
                selectedAccessory=null;ready=false;prepared=null;
                connection.setText(found.isEmpty()?getString(R.string.no_goggles):getString(R.string.one_goggles));
            }
        } else {
            UsbDevice d=found.get(0);
            if(selected==null||selected.getDeviceId()!=d.getDeviceId()) {usbGeneration++;ready=false;prepared=null;}
            selected=d;
            selectedAccessory=null;
            connection.setText(ready?getString(R.string.menu_connected):getString(R.string.usb_detected));
        }
        if(!hasUsbPermission())ready=false;
        updateControls();
    }
    private void connect() {
        if(busy||manager==null)return;refresh();
        if(selected==null&&selectedAccessory==null) {
            status.setText(getString(R.string.no_connection));
            log(getString(R.string.no_device_log));diagnostics();return;
        }
        prepared=null;ready=false;
        if(hasUsbPermission())probe();
        else {
            permissionPending=true;
            PendingIntent pi=PendingIntent.getBroadcast(this,0,new Intent(PERMISSION).setPackage(getPackageName()),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            if(selected!=null)manager.requestPermission(selected,pi);else manager.requestPermission(selectedAccessory,pi);
        }
    }
    private boolean hasUsbPermission() {
        return manager!=null&&(selected!=null&&manager.hasPermission(selected)||selectedAccessory!=null&&manager.hasPermission(selectedAccessory));
    }
    private void probe() {
        start("probe",ui->{if(!"root".equals(ui.text(0,0)))throw new UiFailure("error_no_root");return null;});
    }
    private void key(LinearLayout row,String label,String code) {
        Button b=button(row,label,false);b.setTextSize(13);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(56),1);lp.setMargins(dp(2),dp(3),dp(2),dp(3));b.setLayoutParams(lp);
        controls.add(b);b.setOnClickListener(v->{prepared=null;start("key",ui->{ui.key(code);return null;});});
    }
    private void start(String operation,Job job) {
        if(busy||!hasUsbPermission())return;
        final UsbDevice device=selected;final UsbAccessory accessory=selectedAccessory;final long generation=usbGeneration;
        busy=true;cancel.set(false);updateControls();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        status.setText(getString(R.string.connecting));log("Start "+operation);
        worker.execute(()-> {
            Object result=null;String error=null;
            try(MenuLink link=device!=null?new UsbLink(manager,device,this::log):new AccessoryLink(manager,accessory,this::log)) {
                GuiSession ui=new GuiSession(link,cancel,message->{log(message);post(()->{if(foreground&&generation==usbGeneration)status.setText(message);});},this::phrase);
                result=job.run(ui);
                if(cancel.get())throw new UiFailure("error_cancelled");
            } catch(Exception e) {error=e instanceof UiFailure?phrase(((UiFailure)e).key,((UiFailure)e).arguments):(e.getMessage()==null?e.toString():e.getMessage());log("Stop: "+error);}
            final Object value=result;final String problem=error;
            post(()-> {
                busy=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if(!foreground||generation!=usbGeneration) {prepared=null;updateControls();return;}
                if(problem!=null) {prepared=null;if(operation.equals("probe"))ready=false;status.setText(getString(R.string.stopped_prefix,problem));}
                else if(operation.equals("probe")) {ready=true;connection.setText(getString(R.string.menu_connected));status.setText(getString(R.string.connected_status));}
                else if(operation.equals("prepare")) {
                    ManualWizard.Prepared p=(ManualWizard.Prepared)value;
                    if(p.verifyOnly) {prepared=null;status.setText(getString(R.string.already_manual));}
                    else {prepared=p;status.setText(getString(R.string.confirm_ready));}
                } else if(operation.equals("confirm"))status.setText(getString(R.string.confirmed));
                else status.setText(getString(R.string.key_accepted));
                updateControls();
            });
        });
    }
    private void updateControls() {
        if(connect==null)return;connect.setEnabled(!busy&&manager!=null);
        prepare.setEnabled(ready&&!busy);confirm.setEnabled(ready&&!busy&&prepared!=null);
        stationary.setEnabled(!busy);stop.setEnabled(busy);if(language!=null)language.setEnabled(!busy);
        for(Button b:controls)b.setEnabled(ready&&!busy);
    }
    private void post(Runnable action) {runOnUiThread(()->{if(alive&&!isFinishing())action.run();});}
    private synchronized void log(String line) {
        if(journal.size()>=800)journal.remove(0);
        journal.add(SystemClock.elapsedRealtime()+" "+line);
    }
    private void help() {
        new AlertDialog.Builder(this).setTitle(getString(R.string.help_title))
            .setMessage(getString(R.string.help_body))
            .setPositiveButton(getString(R.string.understood),null).show();
    }
    private void diagnostics() {
        if(manager==null) {new AlertDialog.Builder(this).setTitle(getString(R.string.diagnostics_title)).setMessage(getString(R.string.no_usb_dialog)).setPositiveButton(getString(R.string.close),null).show();return;}
        StringBuilder s=new StringBuilder("Android "+Build.VERSION.RELEASE+" · "+Build.MODEL+"\n");
        s.append("Host USB: ").append(getPackageManager().hasSystemFeature("android.hardware.usb.host")).append('\n');
        s.append(getString(R.string.devices_prefix)).append(manager.getDeviceList().size()).append('\n');
        UsbAccessory[] accessories=manager.getAccessoryList();
        s.append(getString(R.string.accessories_prefix)).append(accessories==null?0:accessories.length).append('\n');
        if(accessories!=null)for(UsbAccessory a:accessories) {
            s.append("AOA: ").append(a.getManufacturer()).append(" / ").append(a.getModel()).append(" / ").append(a.getDescription()).append('\n');
            s.append(getString(R.string.permission_prefix)).append(manager.hasPermission(a)).append(getString(R.string.recognized_prefix)).append(AccessoryLink.candidate(a)).append('\n');
        }
        for(UsbDevice d:manager.getDeviceList().values()) {
            s.append(String.format(Locale.ROOT,"USB %04x:%04x · zgoda=%s\n",d.getVendorId(),d.getProductId(),manager.hasPermission(d)));
            for(int i=0;i<d.getInterfaceCount();i++) {
                UsbInterface f=d.getInterface(i);s.append("IF ").append(f.getId()).append(getString(R.string.interface_class)).append(f.getInterfaceClass()).append(" EP ");
                for(int j=0;j<f.getEndpointCount();j++)s.append(String.format(Locale.ROOT,"%02x ",f.getEndpoint(j).getAddress()));s.append('\n');
            }
        }
        new AlertDialog.Builder(this).setTitle(getString(R.string.diagnostics_title)).setMessage(s.toString())
            .setPositiveButton(getString(R.string.save_report),(dialog,which)-> {
                Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"N3Manual-raport.json");
                startActivityForResult(save,50);
            }).setNegativeButton(getString(R.string.close),null).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=50||result!=RESULT_OK||data==null||data.getData()==null)return;
        try(OutputStream out=getContentResolver().openOutputStream(data.getData())) {
            JSONObject report=new JSONObject();report.put("app","N3 Manual "+BuildConfig.VERSION_NAME);report.put("phone",Build.MODEL);report.put("android",Build.VERSION.RELEASE);
            report.put("firmware_modified",false);report.put("permanent_manual_fix",false);report.put("manual_result_requires_observation",true);
            synchronized(this){report.put("events",new JSONArray(journal));}
            if(out==null)throw new UiFailure("error_report_open");out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
            status.setText(getString(R.string.report_saved));
        } catch(Exception e) {status.setText(getString(R.string.save_failed_prefix,e instanceof UiFailure?phrase(((UiFailure)e).key,((UiFailure)e).arguments):e.getMessage()));}
    }
    private void about() {
        String info=getString(R.string.about_body,BuildConfig.VERSION_NAME);
        new AlertDialog.Builder(this).setTitle(getString(R.string.about_title)).setMessage(info)
            .setPositiveButton(getString(R.string.close),null)
            .setNegativeButton(getString(R.string.app_license),(d,w)->showLicense("N3 Manual · MIT",R.raw.app_license))
            .setNeutralButton(getString(R.string.third_eye_license),(d,w)->showLicense("third-eye · MIT",R.raw.third_eye_license)).show();
    }
    private void showLicense(String title,int resource) {
        try(java.io.InputStream stream=getResources().openRawResource(resource)) {
            java.io.ByteArrayOutputStream data=new java.io.ByteArrayOutputStream();byte[] bytes=new byte[1024];int n;
            while((n=stream.read(bytes))!=-1)data.write(bytes,0,n);
            new AlertDialog.Builder(this).setTitle(title).setMessage(new String(data.toByteArray(),StandardCharsets.UTF_8)).setPositiveButton(getString(R.string.close),null).show();
        }catch(Exception e){status.setText(e.getMessage());}
    }
    private String phrase(String key,Object... args) {
        Integer id=MessageResources.IDS.get(key);
        if(id==null)throw new IllegalArgumentException("Unknown localized message: "+key);
        return getString(id,args);
    }
    private void chooseLanguage() {
        if(busy)return;
        String[] modes={"system","en","pl"};
        String current=AppLanguage.choice(this);int selected=Arrays.asList(modes).indexOf(current);
        new AlertDialog.Builder(this).setTitle(R.string.language_title)
            .setSingleChoiceItems(new String[]{getString(R.string.language_system),getString(R.string.language_english),getString(R.string.language_polish)},selected,(dialog,which)->{
                dialog.dismiss();cancel.set(true);prepared=null;ready=false;
                AppLanguage.set(this,modes[which]);
            }).setNegativeButton(R.string.cancel,null).show();
    }
    private int dp(int n) {return Math.round(getResources().getDisplayMetrics().density*n);}
    private LinearLayout card() {
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(14),dp(16),dp(14));
        GradientDrawable bg=new GradientDrawable();bg.setColor(CARD);bg.setCornerRadius(dp(18));box.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(16);content.addView(box,lp);return box;
    }
    private TextView text(LinearLayout parent,String value,int size,int color,boolean bold) {
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(3),0,dp(5));
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);parent.addView(t);return t;
    }
    private Button button(LinearLayout parent,String label,boolean primary) {
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(15);
        b.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{MUTED,primary?BG:INK}));
        b.setBackgroundTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xff30414b,primary?MINT:0xff2b3e49}));
        b.setMinHeight(dp(52));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(5);parent.addView(b,lp);return b;
    }
}
