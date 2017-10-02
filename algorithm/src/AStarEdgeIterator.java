import com.graphhopper.util.EdgeExplorer;

/**
 * The weight is also distance in this iterator,
 * since the heuristic is estimated based on the distance
 */
public class AStarEdgeIterator extends DefaultEdgeIterator {

    public AStarEdgeIterator(int current, int prevEdge, EdgeExplorer outEdgeExplorer) {
        super(current, prevEdge, outEdgeExplorer);
    }

    @Override
    public double getCost() {
        return mGhEdgeIterator.getDistance();
    }

}
