package server;

import util.*;

import javax.swing.plaf.synth.Region;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DistanceCalcEarth;

import java.util.ArrayList;

public class ReturnGoodRegions {
    private Pair<MapPoint, MapPoint> region;
    private Surroundings mSurroundings;
    private GraphHopper mHopper;
    private EncodingManager mEm;
    private String mStrategy;

    public ReturnGoodRegions(String osmPath, String osmFolder, String strategy)
    {
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

    public Pair<int[], int[]> getPrivacyRegion(MapPoint center, InRegionTest inRegionTest)
            throws PointNotFoundException {
        Pair<ArrayList<MapPoint>, ArrayList<MapPoint>> pointsAndBorder = mSurroundings
                .getSurroundingAndBoundary(center.getLat(), center.getLon(), inRegionTest);
        ArrayList<MapPoint> points = pointsAndBorder.mFirst;
        if (points.size() == 0) {
            throw new PointNotFoundException(center);
        }
        int[] allArray = new int[points.size()];
        // find all points
        for (int i = 0; i < allArray.length; i++) {
            QueryResult closest = mHopper
                    .getLocationIndex()
                    .findClosest(points.get(i).getLat(), points.get(i).getLon(), EdgeFilter.ALL_EDGES);
            allArray[i] = closest.getClosestNode();
        }
        // find all the border points
        ArrayList<MapPoint> border = pointsAndBorder.mSecond;
        int[] borderArray = new int[border.size()];
        for (int i = 0; i < borderArray.length; i++) {
            QueryResult closest = mHopper
                    .getLocationIndex()
                    .findClosest(border.get(i).mFirst, border.get(i).mSecond, EdgeFilter.ALL_EDGES);
            borderArray[i] = closest.getClosestNode();
        }
        return new Pair<>(allArray, borderArray);
    }
}
