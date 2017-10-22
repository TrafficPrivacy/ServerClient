package algorithm;

import java.util.HashSet;

public interface CallBacks {
    /**
     * Get a new edge iterator
     * @param current the integer represent the current node
     * @return an iterator will return each edge represented by the end node and the
     * weight
     */
    EdgeIterator getIterator(int current, int prevEdgeID);

    /**
     * Get the potential at the current node
     * @param current current node
     * @param targets the targets
     * @return the potential
     */
    double getPotential(int current, HashSet<Integer> targets);
}
