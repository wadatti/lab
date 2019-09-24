import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class MyServerSocket {
    private ServerSocket sSocket = null;
    private Socket socket = null;
    private BufferedReader reader = null;
    private PrintWriter writer = null;
    private String line = null;

    void runSocket() {
        try {
            sSocket = new ServerSocket();
            sSocket.bind(new InetSocketAddress("localhost", 7777));
            System.out.println("wait for client");

            //クライアントからの要求を待つ
            socket = sSocket.accept();

            System.out.println("Client (" + InetAddress.getLocalHost().getHostAddress() + ") > Accepted connection from " + socket.getRemoteSocketAddress());

            //クライアントからの受取用
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //クライアントへの送信用
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void stopSocket() {
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
            if (sSocket != null) {
                sSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String receiveString() {
        try {
            line = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (line.equals("end")) {
            stopSocket();
        }

        sendString("Thank you for your message: " + line);

        return line;
    }

    void sendString(String mes) {
        writer.println(mes);
    }

}
