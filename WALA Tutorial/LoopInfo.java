import com.ibm.wala.util.intset.IntPair;

import java.util.ArrayList;
import java.util.HashSet;

public class LoopInfo {

    private int loopHead;

    private int loopOutNode;

    private HashSet<Integer> loopBodyNodes = new HashSet<Integer>();

    private ArrayList<IntPair> backEdges = new ArrayList<IntPair>();

    private ArrayList<LoopInfo> nestLoops = new ArrayList<LoopInfo>();

    public LoopInfo(int head) {
        this.loopHead = head;
    }

    public void setOutNode(int outNode) {
        this.loopOutNode = outNode;
    }

    public int getOutNode() {
        return this.loopOutNode;
    }

    public int getHead() {
        return this.loopHead;
    }

    public ArrayList<LoopInfo> getNestLoops() {
        return this.nestLoops;
    }

    public void addNestLoop(LoopInfo li) {
        nestLoops.add(li);
    }

    public HashSet<Integer> getBodyNodes() {
        return this.loopBodyNodes;
    }

    public ArrayList<IntPair> getBackEdges() {
        return this.backEdges;
    }

    public void addBodyNode(int node) {
        this.loopBodyNodes.add(node);
    }

    public void addBackEdge(IntPair ip) {
        this.backEdges.add(ip);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("  == Loop == \n");
        sb.append("  Loop Head: " + this.loopHead + "\n");
        sb.append("  Loop Out Node: " + this.loopOutNode + "\n");
        sb.append("  Loop Body Nodes: ");
        for (Integer i : loopBodyNodes) {
            sb.append(i + " ");
        }
        sb.append("\n");
        sb.append("  BackEdge(s): ");
        for (int i = 0; i < backEdges.size(); i++) {
            IntPair ip = backEdges.get(i);
            sb.append(ip.getX() + " -> " + ip.getY() + " ");
        }
        sb.append("\n");
        if (nestLoops.size() == 0) {
            sb.append("  Nest Loops: No nest loops\n");
        } else {
            sb.append("  Nest Loops: ***\n");
            for (int i = 0; i < nestLoops.size(); i++) {
                sb.append(nestLoops.get(i));
            }
            sb.append("  End Nest Loops: ***\n");
        }
        sb.append("  End Loop === \n");
        return sb.toString();
    }

}