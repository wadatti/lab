package instrument.tool;

/**
 * ログ出力用クラス
 */
public class LogCode {

    public static String out(String type, String hash, String cName, int line) {
        String text = LogOut(type, hash, cName, line);
        return "System.out.println(\"" + text + "\");";

    }

    public static String LogOut(String type, String hash, String cName, int line) {
        String tid = "\"+Thread.currentThread().getId()+\"";
        String pid = "\"+java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split(\"@\")[0]+\"";
        return String.format("[TraceLog] %s, %s, %s, %s, %s, %d", type, hash, tid, pid, cName, line);
    }

    public static String out(String type, String hash, String cName, String mName, int line) {
        String text = LogOut(type, hash, cName, mName, line);
        return "System.out.println(\"" + text + "\");";

    }

    public static String LogOut(String type, String hash, String cName, String mName, int line) {
        String tid = "\"+Thread.currentThread().getId()+\"";
        String pid = "\"+java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split(\"@\")[0]+\"";
        return String.format("[TraceLog] %s, %s, %s, %s, %s, %s, %d", type, hash, tid, pid, cName, mName, line);
    }

    public static String out(String type, String hash, String cName, String mName, String currentClass, int line) {
        String text = LogOut(type, hash, cName, mName, currentClass, line);
        return "System.out.println(\"" + text + "\");";

    }

    public static String LogOut(String type, String hash, String cName, String mName, String currentClass, int line) {
        String tid = "\"+Thread.currentThread().getId()+\"";
        String pid = "\"+java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split(\"@\")[0]+\"";
        return String.format("[TraceLog] %s, %s, %s, %s, %s, %s, %s, %d", type, hash, tid, pid, cName, mName, currentClass, line);
    }

    public static String LogOutTail(String cName, int line) {
        return String.format(", %s, %d", cName, line);
    }
}
