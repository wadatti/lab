import java.util.*;

public class Node {
    private String event;
    private int index;
    private int chain;
    private Set<Node> immediateSuccessors = new HashSet<>();
    private Set<Node> successors = new HashSet<>();
    private Set<Node> chainPredecessors = new HashSet<>();
    private Set<Node> offChainSuccessors = new HashSet<>();
    private Map<Integer, Integer> successorsIndices = new HashMap<>();
    private Map<Integer, Integer> chainPredecessorsIndices = new HashMap<>();


    private boolean used = false;
    private boolean offChain = false;

    public Node(String event) {
        this.event = event;
    }

    public Node(String event, int index, int chain) {
        this.event = event;
        this.index = index;
        this.chain = chain;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setChain(int chain) {
        this.chain = chain;
    }

    public void setImmediateSuccessor(Node successor) {
        immediateSuccessors.add(successor);
    }

    public void setSuccessors(Set<Node> successors) {
        this.successors = successors;
        for (Node node : successors) {
            this.successorsIndices.put(node.chain, node.index);
        }
    }

    public void setSuccessors(Collection<Node> successors) {
        this.successors.addAll(successors);
        for (Node node : successors) {
            this.successorsIndices.put(node.chain, node.index);
        }
    }

    public void removeSuccessors(Collection<Node> successors) {
        this.successors.removeAll(successors);
        for (Node node : successors) {
            this.successorsIndices.remove(node.chain, node.index);
        }
    }

    public void clearSuccessors() {
        this.successors.clear();
        this.successorsIndices.clear();
    }

    public void setChainPredecessors(Collection<Node> chainPredecessors) {
        this.chainPredecessors.addAll(chainPredecessors);
        for (Node node : chainPredecessors) {
            chainPredecessorsIndices.put(node.chain, node.index);
        }
    }

    public void setChainPredecessors(Node chainPredecessors) {
        this.chainPredecessors.add(chainPredecessors);
        this.chainPredecessorsIndices.put(chainPredecessors.chain, chainPredecessors.index);
    }

    public void clearChainPredecessors() {
        this.chainPredecessors.clear();
        this.chainPredecessorsIndices.clear();
    }

    public void setOffChainSuccessors(Collection<Node> offChainSuccessors) {
        this.offChainSuccessors.addAll(offChainSuccessors);
        for (Node node : offChainSuccessors) {
        }
    }

    public void retainOffChainSuccessors(Collection<Node> offChainSuccessors) {
        this.offChainSuccessors.retainAll(offChainSuccessors);
    }

    public void removeOffChainSuccessors(Collection<Node> offChainSuccessors) {
        this.offChainSuccessors.removeAll(offChainSuccessors);
    }

    public void clearOffChainSuccessors() {
        this.offChainSuccessors.clear();
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public void setOffChain(boolean offChain) {
        this.offChain = offChain;
    }

    public String getEvent() {
        return event;
    }

    public int getIndex() {
        return index;
    }

    public int getChain() {
        return chain;
    }

    public Set<Node> getImmediateSuccessors() {
        return immediateSuccessors;
    }

    public Set<Node> getSuccessors() {
        return successors;
    }

    public Set<Node> getChainPredecessors() {
        return chainPredecessors;
    }

    public Set<Node> getOffChainSuccessors() {
        return offChainSuccessors;
    }


    public boolean isUsed() {
        return used;
    }

    public boolean isOffChain() {
        return offChain;
    }

    private void getSuccessors(Set<Node> successors) {
        if (successors.contains(this))
            return;
        successors.add(this);
        for (Node node : immediateSuccessors)
            node.getSuccessors(successors);
    }

    public Set<Node> getAllSuccessors() {
        Set<Node> successors = new HashSet<>();
        for (Node node : immediateSuccessors)
            node.getSuccessors(successors);
        return successors;
    }

    public Node unusedSuccessor() {
        Set<Node> unexploredNodes = new HashSet<>(this.getImmediateSuccessors());
        Set<Node> temp = new HashSet<>();
        while (true) {
            int currentSize = unexploredNodes.size();
            for (Node n : unexploredNodes) {
                temp.addAll(n.getImmediateSuccessors());
                if (!n.isUsed()) return n;
            }
            unexploredNodes.addAll(temp);
            if (unexploredNodes.size() == currentSize) break;
        }
        for (Node n : unexploredNodes) {
            if (!n.isUsed()) return n;
        }
        return null;
    }

    public boolean isUnusedSuccessors() {
        Set<Node> unexploredNodes = new HashSet<>(this.getImmediateSuccessors());
        Set<Node> temp = new HashSet<>();
        while (true) {
            int currentSize = unexploredNodes.size();
            for (Node n : unexploredNodes) {
                temp.addAll(n.getImmediateSuccessors());
                if (!n.isUsed()) return true;
            }
            unexploredNodes.addAll(temp);
            if (unexploredNodes.size() == currentSize) break;
        }
        for (Node n : unexploredNodes) {
            if (!n.isUsed()) return true;
        }
        return false;
    }

    public Node unusedImmediateSuccessor() {
        HashSet<Node> successorsNodes = new HashSet<>(this.getImmediateSuccessors());
        for (Node node : successorsNodes) {
            if (!node.isUsed()) return node;
        }
        return null;
    }

    public boolean isReachable(Node destination) {
        // (1)
        if (!destination.offChain) {
            if (this.successors.contains(destination)) {
                return true;
            } else return this.successorsIndices.containsKey(destination.chain) && this.successorsIndices.get(destination.chain) < destination.index;
        }

        // (2)
        else if (!this.offChain) {
            if (destination.chainPredecessors.contains(this)) {
                return true;
            } else return destination.chainPredecessorsIndices.containsKey(this.chain) && this.index < destination.chainPredecessorsIndices.get(this.chain);
        }

        // (3)
        else {
            if (this.offChainSuccessors.contains(destination)) {
                return true;
            }
            for (Map.Entry<Integer, Integer> index : this.successorsIndices.entrySet()) {
                if (destination.chainPredecessorsIndices.containsKey(index.getKey()) && destination.chainPredecessorsIndices.get(index.getKey()) >= index.getValue())
                    return true;
            }
        }

        return false;
    }


    @Override
    public String toString() {
        return event;
    }
}
