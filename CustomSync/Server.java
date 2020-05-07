import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;


/**
 * RMI registry server
 * port: 7777
 */
public class Server implements Sync {
    private Map<Integer, String> taskMap = new HashMap<>();

    public static void main(String[] args) throws RemoteException, AlreadyBoundException, InterruptedException {
        Server obj = new Server();
        Sync stub = (Sync) UnicastRemoteObject.exportObject(obj, 0);
        Registry registry = LocateRegistry.getRegistry(7777);   // port 7777
        registry.bind("sync", stub);

        System.out.println("Server ready");

        Thread.sleep(5000);

        obj.taskMap.put(1, "Task1");
        System.out.println("put");
    }

    @Override
    public boolean sync() throws RemoteException {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (taskMap.containsKey(1)) {
            return true;
        }
        return false;
    }
}
