import java.util.Random;

/**
 * task
 */
public class Task implements Runnable {
    int taskID;

    /**
     * constructor of task
     *
     * @param threadID
     */
    public Task(int threadID) {
        this.taskID = threadID;
    }

    @Override
    public void run() {
        try {
            Random rndSeed = new Random(System.currentTimeMillis() + taskID);
            int Time = rndSeed.nextInt(1000);
            Thread.sleep(Time);
            System.out.println("Task end:" + taskID + " Time:" + Time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}