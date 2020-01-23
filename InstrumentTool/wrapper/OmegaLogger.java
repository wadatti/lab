
package wrapper;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class OmegaLogger {
    private static File file;

    static {
        try {
            file = new File("/var/log/OmegaLog/Omega_" + InetAddress.getLocalHost().getHostName() + ".log");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void LogOutPutFile(String s) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            bw.write(s);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}