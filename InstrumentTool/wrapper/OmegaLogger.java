package wrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class OmegaLogger {
    public static final Log LOG = LogFactory.getLog(OmegaLogger.class);
    private static File file;

    static {
        try {
            file = new File("/var/log/OmegaLog/Omega_" + InetAddress.getLocalHost().getHostName() + ".log");
            file.setExecutable(true, false);
            file.setReadable(true, false);
            file.setWritable(true, false);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void LogOut(String s) {
        LOG.info(s);
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