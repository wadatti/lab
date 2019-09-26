import java.io.FileWriter;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

class MyLogger extends Thread {
    FileWriter fw = null;
    private static Queue<String> messageQueue = new ConcurrentLinkedDeque<>();
    Thread mainThread = null;


    public MyLogger(Thread mainThread) throws IOException {
        fw = new FileWriter("log.txt", false);
        fw.write("------Event(eventKind,ThreadID,eventID)\n");
        fw.close();
        this.mainThread = mainThread;
    }

    @Override
    public void run() {
        while (true) {
            if (!messageQueue.isEmpty()) {
                try {
                    fw = new FileWriter("log.txt", true);
                    fw.write(messageQueue.poll() + "\n");
                    fw.close();
                } catch (IOException e) {
                    System.out.println(e);
                }
            } else if (!mainThread.isAlive()) {
                break;
            }
        }
    }

    public static void writeLog(String mes) {
        if (!messageQueue.offer(mes)) {
            System.out.println("over!!!!");
        }
    }
}