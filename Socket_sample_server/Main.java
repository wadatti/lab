import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) throws UnknownHostException {
        MyServerSocket sSocket = new MyServerSocket();
        sSocket.runSocket();

        System.out.println(sSocket.receiveString());
        System.out.println(java.net.InetAddress.getLocalHost().getHostName());

        sSocket.stopSocket();
    }
}
