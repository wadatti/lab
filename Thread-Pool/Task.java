public class Task implements Runnable {
    int taskId;

    public Task(int id) {
        taskId = id;
    }

    @Override
    public void run() {
        try {
            System.out.println("Task Start:" + taskId);
            Thread.sleep(1000);
            System.out.println("Task End:" + taskId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
