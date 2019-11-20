package wrapper;

public class WrapperThread extends Thread {
    private Runnable task;

    public WrapperThread(Runnable task) {
        super(task);
        this.task = task;
    }

    public WrapperThread(Runnable task, String name) {
        super(task, name);
        this.task = task;
    }

    public WrapperThread(ThreadGroup group, Runnable task) {
        super(group, task);
        this.task = task;
    }

    public WrapperThread(ThreadGroup group, Runnable task, String name) {
        super(group, task, name);
        this.task = task;
    }

    public WrapperThread(ThreadGroup group, Runnable task, String name, long stackSize) {
        super(group, task, name, stackSize);
        this.task = task;
    }

    public WrapperThread(ThreadGroup group, String name) {
        super(group, name);
    }

    public Runnable getTask() {
        return task;
    }
}