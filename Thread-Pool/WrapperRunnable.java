import java.io.IOException;

public class WrapperRunnable implements Runnable {
    int eventID;

    @Override
    public void run() {
    }

    void begin() throws IOException {
        java.io.FileWriter fw = new java.io.FileWriter("log.txt", true);
        fw.write("ThreadBegin(" + this.getClass().getName() + "," + Thread.currentThread().getId() + "," + this.eventID + ")\n");
        fw.close();
    }

    void end() throws IOException {
        java.io.FileWriter fw = new java.io.FileWriter("log.txt", true);
        fw.write("ThreadEnd(" + this.getClass().getName() + "," + Thread.currentThread().getId() + "," + this.eventID + ")\n");
        fw.close();
    }
}