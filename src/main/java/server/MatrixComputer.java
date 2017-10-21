package server;

import algorithm.*;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.shapes.GHPoint;
import util.*;

import java.util.ArrayList;
import java.util.HashSet;

public class MatrixComputer {
    private String mStrategy;
    private EncodingManager mEm;
    private GraphHopper mHopper;
    private Surroundings mSurroundings;

    public MatrixComputer(String osmPath, String osmFolder, String strategy) {
        /*TODO: change this hard coded encoding*/
        mEm = new EncodingManager("car");
        mHopper = new GraphHopperOSM()
                .setOSMFile(osmPath)
                .forDesktop()
                .setGraphHopperLocation(osmFolder)
                .setEncodingManager(mEm)
                .importOrLoad();

        mStrategy = strategy;
        mSurroundings = new Surroundings(mHopper.getGraphHopperStorage(),
                mHopper.getLocationIndex(), mEm.getEncoder("car"));
    }

    public Surroundings getmSurroundings() {
        return mSurroundings;
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
            QueryResult closest = mHopper
                    .getLocationIndex()
                    .findClosest(points.get(i).lat, points.get(i).lon, EdgeFilter.ALL_EDGES);
            allArray[i] = closest.getClosestNode();
        }
        // find all the border points
        ArrayList<MapPoint> border = Convex.getConvex(MapPoint.convertFromGHPoint(points));
        int[] borderArray = new int[border.size()];
        for (int i = 0; i < borderArray.length; i++) {
            QueryResult closest = mHopper
                    .getLocationIndex()
                    .findClosest(border.get(i).mFirst, border.get(i).mSecond, EdgeFilter.ALL_EDGES);
            borderArray[i] = closest.getClosestNode();
        }
        return new Pair<>(allArray, borderArray);
    }

    public Paths set2Set(int[] set1, int[] set2, boolean hasCenter, int targetCenter, double radius) throws Exception {
        try {
            /*TODO: change the weight*/
            S2SStrategy strategy = S2SStrategy.strategyFactory(mStrategy, new CallBacks() {
                @Override
                public EdgeIter getIterator(int current, int prevEdgeID) {
                    /*TODO: change the hard coded name too*/
                    if (mStrategy.equalsIgnoreCase(S2SStrategy.ASTAR)) {
                        return new AStarEdgeIterator(current, prevEdgeID, mHopper.getGraphHopperStorage()
                                .createEdgeExplorer(new DefaultEdgeFilter(mEm.getEncoder("car"), false, true)));
                    }
                    return new DefaultEdgeIterator(current, prevEdgeID, mHopper.getGraphHopperStorage()
                            .createEdgeExplorer(new DefaultEdgeFilter(mEm.getEncoder("car"), false, true)));
                }

                /**
                 * Get the minimum potential among all the targets
                 * @param current current node
                 * @param targets the targets
                 * @return the minimum potential
                 */
                @Override
                public double getPotential(int current, HashSet<Integer> targets) {
                    if (!mStrategy.equalsIgnoreCase(S2SStrategy.ASTAR)) {
                        return 0.0;
                    }
                    NodeAccess nodeAccess = mHopper.getGraphHopperStorage().getNodeAccess();
                    DistanceCalcEarth distanceCalcEarth = new DistanceCalcEarth();
                    double fromLat = nodeAccess.getLat(current);
                    double fromLon = nodeAccess.getLon(current);
                    double toLat = nodeAccess.getLat(targetCenter);
                    double toLon = nodeAccess.getLon(targetCenter);
                    double toCenter = distanceCalcEarth.calcDist(fromLat, fromLon, toLat, toLon);
                    if (!hasCenter || toCenter < radius) {
                        double minDist = 1e100;
                        for (int target : targets) {
                            toLat = nodeAccess.getLat(target);
                            toLon = nodeAccess.getLon(target);
                            double distance = distanceCalcEarth.calcDist(fromLat, fromLon, toLat, toLon);
                            if (distance < minDist) {
                                minDist = distance;
                            }
                        }
                        return minDist;
                    }
                    return toCenter - radius;
                }
            });
            return strategy.compute(set1, set2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public NodeAccess getNodeAccess() {
        return mHopper.getGraphHopperStorage().getNodeAccess();
    }
}

