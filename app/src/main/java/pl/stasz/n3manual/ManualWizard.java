package pl.stasz.n3manual;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ManualWizard {
    private ManualWizard() {}
    public interface Ui {
        Map<Integer,String> scan() throws Exception;
        String text(int node,int field) throws Exception;
        boolean focused(int node) throws Exception;
        void key(String name) throws Exception;
        void progress(String message);
    }
    public static final class Prepared {
        public final int left,right;
        public final Map<Integer,String> screen;
        public final boolean verifyOnly;
        Prepared(int a,int b,Map<Integer,String> s,boolean v) {
            left=a;right=b;screen=new HashMap<>(s);verifyOnly=v;
        }
    }
    static IOException changed(String text) { return new IOException(text); }
    static int unique(Map<Integer,String> nodes,String name) throws Exception {
        int found=-1;
        for(Map.Entry<Integer,String> e:nodes.entrySet()) if(name.equals(e.getValue())) {
            if(found!=-1) throw changed("Niejednoznaczna pozycja: "+name);
            found=e.getKey();
        }
        if(found==-1) throw changed("Brak pozycji: "+name+". Sprawdź połączenie Neo i RC3.");
        return found;
    }
    static void focus(Ui ui,String first,String target,int bound) throws Exception {
        Map<Integer,String> n=ui.scan(); int a=unique(n,first), b=unique(n,target);
        int i=0;
        while(!ui.focused(a)) {
            if(i++>=bound) throw changed("Nie znaleziono początku listy.");
            ui.key("up");
        }
        i=0;
        while(!ui.focused(b)) {
            if(i++>=bound) throw changed("Nie znaleziono zaznaczenia: "+target);
            ui.key("down");
        }
    }
    static int customMode(Ui ui,Map<Integer,String> nodes) throws Exception {
        List<Integer> matched=new ArrayList<>();
        for(Map.Entry<Integer,String> e:nodes.entrySet()) {
            int parent=e.getKey();
            if(!"menu.Menu".equals(e.getValue())) continue;
            String d=ui.text(parent,9);
            if(d==null||!d.startsWith("List|menu.Menu\n")) continue;
            List<Integer> rows=new ArrayList<>();
            for(Map.Entry<Integer,String> r:nodes.entrySet())
                if(r.getKey()>parent&&r.getKey()<parent+16&&"menuitem.".equals(r.getValue())) rows.add(r.getKey());
            Collections.sort(rows);
            if(rows.size()!=6) continue;
            String[] kinds={"Button|","Button|","Button|","Label|","Button|","Button|"};
            boolean valid=true;
            for(int i=0;i<6;i++) {
                d=ui.text(rows.get(i),9);
                if(d==null||!d.startsWith(kinds[i])) valid=false;
            }
            if(valid&&"Recenter Gimbal".equals(ui.text(rows.get(3),11))) matched.add(rows.get(5));
        }
        if(matched.size()!=1) throw changed("Inny układ ustawień RC3. Użyj ręcznych strzałek.");
        return matched.get(0);
    }
    public static Prepared prepare(Ui ui) throws Exception {
        ui.progress("Powrót do widoku obrazu…");
        for(int i=0;i<6;i++) ui.key("back");
        ui.key("right");
        focus(ui,"menuitem.Status Entry","menuitem.Settings Entry",5);ui.key("ok");
        focus(ui,"menuitem.Security Entry","menuitem.Control Entry",6);ui.key("ok");
        focus(ui,"menuitem.Rc Settings Entry","menuitem.Rc Settings Entry",10);ui.key("ok");
        ui.progress("Button Customization → Custom Mode…");
        int custom=customMode(ui,ui.scan()); int count=0;
        while(!ui.focused(custom)) {
            if(count++>=8) throw changed("Nie zaznaczono Custom Mode.");
            ui.key("down");
        }
        ui.key("ok");Map<Integer,String> before=ui.scan();
        int manual=unique(before,"menuitem.Manual Mode(0)");
        unique(before,"menuitem.Sport Mode(1)");count=0;
        while(!ui.focused(manual)) {
            if(count++>=2) throw changed("Nie zaznaczono Manual Mode.");
            ui.key("up");
        }
        ui.key("ok");Map<Integer,String> after=ui.scan();
        List<Integer> buttons=new ArrayList<>();
        for(Map.Entry<Integer,String> e:after.entrySet()) if(!before.containsKey(e.getKey())&&"".equals(e.getValue())) {
            String d=ui.text(e.getKey(),9);
            if(d!=null&&d.startsWith("Button|")) buttons.add(e.getKey());
        }
        Collections.sort(buttons);
        if(buttons.isEmpty()&&"menuitem.Manual Mode(0)".equals(after.get(manual))&&ui.focused(manual))
            return new Prepared(-1,-1,after,true);
        if(buttons.size()!=2||!ui.focused(buttons.get(0))) throw changed("Nie rozpoznano okna Confirm. Sprawdź obraz w goglach.");
        ui.key("right");
        if(!ui.focused(buttons.get(1))) throw changed("Nie zaznaczono prawego przycisku.");
        return new Prepared(buttons.get(0),buttons.get(1),after,false);
    }
    public static void confirm(Ui ui,Prepared state) throws Exception {
        if(state==null||state.verifyOnly||!state.screen.equals(ui.scan())) throw changed("Ekran zmienił się. Uruchom krok 1 ponownie.");
        for(int n:new int[]{state.left,state.right}) {
            String d=ui.text(n,9);
            if(!"".equals(ui.text(n,0))||d==null||!d.startsWith("Button|")) throw changed("Inne okno potwierdzenia.");
        }
        if(ui.focused(state.left)||!ui.focused(state.right)) throw changed("Zmieniono zaznaczenie. Nie wysłano OK.");
        ui.key("ok");
        if(ui.text(state.right,0)!=null) throw changed("Okno nadal jest otwarte. Sprawdź komunikat w goglach.");
    }
}
