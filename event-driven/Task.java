import java.util.Random;

/**
 * task
 */
public class Task implements Runnable {
    int threadID;

    /**
     * constructor of task
     *
     * @param threadID
     */
    public Task(int threadID) {
        this.threadID = threadID;
    }

    @Override
    public void run() {
        try {
            Random rndSeed = new Random(System.currentTimeMillis() + threadID);
            int Time = rndSeed.nextInt(1000);
            Thread.sleep(Time);
            System.out.println("Task end:" + threadID + " Time:" + Time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
