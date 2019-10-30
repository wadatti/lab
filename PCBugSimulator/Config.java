
public class Config {

    // Thread num / 1 node
    public static int THREAD_NUM = 5;
    // event num / 1 thread
    public static int OUTER_LOOP = 50;

    public static String RMI_IP = "192.168.2.67";
    public static int RMI_PORT = 1099;

    // Port
    static String IP = "";
    public static int PORT;

    public static String RPC_TAG() {
        return String.format("rmi://%s:%d/%s:%d", RMI_IP, RMI_PORT, IP, PORT);
    }


    // RAITO X * 10 -> X%
    public static final int RAITO_SUM = 1000;
    public static final int R_FIELD_ACCESS = 80 * 10;
    public static final int R_LOCK = 3 * 10;
    public static final int R_FORK = 2 * 10;
    public static final int R_WAIT = 1 * 10;
    public static final int R_EVENT = 1 * 10;
    public static final int R_RPC = 12 * 10;
    public static final int R_SOCKET = 1 * 10;


    // FIELD ACCESS CONTROL
    public static final int R_STATIC_FIELD = 5 * 10; /* /1000 */
    public static final int R_READ = 75 * 10; /* /1000 */

    public static final int STATIC_VARIABLE_NUM = 10;
    public static final int LOCAL_VARIABLE_NUM = 20;
    public static final int LOCK_NUM = 10;
    public static final int INNER_LOOP = 5;
}