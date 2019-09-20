import java.io.IOException;
import java.util.concurrent.RecursiveTask;

public abstract class WrapperRecursiveTask<V> extends RecursiveTask<V> {
    String taskName = this.getClass().getName();
    int taskID = 0;

    @Override
    protected abstract V compute();

    void begin() throws IOException {
        java.io.FileWriter fw = new java.io.FileWriter("log.txt", true);
        fw.write("ThreadBegin(" + this.taskName + "," + Thread.currentThread().getId() + "," + this.taskID + ")\n");
        fw.close();
    }

    void end() throws IOException {
        java.io.FileWriter fw = new java.io.FileWriter("log.txt", true);
        fw.write("ThreadEnd(" + this.taskName + "," + Thread.currentThread().getId() + "," + this.taskID + ")\n");
        fw.close();
    }
}
