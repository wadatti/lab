import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;


public class SocketThread extends Thread {
    ServerSocket ss;

    public SocketThread(Object[] obj, ExecutorService executor) {
        try {
            ss = new ServerSocket(Config.PORT);
        } catch (Exception e) {
            Debug.printErr("ERROR::Port alredy used." + Config.PORT);
            System.exit(2);
        }
    }

    @Override
    public void run() {

    }

}
