import java.util.List;
import java.util.concurrent.RecursiveTask;

public class ParallelSum extends RecursiveTask<Integer> {

    private static final int THRESHOLD = 16;

    private final List<Integer> list;

    public ParallelSum(List<Integer> list) {
        this.list = list;
    }

    @Override
    protected Integer compute() {
        if (this.list.size() < THRESHOLD) {
            int sum = 0;
            for (Integer i : this.list) sum += i;
            return sum;
        } else {
            int m = this.list.size() / 2;
            ParallelSum ps1 = new ParallelSum(this.list.subList(0, m));
            ps1.fork();
            ParallelSum ps2 = new ParallelSum(this.list.subList(m, this.list.size()));
            ps2.fork();

            return ps1.join() + ps2.join();
        }
    }
}