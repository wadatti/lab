import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    // シミュレータのスレッド本体
    private MainThread[] threads;
    // 各スレッドがsynchronizeロックに使用するオブジェクト
    private Object[] locks;
    // RPC用オブジェクト
    Registry rmiregistry;

    // Socket
    private SocketThread socket_thread;

    private ExecutorService executor;

    public static volatile boolean running = true;

    public static void main(String[] args) {
        try {
            // ノードのIPアドレス取得
            Config.IP = InetAddress.getLocalHost().getHostAddress();
            // socketで使用するport番号
            Config.PORT = Integer.parseInt(args[0]);
            // シミュレータのスレッド数
            Config.THREAD_NUM = Integer.parseInt(args[1]);
            // 1スレッドあたりのイベント数
            Config.OUTER_LOOP = Integer.parseInt(args[2]);
            // rmiのip
            Config.RMI_IP = args[3];
            Config.IP = Config.RMI_IP;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

        Main main = new Main();
        /*************************************************************/
        main.start();
        /*************************************************************/

        System.exit(0);

    }

    public Main() {
        // lock用オブジェクト生成
        locks = new Object[Config.LOCK_NUM];
        for (int i = 0; i < Config.LOCK_NUM; i++) {
            locks[i] = new Object();
        }
        // ExecutorService生成
        executor = Executors.newSingleThreadExecutor();

        // Socket用Thread
        socket_thread = new SocketThread(locks, executor);

        // RMIオブジェクト設置
        RmiInit();

        // main thread生成
        threads = new MainThread[Config.THREAD_NUM];
        for (int i = 0; i < Config.THREAD_NUM; i++) {
            threads[i] = new MainThread();
        }
    }

    public void start() {
        running = true;
        Debug.print("*** START RUNNING ***");

        socket_thread.start();
        try {
            // socket用Threadの起動を待つ
            Thread.sleep(500);
        } catch (Exception e) {
            Debug.printErr("ERROR:thread interrupted.");
            System.exit(2);
        }

        // main threadの起動
        for (Thread t : threads) {
            t.start();
        }

        // main threadの終了を待つ
        try {
            for (Thread t : threads) {
                t.join();
            }
            running = false;
            Debug.print("*** STOP RUNNING ***");
        } catch (InterruptedException e) {
            Debug.printErr("InterruptedException::Thread.sleep @ After running");
        }
    }

    public void RmiInit() {
        try {
            Debug.print("trying connect rpc...");
            RmiImpl rmi = new RmiImpl();
            Naming.bind(Config.RPC_TAG(), rmi);
            Debug.print("success connect rpc:" + Config.RPC_TAG());
        } catch (Exception e) {
            Debug.printErr("ERROR::RPC cannot bind.");
            e.printStackTrace();
            System.exit(2);
        }
    }
}
