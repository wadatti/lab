import java.io.*;
import java.util.*;

public class GraphCreator {

    private static int threshold;
    private static int chainNum;
    private static Map<Integer, Set<Node>> chain = new HashMap<>();
    private static Set<Node> offNodes = new HashSet<>();
    private static Set<Node> chainNodes = new HashSet<>();

    public Set<Node> CreateGraph(Set<Node> graph) {
        threshold = (int) Math.sqrt(graph.size());
        graph = indexAssignment(graph);
        graph = chainReduction(graph);
        return graph;
    }

    public static boolean isExistUnusedNode(Set<Node> nodes) {
        for (Node node : nodes) {
            if (!node.isUsed()) return true;
        }
        return false;
    }

    public static Node unusedNode(Set<Node> nodes) {
        for (Node node : nodes) {
            if (!node.isUsed()) return node;
        }
        return null;
    }

    public boolean isReachable(Node source, Node target) {
        return source.isReachable(target);
    }

    public Set<Node> indexAssignment(Set<Node> graph) {
        // procedure1 (Chain Reduction (1))
        int k = 0;
        Node currentNode;

        while (isExistUnusedNode(graph)) {
            currentNode = unusedNode(graph);
            currentNode.setIndex(0);
            currentNode.setChain(k);
            currentNode.setUsed(true);
            int i = 1;

            // there exist "unused" successors of the current node
            while (currentNode.isUnusedSuccessors()) {
                Node unusedSuccessorNode = currentNode.unusedImmediateSuccessor();
                if (unusedSuccessorNode != null) {
                    currentNode = unusedSuccessorNode;
                } else {
                    currentNode = currentNode.unusedSuccessor();
                }
                currentNode.setIndex(i);
                currentNode.setChain(k);
                currentNode.setUsed(true);
                i++;
            }
            k++;
        }

        // procedure1 End


        chainNum = k;

        return graph;

    }

    public Set<Node> chainReduction(Set<Node> graph) {
        // setSuccessors & chain sorting
        for (Node node : graph) {
            node.setSuccessors(node.getAllSuccessors());
            if (!chain.containsKey(node.getChain())) {
                chain.put(node.getChain(), new HashSet<>());
            }
            chain.get(node.getChain()).add(node);
        }


        // change offChain & Sorting ChainNode or OffChainNode
        for (Map.Entry<Integer, Set<Node>> entry : chain.entrySet()) {
            if (entry.getValue().size() <= threshold) {
                for (Node node : entry.getValue()) {
                    node.setOffChain(true);
                    offNodes.add(node);
                }
            } else {
                chainNodes.addAll(entry.getValue());
            }
        }


        for (Node node : graph) {
            node.setOffChainSuccessors(offNodes);
            node.retainOffChainSuccessors(node.getSuccessors());
            node.removeSuccessors(node.getOffChainSuccessors());
        }


        // (3)
        for (Node node : chainNodes) {
            for (Node n : node.getOffChainSuccessors()) {
                if (node.getOffChainSuccessors().contains(n)) {
                    n.setChainPredecessors(node);
                }
            }
            node.clearOffChainSuccessors();
        }

        // chainGroup initialize
        Map<Integer, Set<Node>> chainGroup = new HashMap<>();
        for (int j = 0; j < chainNum; j++) {
            chainGroup.put(j, new HashSet<>());
        }


        for (Node node : chainNodes) {
            for (int j = 0; j < chainNum; j++) {
                chainGroup.get(j).clear();
            }
            for (Node n : node.getSuccessors()) {
                chainGroup.get(n.getChain()).add(n);
            }

            Set<Node> temp_nodes = new HashSet<>();
            for (Map.Entry<Integer, Set<Node>> entry : chainGroup.entrySet()) {
                Node temp_node = null;
                int maxI = Integer.MAX_VALUE;
                Set<Node> temp = entry.getValue();
                for (Node n : temp) {
                    if (n.getIndex() < maxI) {
                        temp_node = n;
                        maxI = n.getIndex();
                    }
                }
                if (temp_node != null) {
                    temp_nodes.add(temp_node);
                }
            }
            node.clearSuccessors();
            node.setSuccessors(temp_nodes);
        }


        //(4)
        for (Node node : offNodes) {
            for (int j = 0; j < chainNum; j++) {
                chainGroup.get(j).clear();
            }
            for (Node n : node.getChainPredecessors()) {
                chainGroup.get(n.getChain()).add(n);
            }
            Set<Node> temp_nodes = new HashSet<>();

            for (Map.Entry<Integer, Set<Node>> entry : chainGroup.entrySet()) {
                Node temp_node = null;
                int minI = Integer.MIN_VALUE;
                Set<Node> temp = entry.getValue();
                for (Node n : temp) {
                    if (n.getIndex() > minI) {
                        temp_node = n;
                        minI = n.getIndex();
                    }
                }
                if (temp_node != null) {
                    temp_nodes.add(temp_node);
                }
            }
            node.clearChainPredecessors();
            node.setChainPredecessors(temp_nodes);
        }


        //(5)
        for (Node node : offNodes) {
            for (int j = 0; j < chainNum; j++) {
                chainGroup.get(j).clear();
            }
            for (Node n : node.getSuccessors()) {
                chainGroup.get(n.getChain()).add(n);
            }

            Set<Node> temp_nodes = new HashSet<>();
            for (Map.Entry<Integer, Set<Node>> entry : chainGroup.entrySet()) {
                Node temp_node = null;
                int maxI = Integer.MAX_VALUE;
                Set<Node> temp = entry.getValue();
                for (Node n : temp) {
                    if (n.getIndex() < maxI) {
                        temp_node = n;
                        maxI = n.getIndex();
                    }
                }
                if (temp_node != null) {
                    temp_nodes.add(temp_node);
                }
            }
            node.clearSuccessors();
            node.setSuccessors(temp_nodes);
        }


        //(6)
        for (Node node : offNodes) {
            if (node.getOffChainSuccessors().size() == 0) {
                continue;
            }
            Set<Node> deleteNode = new HashSet<>();
            for (Node n : node.getOffChainSuccessors()) {
                Set<Node> temp = new HashSet<>(n.getChainPredecessors());
                temp.retainAll(node.getSuccessors());
                if (!temp.isEmpty()) {
                    deleteNode.add(n);
                }
            }
            node.removeOffChainSuccessors(deleteNode);
        }

        return graph;
    }

