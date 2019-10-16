import java.util.*;
import java.util.function.BiFunction;

public class ReachabilityTester<V> {
    private List<V> dataset;
    private Map<String, BiFunction<V, V, Boolean>> functions = new LinkedHashMap<>();

    public ReachabilityTester(Collection<V> dataset) {
        this.dataset = new LinkedList<>(dataset);
    }

    public void put(String name, BiFunction<V, V, Boolean> function) {
        functions.put(name, function);
    }

    public void testReachability() {
        int d = 0;
        int size = dataset.size();
        int length = String.valueOf(size).length();
        int n = functions.size();
        List<BiFunction<V, V, Boolean>> fs = new ArrayList<>(functions.values());
        System.out.printf("TestReachability[%" + length + "d/%d]", d++, size);
        for (V source : dataset) {
            for (V target : dataset) {
                boolean[] b = new boolean[n];
                for (int i = 0; i < n; i++)
                    b[i] = fs.get(i).apply(source, target);
                for (int i = 1; i < n; i++)
                    if (b[0] != b[i]) {
                        System.err.println("ERROR: detect difference");
                        String[] names = functions.keySet().toArray(new String[n]);
                        for (int j = 0; j < n; j++)
                            System.err.println(names[j] + ": " + b[j]);
                        System.err.println();
                        break;
                    }
            }
            System.out.printf("\rTestReachability[%" + length + "d/%d]", d++, size);
        }
        System.out.println();
    }

    public long testPerformance(String name, int seconds) {
        BiFunction<V, V, Boolean> function = functions.get(name);
        long count = 0;
        long endMillis = System.currentTimeMillis() + seconds * 1000;
        while (true)
            for (V source : dataset)
                for (V target : dataset) {
                    function.apply(source, target);
                    count++;
                    if (System.currentTimeMillis() >= endMillis)
                        return count;
                }
    }

    public long testOneMinute(String name) {
        return testPerformance(name, 60);
    }
}
