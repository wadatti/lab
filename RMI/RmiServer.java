import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * RMI Server
 */
public interface RmiServer {

    /**
     * start server
     *
     * @throws RemoteException
     * @throws MalformedURLException
     */
    public void start() throws RemoteException, MalformedURLException;

    /**
     * start server via Registry
     *
     * @param host
     * @param port
     * @param serverName
     * @param remoteObject
     * @throws RemoteException
     */
    public default void start(String host, int port, String serverName, Remote remoteObject) throws RemoteException {

        // rmi url
        String url = "rmi://" + host + ":" + Integer.toString(port) + "/" + serverName;

        // bind remote object to rmiregistry
        System.out.println("bind " + url + " to rmiregistry");
        Registry rmiregistry = LocateRegistry.createRegistry(port);
        rmiregistry.rebind(url, remoteObject);

        System.out.println("Rmi server starting...");

    }

    /**
     * start server via rmiregistry process
     *
     * @param host
     * @param port
     * @param serverName
     * @param remoteObject
     * @throws MalformedURLException
     * @throws RemoteException
     */
    public default void startViaProcess(String host, int port, String serverName, Remote remoteObject) throws MalformedURLException, RemoteException {

        // rmi url
        String url = "rmi://" + host + ":" + Integer.toString(port) + "/" + serverName;

        // bind remote object to rmiregistry
        System.out.println("bind " + url + " to rmiregistry");
        Naming.rebind(url, remoteObject);

        System.out.println("Rmi server starting...");
    }
}
