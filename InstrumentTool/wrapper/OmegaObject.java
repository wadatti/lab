package wrapper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class OmegaObject {
    public static AtomicBoolean flag = new AtomicBoolean(false);
    public static AtomicInteger waitNum = new AtomicInteger(0);
}
