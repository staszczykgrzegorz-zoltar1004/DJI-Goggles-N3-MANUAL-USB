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
    private Button connect,prepare,confirm,stop;
    private LinearLayout content;
    private interface Job { Object run(GuiSession ui) throws Exception; }

    private final BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override public void onReceive(Context context,Intent intent) {
            if(PERMISSION.equals(intent.getAction())) {
                boolean wanted=permissionPending; permissionPending=false;
                if(wanted&&hasUsbPermission()) probe();
                else if(wanted) status.setText("Brak zgody na USB. Dotknij Połącz, aby spróbować ponownie.");
            } else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                UsbDevice detached=intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if(selected!=null&&detached!=null&&selected.getDeviceId()==detached.getDeviceId()) {
                    usbGeneration++;cancel.set(true);prepared=null;ready=false;selected=null;
                    connection.setText("Gogle odłączone");status.setText("Przerwano — odłączono USB.");updateControls();
                }
            } else if(UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(intent.getAction())) {
                usbGeneration++;cancel.set(true);prepared=null;ready=false;selectedAccessory=null;
                connection.setText("Odłączono kanał DJI");status.setText("Podłącz ponownie N3 i dotknij Połącz.");updateControls();
            }
        }
    };
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
        text(content,"Manual, bez przycisku 5D",27,INK,true);
        text(content,"DJI Goggles N3 · Neo · FPV RC3",14,MUTED,false);
        LinearLayout usb=card();
        connection=text(usb,"Podłącz gogle do telefonu",17,INK,true);
        connect=button(usb,"Połącz i sprawdź USB",true);connect.setOnClickListener(v->connect());
        Button usbState=button(usb,"Pokaż stan USB",false);usbState.setOnClickListener(v->diagnostics());
        text(usb,"Tylko gogle na USB. Neo i RC3 połączone radiowo. Zamknij DJI Fly.",14,MUTED,false);
        LinearLayout restore=card();
        text(restore,"Przywróć przypisanie M",20,INK,true);
        stationary=new CheckBox(this);stationary.setText("Zestaw na stole, śmigła zdjęte, silniki wyłączone");
        stationary.setTextColor(INK);stationary.setTextSize(14);stationary.setButtonTintList(ColorStateList.valueOf(MINT));restore.addView(stationary);
        prepare=button(restore,"1. Przejdź do Manual",true);
        prepare.setOnClickListener(v->{
            if(!stationary.isChecked()) {status.setText("Przygotuj zestaw na stole i zaznacz pole powyżej.");return;}
            prepared=null;start("prepare",ManualWizard::prepare);
        });
        text(restore,"Spójrz w gogle. Jeśli prawy przycisk ma napis Confirm, użyj kroku 2.",14,MUTED,false);
        confirm=button(restore,"2. Widzę Confirm — zatwierdź",false);
        confirm.setOnClickListener(v->{
            if(!stationary.isChecked()||prepared==null)return;
            ManualWizard.Prepared state=prepared;prepared=null;
            start("confirm",ui->{ManualWizard.confirm(ui,state);return null;});
        });
        status=text(restore,"Najpierw połącz gogle. Polecenia są wysyłane dopiero po dotknięciu przycisku.",15,INK,false);
        stop=button(restore,"Przerwij",false);stop.setOnClickListener(v->{cancel.set(true);prepared=null;status.setText("Przerywam po zwolnieniu klawisza…");});
        LinearLayout arrows=card();text(arrows,"Ręczny pilot menu",19,INK,true);
        LinearLayout row=new LinearLayout(this);arrows.addView(row);
        key(row,"← Lewo","left");key(row,"↑ Góra","up");key(row,"Prawo →","right");
        row=new LinearLayout(this);arrows.addView(row);
        key(row,"Wstecz","back");key(row,"↓ Dół","down");key(row,"OK","ok");
        LinearLayout extra=card();
        Button help=button(extra,"Jak podłączyć USB / OTG",false);help.setOnClickListener(v->help());
        Button diagnostics=button(extra,"Diagnostyka i raport",false);diagnostics.setOnClickListener(v->diagnostics());
        Button about=button(extra,"O aplikacji i prywatności",false);about.setOnClickListener(v->about());
        text(content,"N3 Manual "+BuildConfig.VERSION_NAME+" · niezależne narzędzie\nPrzywraca wybór w menu. Nie zamraża ustawień na stałe.",12,MUTED,false);
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
        if(manager==null) {ready=false;prepared=null;connection.setText("Ten system nie udostępnia obsługi USB.");updateControls();return;}
        List<UsbDevice> found=new ArrayList<>();for(UsbDevice d:manager.getDeviceList().values())if(UsbLink.candidate(d))found.add(d);
        List<UsbAccessory> accessories=new ArrayList<>();UsbAccessory[] all=manager.getAccessoryList();
        if(all!=null)for(UsbAccessory a:all)if(AccessoryLink.candidate(a))accessories.add(a);
        if(found.size()!=1) {
            selected=null;
            if(found.isEmpty()&&accessories.size()==1) {
                UsbAccessory a=accessories.get(0);
                if(selectedAccessory==null||!selectedAccessory.equals(a)){usbGeneration++;ready=false;prepared=null;}
                selectedAccessory=a;
                connection.setText(ready?"N3 połączone · kanał DJI odpowiada":"Wykryto kanał DJI telefonu · dotknij Połącz");
            } else {
                selectedAccessory=null;ready=false;prepared=null;
                connection.setText(found.isEmpty()?"Nie wykryto połączenia USB z N3":"Podłącz tylko jedne gogle DJI");
            }
        } else {
            UsbDevice d=found.get(0);
            if(selected==null||selected.getDeviceId()!=d.getDeviceId()) {usbGeneration++;ready=false;prepared=null;}
            selected=d;
            selectedAccessory=null;
            connection.setText(ready?"N3 połączone · menu odpowiada":"Wykryto urządzenie DJI · sprawdź połączenie");
        }
        if(!hasUsbPermission())ready=false;
        updateControls();
    }
    private void connect() {
        if(busy||manager==null)return;refresh();
        if(selected==null&&selectedAccessory==null) {
            status.setText("Brak dostępu do N3: Android nie udostępnia ani urządzenia host USB, ani kanału DJI. Dotknij Pokaż stan USB.");
            log("Connect: brak urządzenia i rozpoznanego akcesorium");diagnostics();return;
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
        start("probe",ui->{if(!"root".equals(ui.text(0,0)))throw new Exception("Brak rozpoznanej odpowiedzi GUI N3.");return null;});
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
        status.setText("Łączę z menu gogli…");log("Start "+operation);
        worker.execute(()-> {
            Object result=null;String error=null;
            try(MenuLink link=device!=null?new UsbLink(manager,device,this::log):new AccessoryLink(manager,accessory,this::log)) {
                GuiSession ui=new GuiSession(link,cancel,message->{log(message);post(()->{if(foreground&&generation==usbGeneration)status.setText(message);});});
                result=job.run(ui);
                if(cancel.get())throw new Exception("Przerwano operację.");
            } catch(Exception e) {error=e.getMessage()==null?e.toString():e.getMessage();log("Stop: "+error);}
            final Object value=result;final String problem=error;
            post(()-> {
                busy=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if(!foreground||generation!=usbGeneration) {prepared=null;updateControls();return;}
                if(problem!=null) {prepared=null;if(operation.equals("probe"))ready=false;status.setText("Zatrzymano: "+problem);}
                else if(operation.equals("probe")) {ready=true;connection.setText("N3 połączone · menu odpowiada");status.setText("Połączono. Możesz użyć kreatora lub strzałek.");}
                else if(operation.equals("prepare")) {
                    ManualWizard.Prepared p=(ManualWizard.Prepared)value;
                    if(p.verifyOnly) {prepared=null;status.setText("Wybrano Manual bez nowego Confirm. Sprawdź znacznik przy Manual Mode w goglach.");}
                    else {prepared=p;status.setText("Spójrz w gogle. Jeśli prawy przycisk to Confirm, dotknij kroku 2.");}
                } else if(operation.equals("confirm"))status.setText("Zatwierdzono. Sprawdź w goglach, czy Manual jest wybrane.");
                else status.setText("Klawisz przyjęty. Sprawdź reakcję w goglach.");
                updateControls();
            });
        });
    }
    private void updateControls() {
        if(connect==null)return;connect.setEnabled(!busy&&manager!=null);
        prepare.setEnabled(ready&&!busy);confirm.setEnabled(ready&&!busy&&prepared!=null);
        stationary.setEnabled(!busy);stop.setEnabled(busy);
        for(Button b:controls)b.setEnabled(ready&&!busy);
    }
    private void post(Runnable action) {runOnUiThread(()->{if(alive&&!isFinishing())action.run();});}
    private synchronized void log(String line) {
        if(journal.size()>=800)journal.remove(0);
        journal.add(SystemClock.elapsedRealtime()+" "+line);
    }
    private void help() {
        new AlertDialog.Builder(this).setTitle("Połączenie z goglami")
            .setMessage("1. Połącz włączone N3 z telefonem przewodem USB z transmisją danych. Neo i RC3 pozostają połączone radiowo, nie przez USB.\n\n2. Zamknij DJI Fly. W oknie wyboru aplikacji USB wybierz N3 Manual i zaakceptuj zgodę Androida. Jeśli zawsze uruchamia się Fly, usuń jego domyślne otwieranie USB w ustawieniach aplikacji.\n\n3. Dotknij Połącz i sprawdź USB. Gdy menu odpowiada, uruchom krok 1. Patrząc w gogle, użyj kroku 2 tylko przy zaznaczonym prawym Confirm.\n\n4. Przy błędzie użyj Pokaż stan USB i zapisz raport. Możliwe są zarówno połączenie Android Accessory przez C–C, jak i host OTG. Nie potrzeba debugowania USB do korzystania z aplikacji.\n\n5. Ustawienia zmieniaj na ziemi, ze zdjętymi śmigłami i wyłączonymi silnikami. Przed lotem sprawdź przypisanie M w goglach. Aplikacja nie zamraża ustawień i nie naprawia firmware.")
            .setPositiveButton("Rozumiem",null).show();
    }
    private void diagnostics() {
        if(manager==null) {new AlertDialog.Builder(this).setTitle("Diagnostyka USB").setMessage("System nie udostępnia usługi USB.").setPositiveButton("Zamknij",null).show();return;}
        StringBuilder s=new StringBuilder("Android "+Build.VERSION.RELEASE+" · "+Build.MODEL+"\n");
        s.append("Host USB: ").append(getPackageManager().hasSystemFeature("android.hardware.usb.host")).append('\n');
        s.append("Urządzenia USB: ").append(manager.getDeviceList().size()).append('\n');
        UsbAccessory[] accessories=manager.getAccessoryList();
        s.append("Akcesoria Android: ").append(accessories==null?0:accessories.length).append('\n');
        if(accessories!=null)for(UsbAccessory a:accessories) {
            s.append("AOA: ").append(a.getManufacturer()).append(" / ").append(a.getModel()).append(" / ").append(a.getDescription()).append('\n');
            s.append("Zgoda AOA: ").append(manager.hasPermission(a)).append(" · rozpoznane DJI: ").append(AccessoryLink.candidate(a)).append('\n');
        }
        for(UsbDevice d:manager.getDeviceList().values()) {
            s.append(String.format(Locale.ROOT,"USB %04x:%04x · zgoda=%s\n",d.getVendorId(),d.getProductId(),manager.hasPermission(d)));
            for(int i=0;i<d.getInterfaceCount();i++) {
                UsbInterface f=d.getInterface(i);s.append("IF ").append(f.getId()).append(" klasa ").append(f.getInterfaceClass()).append(" EP ");
                for(int j=0;j<f.getEndpointCount();j++)s.append(String.format(Locale.ROOT,"%02x ",f.getEndpoint(j).getAddress()));s.append('\n');
            }
        }
        new AlertDialog.Builder(this).setTitle("Diagnostyka USB").setMessage(s.toString())
            .setPositiveButton("Zapisz raport",(dialog,which)-> {
                Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"N3Manual-raport.json");
                startActivityForResult(save,50);
            }).setNegativeButton("Zamknij",null).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=50||result!=RESULT_OK||data==null||data.getData()==null)return;
        try(OutputStream out=getContentResolver().openOutputStream(data.getData())) {
            JSONObject report=new JSONObject();report.put("app","N3 Manual "+BuildConfig.VERSION_NAME);report.put("phone",Build.MODEL);report.put("android",Build.VERSION.RELEASE);
            report.put("firmware_modified",false);report.put("permanent_manual_fix",false);report.put("manual_result_requires_observation",true);
            synchronized(this){report.put("events",new JSONArray(journal));}
            if(out==null)throw new Exception("Nie otwarto pliku.");out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
            status.setText("Raport zapisany.");
        } catch(Exception e) {status.setText("Nie zapisano raportu: "+e.getMessage());}
    }
    private void about() {
        String info="N3 Manual "+BuildConfig.VERSION_NAME+"\nAutor: Grzegorz Staszczyk\nLicencja MIT\n\nNiezależne narzędzie do menu Goggles N3 + Neo + FPV Remote Controller 3. Bez powiązania z DJI. Inne zestawy nie są potwierdzone.\n\nAplikacja nie korzysta z internetu, konta, kamery ani mikrofonu. Raport jest w pamięci i zapisuje się tylko po wybraniu pliku. Zawiera model i wersję Androida oraz diagnostykę USB, bez numeru seryjnego telefonu.\n\nTesty emulatorów dotyczą zgodności aplikacji, nie fizycznego USB. Po użyciu sprawdź przypisanie Manual w goglach.\n\nFormat LogicLink: third-eye, William Robinet, MIT.";
        new AlertDialog.Builder(this).setTitle("O aplikacji").setMessage(info)
            .setPositiveButton("Zamknij",null)
            .setNegativeButton("MIT aplikacji",(d,w)->showLicense("N3 Manual · MIT",R.raw.app_license))
            .setNeutralButton("MIT third-eye",(d,w)->showLicense("third-eye · MIT",R.raw.third_eye_license)).show();
    }
    private void showLicense(String title,int resource) {
        try(java.io.InputStream stream=getResources().openRawResource(resource)) {
            java.io.ByteArrayOutputStream data=new java.io.ByteArrayOutputStream();byte[] bytes=new byte[1024];int n;
            while((n=stream.read(bytes))!=-1)data.write(bytes,0,n);
            new AlertDialog.Builder(this).setTitle(title).setMessage(new String(data.toByteArray(),StandardCharsets.UTF_8)).setPositiveButton("Zamknij",null).show();
        }catch(Exception e){status.setText(e.getMessage());}
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