    public static void main(String[] args) {
        Set<Node> graph = new HashSet<>();
        GraphLoader graphLoader = new GraphLoader();
        GraphCreator creator = new GraphCreator();
        String fileName = "res/test.csv";
        int nodes = 5000;
        double degree = 5;
        int maxDistance = 100;
        boolean loop = false;
        int testSeconds = 10;

        Timer.start("create new graph");
        EdgeCreator.createNDD(nodes, degree, maxDistance, loop);
        Timer.stop("create new graph");
        try {
            graph = graphLoader.loadGraph2(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(graph.size());


        //fig1 graph
//        Node A = new Node("A");
//        Node B = new Node("B");
//        Node C = new Node("C");
//        Node D = new Node("D");
//        Node E = new Node("E");
//        Node F = new Node("F");
//        Node G = new Node("G");
//        Node H = new Node("H");
//
//        A.setImmediateSuccessor(B);
//        A.setImmediateSuccessor(G);
//        A.setImmediateSuccessor(F);
//        B.setImmediateSuccessor(C);
//        B.setImmediateSuccessor(D);
//        C.setImmediateSuccessor(D);
//        C.setImmediateSuccessor(E);
//        C.setImmediateSuccessor(F);
//        D.setImmediateSuccessor(F);
//        E.setImmediateSuccessor(F);
//        G.setImmediateSuccessor(B);
//        H.setImmediateSuccessor(C);
//
//
//        List<Node> graph = new ArrayList<Node>();
//        graph.add(A);
//        graph.add(B);
//        graph.add(C);
//        graph.add(D);
//        graph.add(E);
//        graph.add(F);
//        graph.add(G);
//        graph.add(H);

        Timer.start("index Assignment");
        graph = creator.indexAssignment(graph);
        Timer.stop("index Assignment");

        Timer.start("chain Reduction");
        graph = creator.chainReduction(graph);
        Timer.stop("chain Reduction");

        ReachabilityTester<Node> tester = new ReachabilityTester<>(graph);

        tester.put("my Graph", creator::isReachable);

        System.out.println(testSeconds + " seconds test");
        System.out.println("my Graph: " + tester.testPerformance("my Graph", testSeconds) + " times");

        Timer.start("test reachability");
        for (Node node : graph) {
            Set<Node> unreachableNodes = new HashSet<>(graph);
            unreachableNodes.removeAll(node.getAllSuccessors());
            for (Node destination : node.getAllSuccessors()) {
                if (!node.isReachable(destination)) {
                    System.err.println("Error1:" + node.getEvent() + "→" + destination.getEvent());
                }
            }
            for (Node destination : unreachableNodes) {
                if (node.isReachable(destination)) {
                    System.err.println("Error2:" + node.getEvent() + "→" + destination.getEvent());
                }
            }
        }
        Timer.stop("test reachability");


    }
}