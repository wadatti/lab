import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * taskQueue dispatcher thread
 */
public class DispatchTask implements Runnable {

    Queue<Runnable> taskQueue;
    ExecutorService exec;

    /**
     * constructor of DispatchTask
     *
     * @param taskQueue
     * @param exec
     */
    public DispatchTask(Queue<Runnable> taskQueue, ExecutorService exec) {
        this.taskQueue = taskQueue;
        this.exec = exec;
    }

    @Override
    public void run() {
        while (true) {
            Runnable task = taskQueue.poll();
            if (task != null) {
                exec.execute(task);
            }
        }
    }
}
