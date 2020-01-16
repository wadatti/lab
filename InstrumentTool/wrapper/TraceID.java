package wrapper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TraceID class generates a unique tag in target program
 */
public class TraceID {
    private static AtomicInteger id = new AtomicInteger(0);

    public static synchronized int getID() {
        if (id.get() == 0) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                Random rand = new Random(System.currentTimeMillis());
                id.set(ByteBuffer.wrap(digest.digest(InetAddress.getLocalHost().getHostName().getBytes())).getInt() - rand.nextInt());
            } catch (UnknownHostException | NoSuchAlgorithmException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return id.incrementAndGet();
    }
}
