import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        MyThreadPool executor = new MyThreadPool(2);
        for (int i = 0; i < 10; i++) {
            executor.execute(new Task(i));
        }
        executor.shutdown();
    }
}
