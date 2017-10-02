import java.util.*;

public class Dijkstra extends S2SStrategy {

    public Dijkstra(CallBacks callBacks) {
        super(callBacks);
    }

    @Override
    public Paths compute(int[] set1, int[] set2) throws Exception {
        if (mCallBacks == null) {
            throw new Exception("No edge provider provided");
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
        // create a hash set for easier lookup
        HashSet<Integer> setSet = new HashSet<>();
        Paths resultPaths = new Paths();

        for (int i : set) {
            if (i != start)         // no point of computing the path to itself
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

            EdgeIter iter = mCallBacks.getIterator(current.mNodeID, current.mPreviousEdgeID);
            while (iter.next()) {

                int nextID = iter.getNext();
                double tempCost = iter.getCost() + current.mCost;
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
            // Path reconstruction. (We found a target point)
            if (setSet.contains(current.mNodeID)) {
                ArrayList<Integer> array = new ArrayList<>();
                NodeWrapper loc_current = current;
                do {
                    array.add(loc_current.mNodeID);
                    loc_current = nodeReference.get(loc_current.mParent);
                } while(loc_current.mNodeID != loc_current.mParent);
                array.add(loc_current.mNodeID);
                setSet.remove(current.mNodeID);

                Integer[] points = new Integer[array.size()];
                for (int i = array.size() - 1; i >= 0; i --) {
                    points[array.size() - 1 - i] = array.get(i);
                }
                resultPaths.addPath(start, current.mNodeID, current.mDistance, current.mCost, points);
            }
        }
        return resultPaths;
    }
}
