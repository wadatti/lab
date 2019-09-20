import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {
        ExecutorService exec = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 5; i++) {
            exec.execute(new Task(i));
        }
        exec.shutdown();
        System.out.println("main End");
    }
}
