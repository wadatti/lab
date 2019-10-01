import java.util.ArrayList;
import java.util.List;

public class Node {
    private String event;
    private int i;
    private int j;
    private List<Node> successors = new ArrayList<Node>();
    private Node predecessor = null;
    private Node offChainSuccessor = null;
    private String mark = "unused";

    public Node(String event) {
        this.event = event;
    }

    public Node(String event, int i, int j) {
        this.event = event;
        this.i = i;
        this.j = j;
    }

    public void setI(int i) {
        this.i = i;
    }

    public void setJ(int j) {
        this.j = j;
    }

    public void setSuccessor(Node successor) {
        successors.add(successor);
    }

    public void setPredecessor(Node predecessor) {
        this.predecessor = predecessor;
    }

    public void setOffChainSuccessor(Node offChainSuccessor) {
        this.offChainSuccessor = offChainSuccessor;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public String getEvent() {
        return event;
    }

    public int getI() {
        return i;
    }

    public int getJ() {
        return j;
    }

    public Node getPredecessor() {
        return predecessor;
    }

    public List<Node> getSuccessors() {
        return successors;
    }

    public Node getOffChainSuccessor() {
        return offChainSuccessor;
    }

    public String getMark() {
        return mark;
    }
}
