package algorithm;

import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;

public class DefaultEdgeIterator extends EdgeIter {

    protected int mPrevEdge;
    protected EdgeIterator mGhEdgeIterator;
    protected Weighting mWeight;

    /**
     * Construct a new edge iterator
     * @param current the current node id
     * @param prevEdge the edge leads to this node
     * @param outEdgeExplorer base graph edge iterator
     */
    public DefaultEdgeIterator(int current, int prevEdge, EdgeExplorer outEdgeExplorer) {
        mPrevEdge = prevEdge;
        mGhEdgeIterator = outEdgeExplorer.setBaseNode(current);
        EncodingManager em = new EncodingManager("car");
        mWeight = new FastestWeighting(em.getEncoder("car"));
    }

    /**
     * Do not use
     * @return always returns false
     */
    @Deprecated
    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public Boolean next() {
        return mGhEdgeIterator.next();
    }

    @Override
    public double getCost() {
        //return mWeight.calcWeight(mGhEdgeIterator, false, mPrevEdge);
        return mGhEdgeIterator.getDistance();
    }

    @Override
    public double getDistance() {
        return mGhEdgeIterator.getDistance();
    }

    @Override
    public int getNext() {
        return mGhEdgeIterator.getAdjNode();
    }

    @Override
    public int getEdge() {
        return mGhEdgeIterator.getEdge();
    }
}
