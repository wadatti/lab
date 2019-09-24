import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) throws UnknownHostException {
        MyClientSocket socket = new MyClientSocket();
        socket.runSocket();

        socket.sendString("Hello from Client");

        System.out.println(socket.receiveString());

        socket.stopSocket();

    }
}
