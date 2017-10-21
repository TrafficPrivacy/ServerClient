package util;

import com.graphhopper.storage.NodeAccess;

import java.io.Serializable;
import java.util.ArrayList;

public class Reply implements Serializable {
    private final Pair<int[], int[]> mSrcCircle;         // first element is all the elements, second is the border
    private final Pair<int[], int[]> mDstCircle;
    private final Paths mSrcPaths;
    private final Paths mDestPaths;
    private final Paths mInterPaths;

    public Reply(Pair<int[], int[]> srcCircle, Pair<int[], int[]> destCircle, Paths srcPaths, Paths destPaths,
                 Paths interPaths) {
        mSrcCircle = srcCircle;
        mDstCircle = destCircle;
        mSrcPaths = srcPaths;
        mDestPaths = destPaths;
        mInterPaths = interPaths;
    }

    public AdjacencyList<Integer> parse() {
        AdjacencyList<Integer> graph = new AdjacencyList<>();
        int[] srcAll = mSrcCircle.mFirst;
        int[] srcBorder = mSrcCircle.mSecond;
        int[] dstAll = mDstCircle.mFirst;
        int[] dstBorder = mDstCircle.mSecond;
        // add all the nodes
        for (int index : srcAll) {
            graph.insertNode(index);
        }
        for (int index : dstAll) {
            graph.insertNode(index);
        }
        // add edges in the source circle
        recoveryGraph(graph, srcAll, srcBorder, mSrcPaths);

        // add edges in the destination circle
        recoveryGraph(graph, dstBorder, dstAll, mDestPaths);

        // add edges between borders of two circles
        recoveryGraph(graph, srcBorder, dstBorder, mInterPaths);
        return graph;
    }

    private void recoveryGraph(AdjacencyList<Integer> graph, int[] fromSet, int[] toSet, Paths paths) {
        for (int fromIdx : fromSet) {
            for (int toIdx : toSet) {
                if (paths.findWeight(fromIdx, toIdx) > 0) {
                    graph.insertEdge(fromIdx, toIdx)
                            .setWeight(fromIdx, toIdx, paths.findWeight(fromIdx, toIdx))
                            .setDistance(fromIdx, toIdx, paths.findDistance(fromIdx, toIdx));
                }
            }
        }
    }

    public ArrayList<MapPoint> recoveryPath(Integer[] metaPath, NodeAccess nodeAccess) {
        ArrayList<MapPoint> path = new ArrayList<>();
        if (metaPath != null) {
            for (int i = 1; i < metaPath.length; i++) {
            /*TODO: find a better implementation*/
                Integer[] sPath = mSrcPaths.findPath(metaPath[i - 1], metaPath[i]);
                Integer[] iPath = mInterPaths.findPath(metaPath[i - 1], metaPath[i]);
                Integer[] dPath = mDestPaths.findPath(metaPath[i - 1], metaPath[i]);
                if (sPath != null) {
                    for (int idx : sPath) {
                        path.add(new MapPoint(nodeAccess.getLat(idx), nodeAccess.getLon(idx)));
                    }
                } else if (iPath != null) {
                    for (int idx : iPath) {
                        path.add(new MapPoint(nodeAccess.getLat(idx), nodeAccess.getLon(idx)));
                    }
                } else if (dPath != null) {
                    for (int idx : dPath) {
                        path.add(new MapPoint(nodeAccess.getLat(idx), nodeAccess.getLon(idx)));
                    }
                }
            }
        }
        return path;
    }

    public int[] getSrcPoints() {
        return mSrcCircle.mFirst;
    }

    public int[] getDstPoints() {
        return mDstCircle.mFirst;
    }

}
