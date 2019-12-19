package wrapper;

/**
 * ThreadWrapper class wraps java.lang.Thread
 */
public class ThreadWrapper extends Thread {
    private static int ThreadWrapperID = 0;
    private Runnable task;

    public ThreadWrapper() {
        super();
        ThreadWrapperID = TraceID.getID();
    }

    public ThreadWrapper(Runnable task) {
        super(task);
        this.task = task;
        ThreadWrapperID = TraceID.getID();
    }

    public ThreadWrapper(Runnable task, String name) {
        super(task, name);
        this.task = task;
        ThreadWrapperID = TraceID.getID();
    }

    public ThreadWrapper(String name) {
        super(name);
        ThreadWrapperID = TraceID.getID();

    }

    public ThreadWrapper(ThreadGroup group, Runnable task) {
        super(group, task);
        this.task = task;
        ThreadWrapperID = TraceID.getID();
    }

    public ThreadWrapper(ThreadGroup group, Runnable task, String name) {
        super(group, task, name);
        this.task = task;
        ThreadWrapperID = TraceID.getID();
    }

    public ThreadWrapper(ThreadGroup group, Runnable task, String name, long stackSize) {
        super(group, task, name, stackSize);
        this.task = task;
        ThreadWrapperID = TraceID.getID();
    }

    public ThreadWrapper(ThreadGroup group, String name) {
        super(group, name);
        ThreadWrapperID = TraceID.getID();
    }

    public int getID() {
        if (this.task == null) {
            return ThreadWrapperID;
        }
        return task.hashCode();
    }
}