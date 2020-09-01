import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.graph.Acyclic;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntPair;

import java.util.*;

public class AllLoopInfo {

    private Map<Integer, LoopInfo> loop = new TreeMap<>();

    public Collection<Integer> getHeads() {
        return loop.keySet();
    }

    public Collection<LoopInfo> getLoops() {
        return loop.values();
    }

    public boolean hasLoop() {
        return !loop.isEmpty();
    }

    public void addLoop(LoopInfo li) {
        if (loop.containsKey(li.getHead())) {
            //already contain this headNode, merge loop
            LoopInfo lOld = loop.get(li.getHead());
            //add back edge
            lOld.addBackEdge(li.getBackEdges().get(0));//without merge every loop only has one back edge
            //add loop node
            for (int i : li.getBodyNodes()) {
                lOld.addBodyNode(i);
            }
        } else {
            loop.put(li.getHead(), li);
        }
    }

    //find Nest loops. simply compare the nodes they contained.
    public void computeNestLoops() {
        for (LoopInfo out : getLoops()) {
            for (LoopInfo in : getLoops()) {
                if (out.getHead() == in.getHead() || out.getBodyNodes().size() <= in.getBodyNodes().size()) {
                    //same loopinfo or out loop size <= inner loop size cannot be nest loop
                    continue;
                }
                if (out.getBodyNodes().containsAll(in.getBodyNodes())) {
                    out.addNestLoop(in);
                }
            }
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("===== All Loop ==========\n");
        for (LoopInfo li : getLoops()) {
            sb.append(li);
        }
        return sb.toString();
    }

    public static AllLoopInfo getAllLoopInfo(SSACFG ssacfg) {
        //compute back edges in the cfg
        IBinaryNaturalRelation result = Acyclic.computeBackEdges(ssacfg, ssacfg.getBasicBlock(1));
        //create AllLoopInfo datastructure
        AllLoopInfo ali = new AllLoopInfo();

        //iterate back edges
        for (IntPair ip : result) {
            int source = ip.getX();
            int dest = ip.getY();

            //new instance of loopInfo
            LoopInfo li = new LoopInfo(dest);

            //add back edge
            li.addBackEdge(ip);
            HashSet<Integer> hsBBloop = new HashSet<Integer>();

            //get Loop Nodes
            getLoopNode(source, dest, ssacfg, hsBBloop);

            //add nodes to LoopInfo
            for (Integer i : hsBBloop) {
                li.addBodyNode(i);
            }

            //compute Loop Out Node:
            for (int i : li.getBodyNodes()) {
                Collection<ISSABasicBlock> c1 = ssacfg.getNormalSuccessors(ssacfg.getBasicBlock(i));
                for (ISSABasicBlock bb : c1) {
                    if (!hsBBloop.contains(ssacfg.getNumber(bb))) { // if bb is outnode, i is candidate for ConditionalBranch
                        for (SSAInstruction inst : ssacfg.getBasicBlock(i).getAllInstructions()) {
                            if (inst.toString().contains("conditional branch"))
                                li.addConditionalBB(i);
                        }
                        li.setOutNode(ssacfg.getNumber(bb));
                    }
                }
            }

            Collection<ISSABasicBlock> c1 = ssacfg.getNormalSuccessors(ssacfg.getBasicBlock(dest));
            for (ISSABasicBlock bb : c1) {
                if (!hsBBloop.contains(ssacfg.getNumber(bb))) {
                    li.setOutNode(ssacfg.getNumber(bb));
                }
            }

            //add loop into AllLoopInfo, will see if need merge or add new loopinfo to it.
            ali.addLoop(li);
        }
        ali.computeNestLoops();
        for (LoopInfo li : ali.getLoops()) {
            System.out.println(li);
        }
        return ali;
    }

    public static void getLoopNode(int backEdgeSource, int backEdgeDest, SSACFG ssacfg, HashSet<Integer> hs) {
        hs.add(backEdgeSource);
        hs.add(backEdgeDest);
        for (ISSABasicBlock bb : ssacfg.getNormalPredecessors(ssacfg.getBasicBlock(backEdgeSource))) {
            int num = ssacfg.getNumber(bb);
            if (!hs.contains(num)) {
                hs.add(num);
                getLoopNode(num, ssacfg, hs);
            }
        }
    }

    public static void getLoopNode(int currentBB, SSACFG ssacfg, HashSet<Integer> hs) {
        for (ISSABasicBlock bb : ssacfg.getNormalPredecessors(ssacfg.getBasicBlock(currentBB))) {
            int num = ssacfg.getNumber(bb);
            if (!hs.contains(num)) {
                hs.add(num);
                getLoopNode(num, ssacfg, hs);
            }
        }
    }

}