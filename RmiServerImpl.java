import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Rmi Server implementation
 */
public class RmiServerImpl implements RmiServer {

    private String host;
    private int port;
    private String serverName;
    private Remote remoteObject;

    public RmiServerImpl(String host, int port, String serverName, Remote remoteObject) {
        this.host = host;
        this.port = port;
        this.serverName = serverName;
        this.remoteObject = remoteObject;
    }

    @Override
    public void start() throws RemoteException, MalformedURLException {
        startViaProcess(host, port, serverName, remoteObject);
    }

    /**
     * server main program
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            int port = 7777;
            String serverName = "server";
            Messenger messenger = new MessengerImpl();

            RmiServer server = new RmiServerImpl(host, port, serverName, messenger);
            server.start();

        } catch (UnknownHostException | RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }

    }

}
