package pl.stasz.n3manual;

import android.os.SystemClock;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GuiSession implements ManualWizard.Ui {
    public interface Text { String get(String key,Object... args); }
    private final Text language;
    private final MenuLink link;
    private final AtomicBoolean cancel;
    private final UsbLink.Log progress;
    private final long deadline=SystemClock.elapsedRealtime()+90000;
    public GuiSession(MenuLink l,AtomicBoolean c,UsbLink.Log p,Text text) {link=l;cancel=c;progress=p;language=text;}
    private void check() throws Exception {
        if(cancel.get()) throw new UiFailure("error_cancelled");
        if(SystemClock.elapsedRealtime()>deadline) throw new UiFailure("error_timeout");
    }
    private byte[] field(int node,int field) throws Exception {
        check();byte[] p=link.read(node,field);
        if(p.length<5||p[0]!=0||Duml.word(p,1)!=field) return null;
        return p;
    }
    @Override public String text(int node,int field) throws Exception {
        if(field!=0&&field!=9&&field!=11) throw new UiFailure("error_text_field");
        byte[] p=field(node,field);if(p==null)return null;
        int end=p.length;while(end>5&&p[end-1]==0)end--;
        return new String(p,5,end-5,StandardCharsets.UTF_8);
    }
    @Override public boolean focused(int node) throws Exception {byte[] p=field(node,4);return p!=null&&p[3]!=0;}
    @Override public Map<Integer,String> scan() throws Exception {
        Map<Integer,String> result=new HashMap<>();
        for(int n=0;n<1024;n++) {
            String name=text(n,0);if(name!=null)result.put(n,name);
            if(n%256==0)progress.write(language.get("progress_scan",n));
        }
        return result;
    }
    @Override public void key(String key) throws Exception {
        check();progress.write(language.get("progress_key",key));
        try {link.event(key,0);Thread.sleep(100);}
        finally {link.event(key,1);}
        Thread.sleep(100);
    }
    @Override public void progress(String message) {progress.write(language.get(message));}
}
