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
        // String pid = "0";//"\"+ProcessHandlecurrent().pid()+\"";
        String pid = "\"+java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split(\"@\")[0]+\"";
        return String.format("[TraceLog] %s, %s, %s, %s, %s, %d", type, hash, tid, pid, cName, line);
    }
}
