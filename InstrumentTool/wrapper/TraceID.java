package wrapper;

import java.util.concurrent.atomic.AtomicInteger;

public class TraceID {
    private static AtomicInteger id = new AtomicInteger(0);

    public static int getID() {
        return id.incrementAndGet();
    }
}
