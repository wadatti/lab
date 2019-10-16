import java.util.HashMap;
import java.util.Map;

public class Timer {
    private static Map<String, Long> time = new HashMap<>();

    public static void start(String name) {
//        System.out.println("start: " + name);
        time.put(name, System.currentTimeMillis());
    }

    public static void stop(String name) {
        long end = System.currentTimeMillis();
        System.out.println((end - time.remove(name))/1000. + "s: " + name);
    }
}
