package pl.stasz.n3manual;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Only the two observed N3 GUI commands are constructible here. */
public final class Duml {
    private Duml() {}
    public static int crc(byte[] data, int count, int seed, int poly) {
        int r = seed;
        for (int i=0;i<count;i++) {
            r ^= data[i]&255;
            for(int b=0;b<8;b++) r=(r>>>1)^((r&1)!=0?poly:0);
        }
        return r;
    }
    public static int word(byte[] a,int n) { return (a[n]&255)|((a[n+1]&255)<<8); }
    public static int keyCode(String name) {
        switch(name) {
            case "left": return 6; case "right": return 7;
            case "up": return 4; case "down": return 5;
            case "ok": return 1; case "back": return 2;
            default: throw new UiFailure("error_unknown_key");
        }
    }
    public static byte[] read(int seq,int node,int field) {
        return read(seq,node,field,10);
    }
    public static byte[] read(int seq,int node,int field,int sender) {
        if(node<0||node>1024||!(field==0||field==2||field==3||field==4||field==9||field==11))
            throw new UiFailure("error_read_range");
        byte[] p=ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(node).putShort((short)field).put((byte)(field==9?1:0)).putShort((short)0).array();
        return guiFrame(seq,0,p,sender);
    }
    public static byte[] key(int seq,String name,int event) {
        return key(seq,name,event,10);
    }
    public static byte[] key(int seq,String name,int event,int sender) {
        if(event!=0&&event!=1) throw new UiFailure("error_key_event");
        byte[] p=ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0).putShort((short)1).put((byte)event).putInt(keyCode(name)).put((byte)0).array();
        return guiFrame(seq,1,p,sender);
    }
    private static byte[] guiFrame(int seq,int cmd,byte[] p,int sender) {
        if(sender!=2&&sender!=10)throw new UiFailure("error_client_address");
        return frame(seq,sender,28,0x40,83,cmd,p);
    }
    private static byte[] frame(int seq,int sender,int receiver,int flags,int set,int cmd,byte[] p) {
        byte[] a=new byte[13+p.length]; a[0]=0x55;
        int length=a.length|0x400; a[1]=(byte)length; a[2]=(byte)(length>>>8);
        a[3]=(byte)crc(a,3,0x77,0x8c); a[4]=(byte)sender; a[5]=(byte)receiver;
        a[6]=(byte)seq; a[7]=(byte)(seq>>>8); a[8]=(byte)flags; a[9]=(byte)set; a[10]=(byte)cmd;
        System.arraycopy(p,0,a,11,p.length);
        int sum=crc(a,a.length-2,0x3692,0x8408);
        a[a.length-2]=(byte)sum; a[a.length-1]=(byte)(sum>>>8); return a;
    }
    /** Answer only transport identity/heartbeat, never generic FC requests. */
    public static byte[] linkReply(byte[] request) {
        if(request.length<13||(request[8]&0x80)!=0||(request[8]&0x60)==0
            ||request[5]!=2||request[9]!=0)return null;
        int peer=request[4]&255, cmd=request[10]&255;byte[] p;
        if(peer==188&&cmd==0x81&&request.length>=13+48) {
            p=new byte[64];p[1]='A';p[2]='P';p[3]='P';p[34]=2;
            System.arraycopy(request,11+32,p,41,8);
        } else if(peer==188&&cmd==0x82) p=new byte[]{0};
        else if(peer==60&&cmd==0x88&&request.length>=15&&request[11]==0x19) p=new byte[]{0x1a,0,0,0,0};
        else return null;
        return frame(word(request,6),2,peer,0x80,0,cmd,p);
    }
    public static boolean matches(byte[] request,byte[] response) {
        return response.length>=13&&(response[8]&0x80)!=0
            &&response[4]==request[5]&&response[5]==request[4]
            &&word(response,6)==word(request,6)&&response[9]==request[9]&&response[10]==request[10];
    }
    public static byte[] payload(byte[] frame) { return Arrays.copyOfRange(frame,11,frame.length-2); }
    public static final class Parser {
        private byte[] buf=new byte[0];
        public List<byte[]> feed(byte[] data,int count) {
            if(count<0||count>data.length||buf.length+count>65536) throw new UiFailure("error_usb_buffer");
            byte[] joined=Arrays.copyOf(buf,buf.length+count);
            System.arraycopy(data,0,joined,buf.length,count);
            List<byte[]> out=new ArrayList<>(); int i=0;
            while(joined.length-i>=4) {
                byte[] head=Arrays.copyOfRange(joined,i,i+4);
                int lv=word(head,1), size=lv&1023;
                if(head[0]!=0x55||(lv>>>10)!=1||size<13||(crc(head,3,0x77,0x8c)&255)!=(head[3]&255)) { i++;continue; }
                if(joined.length-i<size) break;
                byte[] frame=Arrays.copyOfRange(joined,i,i+size);
                if(crc(frame,size-2,0x3692,0x8408)!=word(frame,size-2)) {i++;continue;}
                out.add(frame);i+=size;
            }
            buf=Arrays.copyOfRange(joined,i,joined.length); return out;
        }
    }
}
