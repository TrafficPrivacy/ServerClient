import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;

import java.util.*;

public class Dijkstra extends S2SStrategy{

    public Dijkstra(EdgeExplorer edgeExplorer, Weighting weighting) {
        super(edgeExplorer, weighting);
    }

    @Override
    public Paths compute(int[] set1, int[] set2) {
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
        // create a hash set for easier lookup
        HashSet<Integer> setSet = new HashSet<>();
        Paths resultPaths = new Paths();

        for (int i : set) {
            setSet.add(i);
        }

        // dijkstra

        HashMap<Integer, NodeWrapper> nodeReference = new HashMap<>();
        PriorityQueue<NodeWrapper> queue = new PriorityQueue<>();
        nodeReference.put(start, new NodeWrapper(start, 0, start, -1, 0));
        queue.add(nodeReference.get(start));
        while (!queue.isEmpty() && setSet.size() > 0) {
            NodeWrapper current = queue.poll();
            if (current.mNodeID == -1)
                continue;
            EdgeIterator iter = mOutEdgeExplorer.setBaseNode(current.mNodeID);
            while (iter.next()) {
                int nextID = iter.getAdjNode();
                double tempCost = current.mCost + mWeighting.calcWeight(iter, false, current.mPreviousEdgeID);
                if (nodeReference.containsKey(nextID)) {
                    NodeWrapper next = nodeReference.get(nextID);
                    // decrease key operation in the priority queue
                    if (next.mCost > tempCost) {
                        queue.remove(next);
                        next.mCost = tempCost;
                        next.mParent = current.mNodeID;
                        next.mDistance = current.mDistance + iter.getDistance();
                        queue.add(next);
                    }
                } else {
                    NodeWrapper next = new NodeWrapper(nextID, tempCost, current.mNodeID,
                                                iter.getEdge(), current.mDistance + iter.getDistance());
                    nodeReference.put(nextID, next);
                    queue.add(next);
                }
            }
            // Path reconstruction
            if (setSet.contains(current.mNodeID)) {
                Deque<Integer> stack = new ArrayDeque<>();
                ArrayList<Integer> array = new ArrayList<>();
                stack.add(current.mNodeID);
                NodeWrapper loc_current = current;
                while (loc_current.mNodeID != loc_current.mParent) {
                    stack.add(loc_current.mParent);
                    loc_current = nodeReference.get(loc_current.mParent);
                }
                setSet.remove(current.mNodeID);

                // reverse the stack
                for (Integer i : stack) {
                    array.add(i);
                }
                Integer[] points = new Integer[array.size()];
                array.toArray(points);
                resultPaths.addPath(start, current.mNodeID, current.mDistance, points);
            }
        }

        return resultPaths;
    }

    private class NodeWrapper implements Comparable {
        public double mDistance;
        public double mCost;
        public final int mNodeID;
        public int mParent;
        public int mPreviousEdgeID;

        public NodeWrapper(int mID, double cost, int parent, int previousEdgeID, double distance) {
            mNodeID = mID;
            mDistance = distance;
            mParent = parent;
            mPreviousEdgeID = previousEdgeID;
            mCost = cost;
        }

        public int compareTo(Object o) {
            NodeWrapper n = (NodeWrapper) o;
            if (mCost == n.mCost)
                return 0;
            if (mCost < n.mCost)
                return -1;
            return 1;
        }
    }
}
