import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.*;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.Parameters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class S2SStrategyTest {
    final private double LATMIN = 39.65;
    final private double LATMAX = 40.39;
    final private double LONMIN = -90.63;
    final private double LONMAX = -87.91;
    final private int STARTSETSIZE = 20;
    final private int DESTSETSIZE  = 20;
    final private int ALGORITHM = S2SStrategy.DIJKSTRA;

    private GraphHopper mHopper;
    private int[] mStartSet;
    private int[] mDestSet;
    private S2SStrategy mStrategy;

    private int[] generatePoints(int number) {
        int[] pointIdx = new int[number];
        for (int i = 0; i < number; i++) {
            double lat = Math.random() * (LATMAX - LATMIN) + LATMIN;
            double lon = Math.random() * (LONMAX - LONMIN) + LONMIN;
            QueryResult closest = mHopper.getLocationIndex().findClosest(lat, lon, EdgeFilter.ALL_EDGES);
            pointIdx[i] = closest.getClosestEdge().getBaseNode();
        }
        return pointIdx;
    }

    private PathWrapper calcPath(int from, int to) throws Exception{
        NodeAccess nodeAccess = mHopper.getGraphHopperStorage().getNodeAccess();
        System.out.printf("from: (%f, %f) to: (%f, %f)\n", nodeAccess.getLat(from), nodeAccess.getLon(from),
                                      nodeAccess.getLat(to), nodeAccess.getLon(to));
        GHRequest req = new GHRequest(nodeAccess.getLat(from), nodeAccess.getLon(from),
                                      nodeAccess.getLat(to), nodeAccess.getLon(to))
                        .setWeighting("fastest")
                        .setAlgorithm(Parameters.Algorithms.DIJKSTRA_BI);

        req.getHints().put(Parameters.Routing.INSTRUCTIONS, "false");
        GHResponse resp = mHopper.route(req);
        return resp.getBest();
    }

    @Before
    public void setUp() throws Exception {
        EncodingManager em = new EncodingManager("car");
        mHopper = new GraphHopperOSM()
                  .setOSMFile("../data/illinois-latest.osm.pbf")
                  .forDesktop()
                  .setGraphHopperLocation("../data/illinois")
                  .setEncodingManager(em)
                  .importOrLoad();
        mStartSet = generatePoints(STARTSETSIZE);
        mDestSet  = generatePoints(DESTSETSIZE);
        mStrategy = S2SStrategy.strategyFactory(ALGORITHM, mHopper.getGraphHopperStorage()
                        .createEdgeExplorer(new DefaultEdgeFilter(em.getEncoder("car"), false, true)),
                        new FastestWeighting(em.getEncoder("car")));
    }

    @Test
    public void test() throws Exception {
        Paths paths = mStrategy.compute(mStartSet, mDestSet);
        NodeAccess nodeAccess = mHopper.getGraphHopperStorage().getNodeAccess();
        // check each pair against graphhopper's result
        ArrayList<PathWrapper> ghPaths = new ArrayList<>();
        for (int i = 0; i < STARTSETSIZE; i ++) {
            for (int j = 0; j < DESTSETSIZE; j ++) {
                PathWrapper ghPath;
                try {
                    ghPath = calcPath(mDestSet[j], mStartSet[i]);
                } catch (Exception e) {
                    System.out.println("GH says there is no path");
                    continue;
                }
                ghPaths.add(ghPath);

                double myDist = paths.findPath(mStartSet[i], mDestSet[j]).mFirst;
                Integer[] myPath = paths.findPath(mStartSet[i], mDestSet[j]).mSecond;
                System.out.printf("gh distance %f, my distance %f\n", ghPath.getDistance(), myDist);
                //assertEquals("Path disatnce", ghPath.getDistance(), myDist, 1000.0);

                for (int k = 0; k < ghPath.getPoints().size(); k ++) {
                    System.out.printf("gh path: (%f, %f)\n", ghPath.getPoints().getLat(k), ghPath.getPoints().getLon(k));
                }

                for (int k = 0; k < myPath.length; k++) {
                    System.out.printf("my path (%f, %f)\n", nodeAccess.getLat(myPath[k]), nodeAccess.getLon(myPath[k]));
                }
            }
        }
    }

    @After
    public void tearDown() throws Exception {

    }

}