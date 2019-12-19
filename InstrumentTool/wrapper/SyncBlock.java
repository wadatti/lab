package wrapper;

public class SyncBlock {
    public static void begin(Object o) {
        System.out.println("synchronizedBlockBegin(" + o + ") ");
    }

    public static void end(Object o) {
        System.out.println("synchronizedBlockEnd(" + o + ")");
    }
}
