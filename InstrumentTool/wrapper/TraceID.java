package wrapper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TraceID class generates a unique tag in target program
 */
public class TraceID {
    private static AtomicInteger id = new AtomicInteger(0);
    private static Map<Object, Integer> objectMap = new ConcurrentHashMap<>();
    public static final int MAX_BYTES_TO_READ = 64 * 1024;


    public static synchronized int getID() {
        if (id.get() == 0) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                Random rand = new Random(System.currentTimeMillis());
                id.set(ByteBuffer.wrap(digest.digest(InetAddress.getLocalHost().getHostName().getBytes())).getInt() - rand.nextInt());
            } catch (UnknownHostException | NoSuchAlgorithmException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return id.incrementAndGet();
    }

    public static void objectRegist(Object o) {
        int id = getID();
        if (objectMap.containsKey(o))
            return;
        if (objectMap.containsValue(id))
            OmegaLogger.LogOutPutFile("[TraceError] containsValue");
        objectMap.put(o, id);
    }

    public static int getObjectId(Object o) {
        Integer id = objectMap.get(o);
        if (id == null) {
            OmegaLogger.LogOutPutFile("[TraceError] nullId");
            return 0;
        }
        return id.intValue();
    }
}
