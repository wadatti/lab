import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * RMI client
 * port: 7777
 */
public class Client {
    public static void main(String[] args) throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(7777);   // port 7777
        Sync stub = (Sync) registry.lookup("sync");
        while (!stub.sync()) {
            System.out.println("waiting for another node...");
        }
        System.out.println("Synchronize!!");
    }

}
