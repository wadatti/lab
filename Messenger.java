import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote object
 */
public interface Messenger extends Remote {

    /**
     * send client's message
     *
     * @param message
     * @return client's message
     * @throws RemoteException
     */
    public default String send(String message) throws RemoteException {
        System.out.println("client says " + message + ".");
        return "send" + message + "toserver.";
    }
}
