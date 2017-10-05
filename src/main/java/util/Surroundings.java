package util;

import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.weighting.ShortestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.shapes.GHPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

public class Surroundings {
    private GraphHopperStorage mGhStore;
    private LocationIndex mIndex;
    private FlagEncoder mEncoder;

    private Weighting mWeighting;
    private EdgeExplorer mInEdgeExplorer;
    private EdgeExplorer mOutEdgeExplorer;

    /**
     * The class to find the dots reachable in a certain distance in meters (on the road)
     * @param ghStore The GraphHopperStorage can be got from GraphHopper
     * @param index Also can be got from the GraphHopper
     * @param encoder Can be got from the GraphHopper
     */
    public Surroundings(GraphHopperStorage ghStore, LocationIndex index, FlagEncoder encoder) {
        mGhStore = ghStore;
        mIndex = index;
        mEncoder = encoder;
        mWeighting = new ShortestWeighting(mEncoder);
        mOutEdgeExplorer = ghStore.createEdgeExplorer(new DefaultEdgeFilter(mEncoder, false, true));
        mInEdgeExplorer = ghStore.createEdgeExplorer(new DefaultEdgeFilter(mEncoder, true, false));
    }

    /**
     * Get the surrounding around a center in a certain distance in meters
     * @param latitude
     * @param longitude
     * @param distance
     * @return
     */
    public ArrayList<GHPoint> getSurrounding(double latitude, double longitude, double distance) {
        QueryResult closest = mIndex.findClosest(latitude, longitude, EdgeFilter.ALL_EDGES);
        return DijkstraSSSP(closest.getClosestNode(), distance);
    }

    public int lookupNearest(double latitude, double longitude) {
        return mIndex.findClosest(latitude, longitude, EdgeFilter.ALL_EDGES).getClosestNode();
    }

    private ArrayList<GHPoint> DijkstraSSSP(int start, double distBound) {
        /* TODO: maybe reuse the dijkstra from algorithm */
        HashMap<Integer, NodeWrapper> nodeReference = new HashMap<>();
        PriorityQueue<NodeWrapper> queue = new PriorityQueue<>();
        /* Dijkstra */
        nodeReference.put(start, new NodeWrapper(start, 0, start));
        queue.add(nodeReference.get(start));
        while (!queue.isEmpty()) {
            NodeWrapper current = queue.poll();
            if (current.mNodeID == -1)
                continue;
            EdgeIterator iter = mOutEdgeExplorer.setBaseNode(current.mNodeID);
            while (iter.next()) {
                int nextID = iter.getAdjNode();
                double tempDist = current.mDistance + iter.getDistance();
                if (nodeReference.containsKey(nextID)) {
                    NodeWrapper next = nodeReference.get(nextID);
                    if (next.mDistance > tempDist) {
                        queue.remove(next);
                        next.mDistance = tempDist;
                        next.mParent = current.mNodeID;
                        queue.add(next);
                    }
                } else {
                    if (tempDist < distBound) {
                        NodeWrapper next = new NodeWrapper(nextID, tempDist, current.mNodeID);
                        nodeReference.put(nextID, next);
                        queue.add(next);
                    }
                }
            }
        }
        ArrayList<GHPoint> nodes = new ArrayList<>();
        NodeAccess nodeAccess = mGhStore.getNodeAccess();
        for (int idx : nodeReference.keySet()) {
            nodes.add(new GHPoint(nodeAccess.getLat(idx), nodeAccess.getLon(idx)));
        }
        return nodes;
    }

    private class NodeWrapper implements Comparable {
        public double mDistance;
        public final int mNodeID;
        public int mParent;

        public NodeWrapper(int mID, double distance, int parent) {
            mNodeID = mID;
            mDistance = distance;
            mParent = parent;
        }

        public int compareTo(Object o) {
            NodeWrapper n = (NodeWrapper) o;
            if (mDistance == n.mDistance)
                return 0;
            if (mDistance < n.mDistance)
                return -1;
            return 1;
        }
    }
}
