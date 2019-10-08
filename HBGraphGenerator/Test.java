import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Test {
    public static void loadGraph(File file, Set<Node> graph) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        Map<String, Node> nodes = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] strs = line.split(",", 0);
            Node src, dst;
            if ((src = nodes.get(strs[0])) == null) {
                src = new Node(strs[0]);
                nodes.put(strs[0], src);
                graph.add(src);
            }
            if ((dst = nodes.get(strs[1])) == null) {
                dst = new Node(strs[1]);
                nodes.put(strs[1], dst);
                graph.add(dst);
            }
            src.setImmediateSuccessor(dst);
        }
    }

    public void loadGraph(String fileName, Set<Node> graph) throws IOException {
        loadGraph(new File(fileName), graph);
    }

    public void performanceTest(Set<Node> graph) {
        System.out.println("Performance Test");
        long start = System.currentTimeMillis();
        int i = 0, n = 100000000;
        while (i < n)
            for (Node src : graph) {
                for (Node dst : graph) {
                    src.isReachable(dst);
                    if (++i >= n) break;
                }
                if (i >= n) break;
            }
        long end = System.currentTimeMillis();
        System.out.println((end - start) / 1000. + "s");
    }

    public void reachableTest(Set<Node> graph){
        for (Node node : graph) {
            Set<Node> unreachableNodes = new HashSet<>(graph);
            unreachableNodes.removeAll(node.getAllSuccessors());
            for (Node destination : node.getAllSuccessors()) {
                if (!node.isReachable(destination)) {
                    System.out.println("Error1:" + node.getEvent() + "→" + destination.getEvent());
                }
            }
            for (Node destination : unreachableNodes) {
                if (node.isReachable(destination)) {
                    System.out.println("Error2:" + node.getEvent() + "→" + destination.getEvent());
                }
            }
        }
    }

    public void dispAll(Set<Node> graph) {
        System.out.println("---------------------------");
        for (Node node : graph) {
            System.out.println(node.getEvent() + ":(" + node.getIndex() + "," + node.getChain() + ")");
        }
        for (Node node : graph) {
            System.out.println(node.getEvent() + node.getSuccessors() + node.getChainPredecessors() + node.getOffChainSuccessors());
        }
    }
}
