package wrapper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TraceID class generates a unique tag in target program
 */
public class TraceID {
    private static AtomicInteger id;

    static {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            id = new AtomicInteger(ByteBuffer.wrap(digest.digest(InetAddress.getLocalHost().getHostName().getBytes())).getInt());
        } catch (UnknownHostException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    public static synchronized int getID() {
        return id.incrementAndGet();
    }
}
