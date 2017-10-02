/**
 * The set to set shortest path strategy abstract class
 */
public abstract class S2SStrategy {

    protected CallBacks mCallBacks;

    public static final int DIJKSTRA = 0;
    public static final int FLOYWARSHALL = 1;
    public static final int BELLMANFORD = 2;
    public static final int ASTAR = 3;

    public static S2SStrategy strategyFactory(int type, CallBacks callBacks) throws Exception {
        switch (type) {
            case DIJKSTRA:
                return new Dijkstra(callBacks);

            case FLOYWARSHALL:
                return new FloydWarshall(callBacks);

            case BELLMANFORD:
                return new BellmanFord(callBacks);

            case ASTAR:
                return new AStar(callBacks);

            default:
                throw new Exception("Invalid option for strategy");
        }
    }


    public S2SStrategy(CallBacks callBacks) {
        mCallBacks = callBacks;
    }

    public S2SStrategy setEdgeProvider(CallBacks callBacks) {
        mCallBacks = callBacks;
        return this;
    }

    public abstract Paths compute(int[] set1, int[] set2) throws Exception;

}
