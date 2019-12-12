package wrapper;

public class WrapperThread extends Thread {
    private static int wrapperThreadID = 0;

    public WrapperThread() {
        super();
        wrapperThreadID = TraceID.getID();
    }

    public WrapperThread(Runnable task) {
        super(task);
        wrapperThreadID = TraceID.getID();
    }

    public WrapperThread(Runnable task, String name) {
        super(task, name);
        wrapperThreadID = TraceID.getID();
    }

    public WrapperThread(String name) {
        super(name);
        wrapperThreadID = TraceID.getID();

    }

    public WrapperThread(ThreadGroup group, Runnable task) {
        super(group, task);
        wrapperThreadID = TraceID.getID();
    }

    public WrapperThread(ThreadGroup group, Runnable task, String name) {
        super(group, task, name);
        wrapperThreadID = TraceID.getID();
    }

    public WrapperThread(ThreadGroup group, Runnable task, String name, long stackSize) {
        super(group, task, name, stackSize);
        wrapperThreadID = TraceID.getID();
    }

    public WrapperThread(ThreadGroup group, String name) {
        super(group, name);
        wrapperThreadID = TraceID.getID();

    }

    public int getID() {
        return wrapperThreadID;
    }
}