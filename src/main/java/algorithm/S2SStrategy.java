package algorithm;

import util.Paths;

public abstract class S2SStrategy {

    protected CallBacks mCallBacks;

    public static final String DIJKSTRA = "DIJKSTRA";
    public static final String FLOYWARSHALL = "FLOYWARSHALL";
    public static final String BELLMANFORD = "BELLMANFORD";
    public static final String ASTAR = "ASTAR";
    public static final String ASTARNOREEVALUATE = "ASTARNOREEVALUATE";

    public static S2SStrategy strategyFactory(String type, CallBacks callBacks) throws Exception {

        if (type.equalsIgnoreCase(DIJKSTRA)) {

            return new Dijkstra(callBacks);

        } else if (type.equalsIgnoreCase(FLOYWARSHALL)) {

            return new FloydWarshall(callBacks);

        } else if (type.equalsIgnoreCase(BELLMANFORD)) {

            return new BellmanFord(callBacks);

        } else if (type.equalsIgnoreCase(ASTAR)) {

            return new AStar(callBacks);

        } else if (type.equalsIgnoreCase(ASTARNOREEVALUATE)) {

            return new Dijkstra(callBacks);

        } else {

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
