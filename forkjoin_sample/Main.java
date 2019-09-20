import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class Main {

    public static void main(String[] args) {
        List<Integer> intList = newIntegerList();

        ForkJoinPool forkJoin = new ForkJoinPool();
        int n = forkJoin.invoke(new ParallelSum(intList));

        System.out.println(n);
    }

    private static List<Integer> newIntegerList() {
        int p = 4, n = 1;
        for (int i = 0; i < p; i++) n *= 2;
        List<Integer> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(i);

        return list;
    }
}