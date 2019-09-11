import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        MyThreadPool exec = new MyThreadPool(5);
        for (int i = 0; i < 10; i++) {
            exec.execute(new Task(i));
        }
        exec.shutdown();
    }
}