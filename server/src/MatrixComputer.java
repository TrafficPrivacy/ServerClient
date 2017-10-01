import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.shapes.GHPoint;

import java.util.ArrayList;

public class MatrixComputer {
    final private String mOSMPath;
    final private String mOSMFOlder;
    private S2SStrategy mStrategy;
    private GraphHopper mGraphhopper;
    private Surroundings mSurroundings;

    public MatrixComputer(String osmPath, String osmFolder, int strategy) {
        mOSMPath = osmPath;
        mOSMFOlder = osmFolder;
        /*TODO: change this hard coded encoding*/
        EncodingManager em = new EncodingManager("car");
        mGraphhopper = new GraphHopperOSM()
                .setOSMFile(osmPath)
                .forDesktop()
                .setGraphHopperLocation(osmFolder)
                .setEncodingManager(em)
                .importOrLoad();
        GraphHopperStorage ghStore = mGraphhopper.getGraphHopperStorage();
        /*TODO: change the hard coded name too*/
        EdgeExplorer outEdgeExplorer = ghStore.createEdgeExplorer(new DefaultEdgeFilter(em.getEncoder("car"),
                false, true));
        try {
            /*TODO: change the weight*/
            mStrategy = S2SStrategy.strategyFactory(strategy, new S2SStrategy.EdgeProvider() {
                @Override
                public S2SStrategy.EdgeIter getIterator(int current, int prevEdgeID) {
                    return new DefaultEdgeIterator(current, prevEdgeID, mGraphhopper.getGraphHopperStorage()
                            .createEdgeExplorer(new DefaultEdgeFilter(em.getEncoder("car"), false, true)));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        mSurroundings = new Surroundings(mGraphhopper.getGraphHopperStorage(),
                mGraphhopper.getLocationIndex(), em.getEncoder("car"));
    }

    /**
     * Find all the points inside a circle of certain radius
     * @param center The center point
     * @param radius The radius of the circle. In meters?
     * @return A pair of integer arrays. The first in the pair represents all the points in the
     *         circle. The second represents the border points.
     */
    public Pair<int[], int[]> getCircle(GHPoint center, double radius) {
        ArrayList<GHPoint> points = mSurroundings.getSurrounding(center.getLat(), center.getLon(), radius);
        int[] allArray = new int[points.size()];
        // find all points
        for (int i = 0; i < allArray.length; i++) {
            QueryResult closest = mGraphhopper
                    .getLocationIndex()
                    .findClosest(points.get(i).lat, points.get(i).lon, EdgeFilter.ALL_EDGES);
            allArray[i] = closest.getClosestNode();
        }
        // find all the border points
        ArrayList<MyPoint> border = Convex.getConvex(MyPoint.convertFromGHPoint(points));
        int[] borderArray = new int[border.size()];
        for (int i = 0; i < borderArray.length; i++) {
            QueryResult closest = mGraphhopper
                    .getLocationIndex()
                    .findClosest(border.get(i).mFirst, border.get(i).mSecond, EdgeFilter.ALL_EDGES);
            borderArray[i] = closest.getClosestNode();
        }
        return new Pair<>(allArray, borderArray);
    }

    public Paths set2Set(int[] set1, int[] set2) throws Exception {
        return mStrategy.compute(set1, set2);
    }

    public NodeAccess getNodeAccess() {
        return mGraphhopper.getGraphHopperStorage().getNodeAccess();
    }
}
