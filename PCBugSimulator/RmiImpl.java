import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

public class RmiImpl extends UnicastRemoteObject implements RmiInterface {

    protected RmiImpl() throws RemoteException {
    }

    protected RmiImpl(int port) throws RemoteException {
        super(port);
    }

    protected RmiImpl(int port, RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
        super(port, csf, ssf);
    }

    @Override
    public String getMessage() {
        return "Test RMI";
    }
}
