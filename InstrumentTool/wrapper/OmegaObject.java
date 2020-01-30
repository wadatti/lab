package wrapper;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class OmegaObject {
    public static ReentrantLock lock = new ReentrantLock();
    public static AtomicInteger num = new AtomicInteger(0);
}
