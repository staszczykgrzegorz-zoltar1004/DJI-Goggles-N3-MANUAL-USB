package pl.stasz.n3manual;
import java.io.Closeable;
public interface MenuLink extends Closeable {
    byte[] read(int node,int field) throws Exception;
    void event(String key,int event) throws Exception;
}
