package pl.stasz.n3manual;
import java.util.*;

public final class CoreTests {
    static int checks;
    static void ok(boolean condition) {checks++;if(!condition)throw new AssertionError("check "+checks);}
    static void fails(Throwing action) throws Exception {boolean failed=false;try{action.run();}catch(Exception e){failed=true;}ok(failed);}
    interface Throwing {void run()throws Exception;}
    static byte[] reply(byte[] request,int sequence,int sender) {
        byte[] a=new byte[14];a[0]=0x55;a[1]=14;a[2]=4;a[3]=(byte)Duml.crc(a,3,0x77,0x8c);
        a[4]=(byte)sender;a[5]=10;a[6]=(byte)sequence;a[7]=(byte)(sequence>>>8);a[8]=(byte)0xc0;a[9]=83;a[10]=request[10];
        int c=Duml.crc(a,12,0x3692,0x8408);a[12]=(byte)c;a[13]=(byte)(c>>>8);return a;
    }
    static String hex(byte[] data) {StringBuilder s=new StringBuilder();for(byte b:data)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    static byte[] identityRequest(int sender,int destination,int set,int cmd,byte[] payload) {
        byte[] a=new byte[13+payload.length];int len=a.length|1024;
        a[0]=0x55;a[1]=(byte)len;a[2]=(byte)(len>>>8);a[3]=(byte)Duml.crc(a,3,0x77,0x8c);
        a[4]=(byte)sender;a[5]=(byte)destination;a[6]=99;a[8]=0x40;a[9]=(byte)set;a[10]=(byte)cmd;
        System.arraycopy(payload,0,a,11,payload.length);int sum=Duml.crc(a,a.length-2,0x3692,0x8408);
        a[a.length-2]=(byte)sum;a[a.length-1]=(byte)(sum>>>8);return a;
    }
    static class FakeUi implements ManualWizard.Ui {
        String stage="image",mode="Sport";int index;
        boolean differentLabel;
        List<String> keys=new ArrayList<>();
        Map<Integer,String> names=new HashMap<>();
        Map<String,List<Integer>> layouts=new HashMap<>();
        FakeUi() {
            String[] main={"Status","Album","Transmission","Settings","More"};
            for(int i=0;i<5;i++)names.put(104+i,"menuitem."+main[i]+" Entry");
            names.put(121,"menuitem.Security Entry");names.put(122,"menuitem.Control Entry");names.put(139,"menuitem.Rc Settings Entry");
            names.put(54,"menu.Menu");names.put(150,"menu.Menu");
            for(int n:new int[]{151,152,153,155,156,157})names.put(n,"menuitem.");
            names.put(180,"menuitem.Manual Mode(0)");names.put(181,"menuitem.Sport Mode(1)");
            layouts.put("main",Arrays.asList(104,105,106,107,108));layouts.put("settings",Arrays.asList(121,122));
            layouts.put("control",Arrays.asList(139));layouts.put("custom",Arrays.asList(151,152,153,155,156,157));
            layouts.put("selector",Arrays.asList(180,181));layouts.put("dialog",Arrays.asList(200,201));
        }
        public Map<Integer,String> scan() {Map<Integer,String> m=new HashMap<>(names);if(stage.equals("dialog")){m.put(200,"");m.put(201,"");}return m;}
        public String text(int n,int f) {
            if(f==0)return scan().get(n);
            if(f==9){if(n==54||n==150)return "List|menu.Menu\n";return n==155?"Label|":"Button|";}
            if(f==11)return n==155?(differentLabel?"other":"Recenter Gimbal"):"";
            return null;
        }
        public boolean focused(int n) {return layouts.containsKey(stage)&&index>=0&&layouts.get(stage).get(index)==n;}
        public void progress(String s){}
        public void key(String k) throws Exception {
            keys.add(k);
            if(k.equals("back")){stage="image";index=0;return;}
            if(stage.equals("image")&&k.equals("right")){stage="main";index=0;return;}
            if(k.equals("up")||k.equals("down")){index=Math.max(0,Math.min(layouts.get(stage).size()-1,index+(k.equals("down")?1:-1)));return;}
            if(stage.equals("dialog")&&k.equals("right")){index=1;return;}
            if(!k.equals("ok"))throw new Exception("unexpected key");
            switch(stage) {
                case "main":ok(index==3);stage="settings";index=0;break;
                case "settings":ok(index==1);stage="control";index=0;break;
                case "control":stage="custom";index=-1;break;
                case "custom":ok(index==5);stage="selector";index=1;break;
                case "selector":ok(index==0);if(!mode.equals("Manual")){stage="dialog";index=0;}break;
                case "dialog":ok(index==1);mode="Manual";stage="selector";index=0;break;
                default:throw new Exception("unexpected stage");
            }
        }
    }
    public static void main(String[] args) throws Exception {
        byte[] req=Duml.read(0x1234,263,9),ack=reply(req,0x1234,28);
        ok(Duml.matches(req,ack));ok(!Duml.matches(req,reply(req,0x1235,28)));ok(!Duml.matches(req,reply(req,0x1234,188)));ok(!Duml.matches(req,req));
        Duml.Parser p=new Duml.Parser();ok(p.feed(Arrays.copyOf(ack,5),5).isEmpty());
        List<byte[]> parsed=p.feed(Arrays.copyOfRange(ack,5,ack.length),ack.length-5);ok(parsed.size()==1&&Arrays.equals(parsed.get(0),ack));
        byte[] bad=ack.clone();bad[bad.length-1]^=1;
        byte[] noise=new byte[bad.length+ack.length+1];noise[0]=0x11;System.arraycopy(bad,0,noise,1,bad.length);System.arraycopy(ack,0,noise,1+bad.length,ack.length);
        ok(new Duml.Parser().feed(noise,noise.length).size()==1);
        fails(()->Duml.read(1,2048,9));fails(()->Duml.read(1,0,5));fails(()->Duml.key(1,"motor",0));fails(()->Duml.key(1,"ok",2));
        for(String key:new String[]{"left","right","up","down","ok","back"})for(int event=0;event<2;event++) {
            byte[] f=Duml.key(0x1234,key,event);ok(f[4]==10&&f[5]==28&&f[9]==83&&f[10]==1);ok(f[17]==event);
            System.out.println("VECTOR key:"+key+":"+event+" "+hex(f));
        }
        System.out.println("VECTOR read:263:9 "+hex(req));
        byte[] mobile=Duml.read(0x1234,263,9,2);ok(mobile[4]==2&&mobile[5]==28);
        fails(()->Duml.key(1,"ok",0,3));fails(()->Duml.read(1,0,0,3));
        byte[] wrapped=LogicTunnel.wrap(mobile);ok(hex(Arrays.copyOf(wrapped,8)).equals("55cc495716000000"));
        LogicTunnel tunnel=new LogicTunnel();ok(tunnel.feed(Arrays.copyOf(wrapped,6),6).isEmpty());
        parsed=tunnel.feed(Arrays.copyOfRange(wrapped,6,wrapped.length),wrapped.length-6);
        ok(parsed.size()==1&&Arrays.equals(parsed.get(0),mobile));
        byte[] video=LogicTunnel.wrap(new byte[]{1,2,3});video[2]=0x4a;ok(new LogicTunnel().feed(video,video.length).isEmpty());
        byte[] empty=LogicTunnel.wrap(new byte[0]);ok(new LogicTunnel().feed(empty,empty.length).isEmpty());
        byte[] mixed=new byte[1+video.length+wrapped.length];System.arraycopy(video,0,mixed,1,video.length);System.arraycopy(wrapped,0,mixed,1+video.length,wrapped.length);
        ok(new LogicTunnel().feed(mixed,mixed.length).size()==1);
        byte[] n3=wrapped.clone();n3[2]=0x30;n3[3]=0x75;
        parsed=new LogicTunnel().feed(n3,n3.length);ok(parsed.size()==1&&Arrays.equals(parsed.get(0),mobile));
        tunnel=new LogicTunnel();ok(tunnel.feed(Arrays.copyOf(n3,11),11).isEmpty());
        parsed=tunnel.feed(Arrays.copyOfRange(n3,11,n3.length),n3.length-11);ok(parsed.size()==1);
        byte[] n3AndOld=new byte[n3.length+wrapped.length];System.arraycopy(n3,0,n3AndOld,0,n3.length);System.arraycopy(wrapped,0,n3AndOld,n3.length,wrapped.length);
        ok(new LogicTunnel().feed(n3AndOld,n3AndOld.length).size()==2);
        byte[] video2=video.clone();video2[2]=0x4b;ok(new LogicTunnel().feed(video2,video2.length).isEmpty());
        byte[] record=new byte[63];record[32]=5;record[33]=28;
        byte[] ident=identityRequest(188,2,0,0x81,record),answer=Duml.linkReply(ident);
        ok(answer!=null&&Duml.matches(ident,answer));ok(Duml.payload(answer).length==64);
        ok(Duml.payload(answer)[34]==2&&Duml.payload(answer)[41]==5&&Duml.payload(answer)[42]==28);
        ok(new Duml.Parser().feed(answer,answer.length).size()==1);
        ok(Duml.linkReply(identityRequest(3,2,3,0x8f,record))==null);
        ok(Duml.linkReply(identityRequest(188,10,0,0x81,record))==null);
        ok(Duml.linkReply(identityRequest(60,2,0,0x88,new byte[]{0x17,0}))==null);
        ok(Arrays.equals(Duml.payload(Duml.linkReply(identityRequest(60,2,0,0x88,new byte[]{0x19,0}))),new byte[]{0x1a,0,0,0,0}));
        FakeUi ui=new FakeUi();ManualWizard.Prepared state=ManualWizard.prepare(ui);
        ok(ui.stage.equals("dialog")&&ui.mode.equals("Sport")&&!state.verifyOnly);
        ManualWizard.confirm(ui,state);ok(ui.mode.equals("Manual"));
        ui=new FakeUi();ui.mode="Manual";state=ManualWizard.prepare(ui);ok(state.verifyOnly&&ui.stage.equals("selector"));
        final FakeUi existing=ui;final ManualWizard.Prepared noDialog=state;int count=ui.keys.size();fails(()->ManualWizard.confirm(existing,noDialog));ok(count==ui.keys.size());
        final FakeUi moved=new FakeUi();final ManualWizard.Prepared pending=ManualWizard.prepare(moved);moved.index=0;count=moved.keys.size();fails(()->ManualWizard.confirm(moved,pending));ok(count==moved.keys.size());
        final FakeUi changed=new FakeUi();final ManualWizard.Prepared old=ManualWizard.prepare(changed);changed.names.put(250,"unexpected");count=changed.keys.size();fails(()->ManualWizard.confirm(changed,old));ok(count==changed.keys.size());
        final FakeUi wrong=new FakeUi();wrong.differentLabel=true;fails(()->ManualWizard.prepare(wrong));ok(wrong.stage.equals("custom")&&wrong.index==-1);
        System.out.println("PASS "+checks+" assertions; protocol, framing, changed UI, two-step confirmation, already-Manual");
    }
}
