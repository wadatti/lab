import java.io.*;

public class EdgeCreator {
    public static boolean singleLoop = false;
    public static boolean loop = true;

    public static void create(int nodes, double probability, boolean loop, boolean singleLoop) {
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
                    if (Math.random() < probability)
                        pw.println(src + "," + dst);
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void create(int nodes, double probability, boolean loop) {
        create(nodes, probability, loop, false);
    }

    public static void create(int nodes, double probability) {
        create(nodes, probability, false, false);
    }

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
}