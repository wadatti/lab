import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class SocketReceiver extends Thread {
    ServerSocket ss;

    public SocketReceiver(Object[] obj, ExecutorService executor) {
        try {
            ss = new ServerSocket(Config.PORT);
        } catch (Exception e) {
            Debug.printErr("ERROR::Port alredy used." + Config.PORT);
            System.exit(2);
        }
    }


    @Override
    public void run() {
        try {
            Socket sSocket = ss.accept();
            Thread t = new SocketClient(sSocket);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class SocketClient extends Thread {
    Socket sc;
    ObjectInputStream ois;

    SocketClient(Socket s) {
        sc = s;
    }

    @Override
    public void run() {
        try {
            ois = new ObjectInputStream(sc.getInputStream());
        } catch (Exception e) {
            Debug.printErr("ERROR:: ObjectInputStream");
        }
        while (true) {
            try {
                Object obj = ois.readObject();
                Debug.print("socket通信受信");
            } catch (Exception e) {
                Debug.print("socket通信の終わり?");
                e.printStackTrace();
                try {
                    ois.close();
                    sc.close();
                    return;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Debug.printErr("ERROR:: socket close error.");
                }
            }
        }
    }
}