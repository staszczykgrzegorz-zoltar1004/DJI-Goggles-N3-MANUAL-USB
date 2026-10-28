package pl.stasz.n3manual;

import android.hardware.usb.*;
import android.os.SystemClock;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

public final class UsbLink implements MenuLink {
    public interface Log { void write(String line); }
    private final UsbDeviceConnection conn;
    private final UsbInterface face;
    private final UsbEndpoint input,output;
    private final Duml.Parser parser=new Duml.Parser();
    private final Log log;
    private int sequence=(int)(SystemClock.elapsedRealtime()%60000);
    public static boolean candidate(UsbDevice d) {
        return d.getVendorId()==0x2ca3&&d.getProductId()==0x20;
    }
    public UsbLink(UsbManager manager,UsbDevice device,Log logger) throws Exception {
        log=logger;
        if(!candidate(device)||!manager.hasPermission(device)) throw new IOException("Brak zgody Androida na dostęp do gogli.");
        if(!"987654321ABCDEF".equals(device.getSerialNumber()))
            throw new IOException("Nie rozpoznano tego urządzenia jako testowanych N3. Nie podłączaj Neo do USB.");
        UsbInterface chosen=null;UsbEndpoint in=null,out=null;
        for(int i=0;i<device.getInterfaceCount();i++) {
            UsbInterface f=device.getInterface(i);
            if(f.getId()!=4||f.getInterfaceClass()!=255||f.getInterfaceSubclass()!=0x43||f.getInterfaceProtocol()!=1) continue;
            for(int j=0;j<f.getEndpointCount();j++) {
                UsbEndpoint ep=f.getEndpoint(j);
                if(ep.getType()!=UsbConstants.USB_ENDPOINT_XFER_BULK) continue;
                if(ep.getAddress()==0x85) in=ep;
                if(ep.getAddress()==0x04) out=ep;
            }
            if(in!=null&&out!=null) { chosen=f;break; }
        }
        if(chosen==null) throw new IOException("Brak interfejsu menu. Telefon musi działać jako host USB/OTG, a gogle jak przy połączeniu z PC.");
        UsbDeviceConnection c=manager.openDevice(device);
        if(c==null) throw new IOException("Nie otwarto USB. Zamknij DJI Fly i spróbuj ponownie.");
        if(!c.claimInterface(chosen,true)) {c.close();throw new IOException("Interfejs USB zajęty. Zamknij DJI Fly i podłącz przewód ponownie.");}
        conn=c;face=chosen;input=in;output=out;
        log.write("USB: zweryfikowane N3, interfejs 4, OUT04/IN85");
    }
    private int next() { sequence=(sequence+1)&65535;return sequence; }
    public byte[] read(int node,int field) throws Exception { return exchange(Duml.read(next(),node,field),500); }
    public void event(String key,int event) throws Exception {
        byte[] result=exchange(Duml.key(next(),key,event),1300);
        if(result.length!=1||result[0]!=0) throw new IOException("Gogle nie potwierdziły klawisza.");
        log.write("GUI key "+key+" "+(event==0?"down":"up")+" ACK00");
    }
    private byte[] exchange(byte[] request,int timeout) throws Exception {
        if(conn.bulkTransfer(output,request,request.length,500)!=request.length) throw new IOException("Przerwany zapis USB.");
        long deadline=SystemClock.elapsedRealtime()+timeout;
        byte[] buf=new byte[16384];
        while(SystemClock.elapsedRealtime()<deadline) {
            int n=conn.bulkTransfer(input,buf,buf.length,80);
            if(n<=0) continue;
            for(byte[] r:parser.feed(buf,n)) if(Duml.matches(request,r)) return Duml.payload(r);
        }
        throw new IOException("Brak odpowiedzi menu N3. Sprawdź kabel i rolę USB telefonu.");
    }
    @Override public void close() { conn.releaseInterface(face);conn.close(); }
}
