package client;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import java.util.ArrayList;
import util.Pair;

public class VisualizerTest {

  public static void main(String[] args) {
    String pbfPath = args[0];
    String ghPath = args[1];
    String mapsforgePath = args[2];
    EncodingManager em = new EncodingManager("car");
    GraphHopper hopper = new GraphHopperOSM()
        .setOSMFile(pbfPath)
        .forDesktop()
        .setGraphHopperLocation(ghPath)
        .setEncodingManager(em)
        .importOrLoad();
    double[][] borderPoints = {
        {40.104107, -88.230257}, {40.104644, -88.230289}, {40.104098, -88.230252},
        {40.103245, -88.230246}, {40.10176, -88.230203}, {40.100611, -88.230203},
        {40.100602, -88.227757}, {40.100602, -88.227757}, {40.100627, -88.221738},
        {40.102711, -88.221813}, {40.10418, -88.221889}, {40.104148, -88.22677},
        {40.105305, -88.223841}, {40.105945, -88.225515}, {40.106856, -88.228841}
    };

    double[][] points = {
        {40.102974, -88.228691}, {40.102301, -88.225214}, {40.10235, -88.224619}
    };

    int[] borders = new int[borderPoints.length];
    int[] allPoints = new int[borderPoints.length + points.length];

    for (int i = 0; i < borderPoints.length; ++i) {
      borders[i] = findNearest(new Pair<>(borderPoints[i][0], borderPoints[i][1]), hopper);
    }

    for (int i = 0; i < points.length; ++i) {
      allPoints[i] = findNearest(new Pair<>(points[i][0], points[i][1]), hopper);
    }

    for (int i = 0; i < borders.length; ++i) {
      allPoints[i + points.length] = borders[i];
    }

    Visualizer visualizer = new Visualizer(mapsforgePath, "Test",
        hopper.getGraphHopperStorage().getNodeAccess());

    ArrayList<Pair<int[], int[]>> regions = new ArrayList<>();
    regions.add(new Pair<>(allPoints, borders));
    visualizer.drawRegions(regions);
    visualizer.show();
  }

  private static int findNearest(Pair<Double, Double> original, GraphHopper hopper) {
    int closest = hopper.getLocationIndex()
        .findClosest(original.mFirst, original.mSecond, EdgeFilter.ALL_EDGES)
        .getClosestNode();
    return closest;
  }
}
