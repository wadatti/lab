import java.io.IOException;

public class WrapperRunnable implements Runnable {
    int eventID;

    @Override
    public void run() {
    }

    void begin() throws IOException {
        MyLogger.writeLog("ThreadBegin(" + this.getClass().getName() + "," + Thread.currentThread().getId() + "," + this.eventID + ")\n");
    }

    void end() throws IOException {
        MyLogger.writeLog("ThreadEnd(" + this.getClass().getName() + "," + Thread.currentThread().getId() + "," + this.eventID + ")\n");
    }
}