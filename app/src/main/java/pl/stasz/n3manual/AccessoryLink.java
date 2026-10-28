package pl.stasz.n3manual;

import android.hardware.usb.*;
import android.os.*;
import android.system.*;
import java.io.IOException;
import java.util.*;

/** Experimental GUI reads/keys over DJI's Android accessory control channel. */
public final class AccessoryLink implements MenuLink {
    private final ParcelFileDescriptor fd;
    private final UsbLink.Log log;
    private final LogicTunnel tunnel=new LogicTunnel();
    private final Duml.Parser duml=new Duml.Parser();
    private int sequence=(int)(SystemClock.elapsedRealtime()%60000),sender=2;
    private int incoming,controlPackets,identityReplies;
    public static boolean candidate(UsbAccessory a) {
        return "DJI".equalsIgnoreCase(a.getManufacturer())&&"com.dji.logiclink".equals(a.getModel())
            &&a.getDescription()!=null&&a.getDescription().toLowerCase(Locale.ROOT).contains("glass");
    }
    public AccessoryLink(UsbManager manager,UsbAccessory accessory,UsbLink.Log logger) throws Exception {
        log=logger;
        if(!candidate(accessory)||!manager.hasPermission(accessory))throw new IOException("Brak rozpoznanego akcesorium DJI glass lub zgody Androida.");
        ParcelFileDescriptor opened=manager.openAccessory(accessory);
        if(opened==null)throw new IOException("Kanał DJI jest zajęty. Zamknij DJI Fly, odepnij kabel i wybierz N3 Manual po ponownym podłączeniu.");
        fd=opened;
        log.write("AOA: DJI glass / com.dji.logiclink; eksperymentalny odczyt GUI");
        boolean found=false;
        try {
            // Read-only route discovery; no mode changes or video registration.
            for(int source:new int[]{2,10,2}) {
                sender=source;
                byte[] reply=exchange(Duml.read(next(),0,0,sender),1000);
                if(reply!=null&&reply.length>=9&&reply[0]==0&&Duml.word(reply,1)==0
                    &&new String(reply,5,reply.length-5,java.nio.charset.StandardCharsets.UTF_8).replace("\0","").equals("root")) {
                    found=true;break;
                }
            }
            if(!found)throw new IOException("Wykryto kanał DJI, lecz menu N3 nie odpowiedziało. Odczytano "+incoming+" B / "+controlPackets+" pakietów sterujących. Zapisz raport diagnostyczny.");
            log.write("AOA: root GUI potwierdzony, sender="+sender);
        } catch(Exception e) {log.write("AOA probe stop: B="+incoming+", control="+controlPackets+", identity="+identityReplies);fd.close();throw e;}
    }
    private int next(){sequence=(sequence+1)&65535;return sequence;}
    private boolean poll(short mask,int timeout) throws Exception {
        StructPollfd p=new StructPollfd();p.fd=fd.getFileDescriptor();p.events=mask;
        int n=Os.poll(new StructPollfd[]{p},timeout);
        if((p.revents&(OsConstants.POLLERR|OsConstants.POLLHUP|OsConstants.POLLNVAL))!=0)throw new IOException("Odłączono kanał USB DJI.");
        return n>0&&(p.revents&mask)!=0;
    }
    private void send(byte[] packet) throws Exception {
        byte[] out=LogicTunnel.wrap(packet);
        if(!poll((short)OsConstants.POLLOUT,500))throw new IOException("Kanał DJI nie przyjmuje danych.");
        if(Os.write(fd.getFileDescriptor(),out,0,out.length)!=out.length)throw new IOException("Niepełny zapis kanału DJI.");
    }
    private byte[] exchange(byte[] request,int timeout) throws Exception {
        send(request);long deadline=SystemClock.elapsedRealtime()+timeout;byte[] buffer=new byte[16384];
        while(SystemClock.elapsedRealtime()<deadline) {
            if(!poll((short)OsConstants.POLLIN,80))continue;
            int n=Os.read(fd.getFileDescriptor(),buffer,0,buffer.length);
            if(n<=0)continue;
            if(incoming==0) {
                StringBuilder header=new StringBuilder();
                for(int i=0;i<Math.min(8,n);i++)header.append(String.format(Locale.ROOT,"%02x",buffer[i]&255));
                log.write("AOA first header8="+header);
            }
            incoming+=n;
            for(byte[] payload:tunnel.feed(buffer,n)) for(byte[] frame:duml.feed(payload,payload.length)) {
                controlPackets++;
                if(controlPackets<=32)log.write(String.format(Locale.ROOT,"AOA RX %d->%d set=%02x cmd=%02x flags=%02x seq=%d bytes=%d",frame[4]&255,frame[5]&255,frame[9]&255,frame[10]&255,frame[8]&255,Duml.word(frame,6),frame.length));
                if(Duml.matches(request,frame))return Duml.payload(frame);
                byte[] response=Duml.linkReply(frame);
                if(response!=null) {send(response);identityReplies++;}
            }
        }
        return null;
    }
    @Override public byte[] read(int node,int field) throws Exception {
        byte[] p=exchange(Duml.read(next(),node,field,sender),700);
        if(p==null)throw new IOException("Menu nie odpowiedziało przez kanał DJI. Zapisz raport.");return p;
    }
    @Override public void event(String name,int event) throws Exception {
        byte[] p=exchange(Duml.key(next(),name,event,sender),1300);
        if(p==null||p.length!=1||p[0]!=0)throw new IOException("Brak potwierdzenia klawisza przez kanał DJI.");
        log.write("AOA GUI "+name+" "+(event==0?"down":"up")+" ACK00");
    }
    @Override public void close() throws IOException {
        log.write("AOA: odebrano "+incoming+" B, control="+controlPackets+", identity/heartbeat replies="+identityReplies);fd.close();
    }
}
