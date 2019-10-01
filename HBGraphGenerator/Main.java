import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Main {

    public static boolean isUnusedinSuccsessors(List<Node> nodes, int currentNode) {
        for (int i = currentNode; i < nodes.size(); i++) {
            if (nodes.get(i).getMark().equals("unused")) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        //fig1 graph
        Node A = new Node("A");
        Node B = new Node("B");
        Node C = new Node("C");
        Node D = new Node("D");
        Node E = new Node("E");
        Node F = new Node("F");
        Node G = new Node("G");

        A.setSuccessor(B);
        A.setSuccessor(G);
        B.setSuccessor(C);
        B.setSuccessor(D);
        C.setSuccessor(D);
        C.setSuccessor(E);
        D.setSuccessor(F);
        E.setSuccessor(F);


        List<Node> nodes = new ArrayList<Node>();
        nodes.add(A);
        nodes.add(B);
        nodes.add(C);
        nodes.add(D);
        nodes.add(E);
        nodes.add(F);
        nodes.add(G);

        int k = 0;
        int currentNum = 0;
        boolean flag = true;

        while (flag) {
            nodes.get(currentNum).setI(0);
            nodes.get(currentNum).setJ(k);
            nodes.get(currentNum).setMark("used");

            int i = 1;
            // there exist "unused" successors of the current node
            while (isUnusedinSuccsessors(nodes, currentNum)) {
                if (nodes.get(currentNum).getSuccessor().getMark().equals("unused")) {
                    currentNum = nodes.indexOf(nodes.get(currentNum).getSuccessor());
                } else {
                    for (int j = currentNum; j < nodes.size(); j++) {
                        if (nodes.get(j).getMark().equals("unused")) {
                            currentNum = j;
                            break;
                        }
                    }
                }
                nodes.get(currentNum).setI(i);
                nodes.get(currentNum).setJ(k);
                nodes.get(currentNum).setMark("used");
                i++;
            }

            k++;
            // there exist "unused" nodes judge
            for (Node tmp : nodes) {
                if (tmp.getMark().equals("unused")) {
                    flag = true;
                    currentNum = nodes.indexOf(tmp);
                    break;
                }
                flag = false;
            }
        }
    }
}
