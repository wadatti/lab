import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * original thread pool
 */
public class MyThreadPool {
    private ExecutorService exec;
    private Queue<Runnable> taskQueue = new ConcurrentLinkedDeque<>();
    private DispatchTask dispatchTask;

    /**
     * constructor of MyThreadPool
     *
     * @param size
     */
    public MyThreadPool(int size) {
        exec = Executors.newFixedThreadPool(size);
        dispatchTask = new DispatchTask(taskQueue, exec);
        Thread t = new Thread(dispatchTask);
        t.setDaemon(true);
        t.start();
    }

    /**
     * execute task
     *
     * @param task
     */
    public void execute(Runnable task) throws IOException {
        taskQueue.add(task);
    }

    /**
     * shutdown thread pool
     */
    public void shutdown() {
        while (taskQueue.size() != 0) {
        }
        exec.shutdown();
    }
}
