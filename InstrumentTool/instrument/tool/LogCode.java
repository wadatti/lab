package instrument.tool;

/**
 * ログ出力用クラス
 */
public class LogCode {


    public static String out(String type, String hash, String cName, String mName, int line) {
        String text = logFormat(type, hash, cName, mName, line);
        return "wrapper.OmegaLogger.LogOutPutFile(\"" + text + "\");";
    }

    public static String byteOut(String type, String hash, String cName, String mName, int line) {
        String text = byteFormat(type, hash, cName, mName, line);
        return "wrapper.OmegaLogger.LogOutPutFile(\"" + text + "\");";
    }

    private static String logFormat(String type, String hash, String cName, String mName, int line) {
        String tid = "\"+Thread.currentThread().getId()+\"";
        String pid = "\"+java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split(\"@\")[0]+\"";
        return String.format("[TraceLog] %s, %s, %s, %s, %s, %s, %d", type, hash, tid, pid, cName, mName, line);
    }

    private static String byteFormat(String type, String hash, String cName, String mName, int line) {
        String tid = "\"+Thread.currentThread().getId()+\"";
        String pid = "\"+java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split(\"@\")[0]+\"";
        String bytes = "\"+wrapper.OmegaLogger.byteOutput(" + hash + ")+\"";
//                +"wrapper.OmegaLogger.byteOutput(hash)"+
        return String.format("[TraceLog] %s, %s, %s, %s, %s, %s, %d", type, bytes, tid, pid, cName, mName, line);
    }


    public static String LogOutTail(String cName, int line) {
        return String.format(", %s, %d", cName, line);
    }
}
