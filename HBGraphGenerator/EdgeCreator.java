import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EdgeCreator {
    public static void create(int nodes, double probability, double attenuation, boolean loop, boolean singleLoop) {
        File file = new File("res/test.csv");
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter pw = new PrintWriter(bw);) {
            for (int src = 0; src < nodes; src++)
                for (int dst = 0; dst < nodes; dst++) {
                    if (!loop && src > dst)
                        continue;
                    if (!singleLoop && src == dst)
                        continue;
                    if (Math.random() < probability * Math.pow(attenuation, Math.abs(src - dst)))
                        pw.println(src + "," + dst);
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void create(int nodes, double probability, double attenuation, boolean loop) {
        create(nodes, probability, attenuation, loop, false);
    }

    public static void create(int nodes, double probability, double attenuation) {
        create(nodes, probability, attenuation, false, false);
    }

    public static void create(int nodes, double probability, boolean loop, boolean singleLoop) {
        create(nodes, probability, 1, loop, singleLoop);
    }

    public static void create(int nodes, double probability, boolean loop) {
        create(nodes, probability, loop, false);
    }

    public static void create(int nodes, double probability) {
        create(nodes, probability, false, false);
    }

    public static void createNDD(int nodes, double degree, int maxDistance, boolean loop) {
        if (!loop)
            degree += degree * maxDistance / (nodes * 2 - maxDistance);
        List<Integer> distances = new ArrayList<>();
        distances.addAll(IntStream.range(1, maxDistance).boxed().collect(Collectors.toList()));
        File file = new File("res/test.csv");
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter pw = new PrintWriter(bw);) {
            for (int source = 0; source < nodes; source++) {
                Collections.shuffle(distances);
                for (int distance : distances.subList(0, (int)(Math.random() * (degree * 2 - 1)) + 1)) {
                    int target = source + distance;
                    if (loop | (0 <= target && target < nodes))
                        pw.println(source + "," + target % nodes);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
