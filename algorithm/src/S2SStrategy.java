import com.graphhopper.routing.weightinghting.Weighting;
import com.graphhopper.util.EdgeExplorer;

/**
 * The set to set shortest path strategy abstract class
 */
public abstract class S2SStrategy {

    protected EdgeExplorer mOutEdgeExplorer;
    protected Weighting mWeighting;

    public static final int DIJKSTRA = 0;
    public static final int FLOYWARSHALL = 1;
    public static final int BELLMANFORD = 2;

    public static S2SStrategy strategyFactory(int type, EdgeExplorer edgeExplorer, Weighting weighting) throws Exception{
        switch (type) {
            case DIJKSTRA:
                return new Dijkstra(edgeExplorer, weighting);

            case FLOYWARSHALL:
                return new FloydWarshall(edgeExplorer, weighting);

            case BELLMANFORD:
                return new BellmanFord(edgeExplorer, weighting);

            default:
                throw new Exception("Invalid option for strategy");
        }
    }


    public S2SStrategy(EdgeExplorer edgeExplorer, Weighting weighting) {
        mWeighting = weighting;
        mOutEdgeExplorer = edgeExplorer;
    }

    public abstract Paths compute(int[] set1, int[] set2);
}
