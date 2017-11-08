package algorithm;

import util.NoEdgeIteratorException;
import util.Paths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

public class Dijkstra extends S2SStrategy {
    protected PriorityQueue<NodeWrapper> mQueue;
    protected HashMap<Integer, NodeWrapper> mNodeReference;
    protected HashSet<Integer> mTargets;


    public Dijkstra(CallBacks callBacks) {
        super(callBacks);
        mQueue = new PriorityQueue<>();
        mNodeReference = new HashMap<>();
        mTargets = new HashSet<>();
    }

    @Override
    public Paths compute(int[] set1, int[] set2) throws NoEdgeIteratorException {
        if (mCallBacks == null) {
            throw new NoEdgeIteratorException();
        }
        Paths paths = new Paths();
        for (int start : set1) {
            Paths newPaths = dijkstra(start, set2);
            paths.addAll(newPaths);
        }
        return paths;
    }

    /**
     * Implementation of one to all dijkstra
     * @param start The graphhopper index for start node
     * @param set The set of desitination ndoes
     * @return a map of each pair of nodes to the distance and path (represented by an array of index)
     */
    private Paths dijkstra(int start, int[] set) {
        mTargets.clear();
        mQueue.clear();
        mNodeReference.clear();

        /* create a hash set to track unsettled targets */
        Paths resultPaths = new Paths();

        for (int i : set) {
            if (i != start)         // no point of computing the path to itself
                mTargets.add(i);
        }

        /* Dijkstra iterations */
        mNodeReference.put(start, new NodeWrapper(start, 0, start, -1, 0));
        mQueue.add(mNodeReference.get(start));
        while (!mQueue.isEmpty() && mTargets.size() > 0) {
            NodeWrapper current = mQueue.poll();
            if (current.mNodeID == -1)
                continue;
            current.mCost -= current.mPotential;
            EdgeIterator nextNodes = mCallBacks.getIterator(current.mNodeID, current.mPreviousEdgeID);
            while (nextNodes.next()) {
                int nextID = nextNodes.getNext();
                double nextPotential = mCallBacks.getPotential(nextID, mTargets);
                double tempCost = current.mCost + nextNodes.getCost() + nextPotential;
                NodeWrapper next;
                if (mNodeReference.containsKey(nextID)) {
                    next = mNodeReference.get(nextID);
                    /* Decrease Key */
                    if (next.mCost > tempCost) {
                        mQueue.remove(next);
                        next.mCost = tempCost;
                        next.mParent = current.mNodeID;
                        next.mDistance = current.mDistance + nextNodes.getDistance();
                        next.mPotential = nextPotential;
                        mQueue.add(next);
                    }
                } else {
                    next = new NodeWrapper(nextID, tempCost, current.mNodeID,
                            nextNodes.getEdge(), current.mDistance + nextNodes.getDistance());
                    mNodeReference.put(nextID, next);
                    next.mPotential = nextPotential;
                    mQueue.add(next);
                }
            }
            /* Path Reconstruction */
            if (mTargets.contains(current.mNodeID)) {
                Integer[] points = reconstructPath(current);
                resultPaths.addPath(start, current.mNodeID, current.mDistance, current.mCost, points);
            }
        }
        return resultPaths;
    }

    protected Integer[] reconstructPath(NodeWrapper current) {
        ArrayList<Integer> array = new ArrayList<>();
        NodeWrapper loc_current = current;
        do {
            array.add(loc_current.mNodeID);
            loc_current = mNodeReference.get(loc_current.mParent);
        } while(loc_current.mNodeID != loc_current.mParent);
        array.add(loc_current.mNodeID);
        mTargets.remove(current.mNodeID);

        Integer[] points = new Integer[array.size()];
        for (int i = array.size() - 1; i >= 0; i --) {
            points[array.size() - 1 - i] = array.get(i);
        }
        return points;
    }
}
