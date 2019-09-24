import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MyClientSocket {
    Socket cSocket = null;
    PrintWriter writer = null;
    BufferedReader reader = null;
    String line = null;

    void runSocket() {
        try {
            cSocket = new Socket("localhost", 7777);

            writer = new PrintWriter(cSocket.getOutputStream(), true);

            reader = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
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
            if (cSocket != null) {
                cSocket.close();
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
        return line;
    }

    void sendString(String mes) {
        writer.println(mes);
        if (line == "end") {
            stopSocket();
        }
    }
}
