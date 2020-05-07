import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Sync extends Remote {
    boolean sync() throws RemoteException;
}
