import io.swagger.models.auth.In;

import java.io.*;
import java.util.*;

public class Main {

    private static int size = 7000;
    private static double probability = 0.5;
    private static double attenuation = 0.95;
    private static int threshold = (int) Math.sqrt(size);
    private static int chainNum;
    private static Map<Integer, Set<Node>> chain = new HashMap<>();
    private static Set<Node> offNodes = new HashSet<>();
    private static Set<Node> chainNodes = new HashSet<>();
    private static Set<Node> graph = new HashSet<>();

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

    public void indexAssignment() {
        System.out.println("-------------Start Procedure1---------------");

        long start = System.currentTimeMillis();

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

        long end = System.currentTimeMillis();
        System.out.println("procedure1 Time :" + ((end - start) / 1000.) + "s");

        chainNum = k;

    }

    public void chainReduction() {
        System.out.println("-------------Start Chain Reduction---------------");

        // Chain Reduction
        long start = System.currentTimeMillis();


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
        for (
                Node node : offNodes) {
            Set<Node> deleteNode = new HashSet<>();
            if (node.getOffChainSuccessors().size() == 0) {
                continue;
            }
            for (Node n : node.getOffChainSuccessors()) {
                Set<Node> temp = new HashSet<>(n.getChainPredecessors());
                temp.retainAll(node.getSuccessors());
                if (!temp.isEmpty()) {
                    deleteNode.add(n);
                }
            }
            node.removeOffChainSuccessors(deleteNode);
        }

        long end = System.currentTimeMillis();

        System.out.println("Chain Reduction Time :" + ((end - start) / 1000.) + "s");
    }


    public static void main(String[] args) {
        Test test = new Test();
        Main main = new Main();
        EdgeCreator.create(size, probability, attenuation);
        String fileName = "res/test.csv";
        try {
            test.loadGraph(fileName, graph);
        } catch (IOException e) {
            e.printStackTrace();
        }


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

        main.indexAssignment();
        main.chainReduction();


        System.out.println("-------------Test---------------");


        test.performanceTest(graph);
        test.reachableTest(graph);

//        dispAll(graph);
    }
}
