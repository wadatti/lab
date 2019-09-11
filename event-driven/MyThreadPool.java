import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * original thread pool
 */
public class MyThreadPool {
    private ExecutorService exec;
    private Queue<Runnable> taskQueue = new ArrayDeque<Runnable>();
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
        java.io.FileWriter fw = new FileWriter("log.txt", false);
        fw.write("execute log\n");
        fw.close();
        taskQueue.add(task);
    }

    /**
     * shutdown thread pool
     */
    public void shutdown() {
        while (taskQueue.peek() != null) {
        }
        exec.shutdown();
    }
}
