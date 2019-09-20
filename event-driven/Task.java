import java.util.Random;

/**
 * task
 */
public class Task implements Runnable {
    int ID;

    /**
     * constructor of task
     *
     * @param threadID
     */
    public Task(int threadID) {
        this.ID = threadID;
    }

    @Override
    public void run() {
        try {
            System.out.println("Task start:" + ID);
            Random rndSeed = new Random(System.currentTimeMillis() + ID);
            int Time = rndSeed.nextInt(1000);
            Thread.sleep(Time);
            System.out.println("Task end:" + ID + " Time:" + Time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
