import java.util.Iterator;

/**
 * The set to set shortest path strategy abstract class
 */
public abstract class S2SStrategy {

    protected EdgeProvider mEdgeProvider;

    public static final int DIJKSTRA = 0;
    public static final int FLOYWARSHALL = 1;
    public static final int BELLMANFORD = 2;

    public static S2SStrategy strategyFactory(int type, EdgeProvider edgeProvider) throws Exception{
        switch (type) {
            case DIJKSTRA:
                return new Dijkstra(edgeProvider);

            case FLOYWARSHALL:
                return new FloydWarshall(edgeProvider);

            case BELLMANFORD:
                return new BellmanFord(edgeProvider);

            default:
                throw new Exception("Invalid option for strategy");
        }
    }


    public S2SStrategy(EdgeProvider edgeProvider) {
        mEdgeProvider = edgeProvider;
    }

    public S2SStrategy setEdgeProvider(EdgeProvider edgeProvider) {
        mEdgeProvider = edgeProvider;
        return this;
    }

    public abstract Paths compute(int[] set1, int[] set2) throws Exception;

    public interface EdgeProvider {
        /**
         * Get a new edge iterator
         * @param current the integer represent the current node
         * @return an iterator will return each edge represented by the end node and the
         * weight
         */
        EdgeIter getIterator(int current, int prevEdgeID);
    }

    static public abstract class EdgeIter implements Iterator<Boolean> {

        /**
         * Standard has next function for iterator
         * @return true if there is more than zero available edge. false otherwise.
         */
        @Override
        public abstract boolean hasNext();

        /**
         * Returns true if hasNext() returns true.
         * If returns true, then also iterate one step
         * @return true if hasNext returns true and proceed one step. False otherwise
         */
        @Override
        public abstract Boolean next();

        /**
         * Get the cost of the current iterated edge. Note the cost could be
         * time, or distance or both.
         * @return the COST of the edge
         */
        public abstract double getCost();

        /**
         * Get the geological distance of the edge.
         * @return the GEOLOGICAL distance of the edge
         */
        public abstract double getDistance();

        /**
         * Get the id of the next node
         * @return id of the next node
         */
        public abstract int getNext();

        /**
         * Will be helpful to compute the weight
         * @return the current edge id
         */
        public abstract int getEdge();

    }

}
