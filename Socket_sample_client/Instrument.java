import javassist.*;

import java.io.IOException;

public class Instrument {

    private static void instrumentMyClientSocket() throws NotFoundException, IOException, CannotCompileException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass myClientSocket = classPool.get("MyClientSocket");

        CtMethod sendMethod = myClientSocket.getDeclaredMethod("sendString");
        sendMethod.insertAfter("java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", true);\n" +
                "    fw.write(\"SendSocket(\" + mes + \",\" + java.net.InetAddress.getLocalHost().getHostName() + \")\\n\");\n" +
                "    fw.close();");

        CtMethod receiveMethod = myClientSocket.getDeclaredMethod("receiveString");
        receiveMethod.insertAfter("java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", true);\n" +
                "    fw.write(\"ReceiveSocket(\" + line + \",\" + java.net.InetAddress.getLocalHost().getHostName() + \")\\n\");\n" +
                "    fw.close();");

        myClientSocket.writeFile();
    }

//    java.io.FileWriter fw = new java.io.FileWriter("log.txt", true);
//    fw.write("SendSocket(" + mes + "," + java.net.InetAddress.getLocalHost().getHostName() + ")\n");
//    fw.close();

    public static void instrumentMain() throws NotFoundException, CannotCompileException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass mainClass = classPool.get("Main");
        CtMethod mainMethod = mainClass.getDeclaredMethod("main");
        mainMethod.insertBefore("java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", false);\n" +
                "    fw.write(\"----------------(message,hostName)\\n\");\n" +
                "    fw.close();");
        mainClass.writeFile();

    }

//    java.io.FileWriter fw = new java.io.FileWriter("log.txt", false);
//    fw.write("----------------(taskName,ThreadID,taskID)\n");
//    fw.close();

    public static void main(String[] args) throws IOException, CannotCompileException, NotFoundException {
        instrumentMyClientSocket();
        instrumentMain();
    }
}
