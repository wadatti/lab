import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RmiInterface extends Remote {
    public String getMessage() throws RemoteException;
}
