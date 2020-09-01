import com.ibm.wala.util.intset.IntPair;

import java.util.ArrayList;
import java.util.HashSet;

public class LoopInfo {

    private int loopHead;

    private HashSet<Integer> loopOutNode = new HashSet<Integer>();

    private HashSet<Integer> loopBodyNodes = new HashSet<Integer>();

    private ArrayList<IntPair> backEdges = new ArrayList<IntPair>();

    private ArrayList<LoopInfo> nestLoops = new ArrayList<LoopInfo>();

    private HashSet<Integer> conditionalBB = new HashSet<>();

    public LoopInfo(int head) {
        this.loopHead = head;
    }

    public void setOutNode(int outNode) {
        loopOutNode.add(outNode);
    }

    public HashSet<Integer> getOutNode() {
        return this.loopOutNode;
    }

    public int getHead() {
        return this.loopHead;
    }

    public HashSet<Integer> getConditionalBB() {
        return this.conditionalBB;
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

    public void addConditionalBB(int bb) {
        this.conditionalBB.add(bb);
    }

    public boolean containsBB(int bb) {
        for (int condBB : conditionalBB) {
            if (bb == condBB) return false;
        }
        for (int i : loopBodyNodes) {
            if (i == bb) return true;
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  == Loop == \n");
        sb.append("  Loop Head: ").append(this.loopHead).append("\n");
        sb.append("  Loop Out Node: ");
        for (Integer i : loopOutNode) {
            sb.append(i).append(" ");
        }
        sb.append("\n");
        sb.append("  Loop Body Nodes: ");
        for (Integer i : loopBodyNodes) {
            sb.append(i).append(" ");
        }
        sb.append("\n");
        sb.append("  BackEdge(s): ");
        for (IntPair ip : backEdges) {
            sb.append(ip.getX()).append(" -> ").append(ip.getY()).append(" ");
        }
        sb.append("\n");
        sb.append("  conditional branch BB(s): ");
        for (int i : conditionalBB) {
            sb.append(i).append(" ");
        }
        sb.append("\n");
        if (nestLoops.size() == 0) {
            sb.append("  Nest Loops: No nest loops\n");
        } else {
            sb.append("  *** Nest Loops: ***\n");
            for (int i = 0; i < nestLoops.size(); i++) {
                sb.append(nestLoops.get(i));
            }
            sb.append("  *** End Nest Loops: ***\n");
        }
        sb.append("  End Loop === \n");
        return sb.toString();
    }

}