package pl.stasz.n3manual;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/** N3 mobile ports: OUT22345, IN30000 control; older models also use IN22345. */
public final class LogicTunnel {
    private byte[] pending=new byte[0];
    public static byte[] wrap(byte[] gui) {
        byte[] out=new byte[8+gui.length];out[0]=0x55;out[1]=(byte)0xcc;out[2]=0x49;out[3]=0x57;
        ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).putInt(4,gui.length);
        System.arraycopy(gui,0,out,8,gui.length);return out;
    }
    public List<byte[]> feed(byte[] bytes,int size) {
        if(size<0||size>bytes.length||pending.length+size>131072)throw new UiFailure("error_tunnel_buffer");
        byte[] all=Arrays.copyOf(pending,pending.length+size);System.arraycopy(bytes,0,all,pending.length,size);
        List<byte[]> controls=new ArrayList<>();int at=0;
        while(all.length-at>=8) {
            int port=(all[at+2]&255)|((all[at+3]&255)<<8);
            boolean control=port==22345||port==30000;
            boolean video=port==22346||port==22347;
            if(all[at]!=0x55||all[at+1]!=(byte)0xcc||(!control&&!video)){at++;continue;}
            int len=ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN).getInt(at+4);
            if(len<0||len>65536){at++;continue;}
            if(all.length-at<8+len)break;
            if(control&&len>0)controls.add(Arrays.copyOfRange(all,at+8,at+8+len));
            at+=8+len;
        }
        pending=Arrays.copyOfRange(all,at,all.length);return controls;
    }
}
