package util;

import algorithm.DefaultEdgeIterator;
import algorithm.NodeWrapper;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EncodingManager;

import java.util.HashMap;
import java.util.PriorityQueue;

import static java.lang.Double.max;

public class GetTime {
    private int start;
    private int end;
    private EncodingManager mEm;
    private GraphHopper mHopper;
    public GetTime( Integer from, Integer to, EncodingManager mEm_in,GraphHopper mHopper_in)
    {
        start=from;
        end=to;
        mEm=mEm_in;
        mHopper=mHopper_in;
    }
    public double GetTime()
    {
        double result=0;
        Integer cur=start;
        PriorityQueue<NodeWrapper> mQueue = new PriorityQueue<>();
        HashMap<Integer, NodeWrapper> mNodeReference = new HashMap<>();
        mNodeReference.put(cur, new NodeWrapper(cur, 0, cur, -1, 0));
        mQueue.add(mNodeReference.get(cur));

        while (!mQueue.isEmpty()) {
            NodeWrapper current = mQueue.poll();
            if (current.mNodeID == -1) {
                continue;
            }
            if(current.mNodeID == end)
            {
                result=current.mCost;
                break;
            }
            DefaultEdgeIterator nextNodes = new DefaultEdgeIterator(current.mNodeID, current.mPreviousEdgeID, mHopper.getGraphHopperStorage()
                    .createEdgeExplorer(new DefaultEdgeFilter(mEm.getEncoder("car"), false, true)));
            while (nextNodes.next()) {
                int nextID = nextNodes.getNext();
                double tempCost = current.mCost + nextNodes.getCost();
                NodeWrapper next;
                if (mNodeReference.containsKey(nextID)) {
                    next = mNodeReference.get(nextID);
                    /* Decrease Key */
                    if (next.mCost > tempCost) {
                        mQueue.remove(next);
                        next.mCost = tempCost;
                        next.mParent = current.mNodeID;
                        next.mDistance = current.mDistance + nextNodes.getDistance();
                        mQueue.add(next);
                    }
                } else {
                    next = new NodeWrapper(nextID, tempCost, current.mNodeID,
                            nextNodes.getEdge(), current.mDistance + nextNodes.getDistance());
                    mNodeReference.put(nextID, next);
                    mQueue.add(next);
                }
            }
        }
        return result;
    }

}
