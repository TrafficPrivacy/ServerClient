import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.shapes.GHPoint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

class MatrixComputer {
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
            mStrategy = S2SStrategy.strategyFactory(strategy, outEdgeExplorer,
                                        new FastestWeighting(em.getEncoder("car")));
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
        AdjacencyList<GHPoint> spTree = mSurroundings.getSurrounding(center.getLat(), center.getLon(), radius);
        ArrayList<GHPoint> points = spTree.getNodes();
        int[] allArray = new int[points.size()];
        // find all points
        for (int i = 0; i < allArray.length; i++) {
            QueryResult closest = mGraphhopper
                                .getLocationIndex()
                                .findClosest(points.get(i).lat, points.get(i).lon, EdgeFilter.ALL_EDGES);
            allArray[i] = closest.getClosestEdge().getBaseNode();
        }
        // find all the border points
        ArrayList<GHPoint> borderPoints = new ArrayList<>();
        for (GHPoint point : points) {
            if (spTree.getNeighbors(point).isEmpty()) {
                borderPoints.add(point); // Since the sptree is directed in this implementation
            }
        }
        int[] borderArray = new int[borderPoints.size()];
        for (int i = 0; i < borderArray.length; i++) {
            QueryResult closest = mGraphhopper
                                .getLocationIndex()
                                .findClosest(borderPoints.get(i).lat, borderPoints.get(i).lon, EdgeFilter.ALL_EDGES);
            borderArray[i] = closest.getClosestEdge().getBaseNode();
        }
        return new Pair<>(allArray, borderArray);
    }

    public Paths inCirclePaths(int[] allPoints, int[] borderPoints) {
        return mStrategy.compute(allPoints, borderPoints);
    }
}

/**
 * The server is responsible to compute three matrices:
 *   1. The all to border shortest path in the start region
 *   2. The all to border shortest path in the destination region
 *   3. The shortest path between any pair of border points (start region, destination region)
 */
public class Server {
    private int mPort;
    private ServerSocket mServer;
    private MatrixComputer mMatrixComputer;

    private final double RADIUS = 1000.0;

    public Server(int port, String osmPath, String osmFolder, int strategy) throws IOException {
        mPort = port;
        mServer = new ServerSocket(port);
        mMatrixComputer = new MatrixComputer(osmPath, osmFolder, strategy);
    }

    public void run() {
        while (true) {
            try {
                Socket sock = mServer.accept();
                handleOne(sock);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleOne(Socket sock) throws IOException {
        DataInputStream in = new DataInputStream(sock.getInputStream());
        DataOutputStream out = new DataOutputStream(sock.getOutputStream());

        double srcLat = in.readDouble();
        double srcLon = in.readDouble();
        double destLat = in.readDouble();
        double destLon = in.readDouble();

        Pair<int[], int[]> srcCircle = mMatrixComputer.getCircle(new GHPoint(srcLat, srcLon), RADIUS);
        Pair<int[], int[]> destCircle = mMatrixComputer.getCircle(new GHPoint(destLat, destLon), RADIUS);
        Paths srcPaths = mMatrixComputer.inCirclePaths(srcCircle.mFirst, srcCircle.mSecond);
        Paths destPaths = mMatrixComputer.inCirclePaths(destCircle.mFirst, destCircle.mSecond);
        Paths interPaths = mMatrixComputer.inCirclePaths(srcCircle.mSecond, destCircle.mSecond);

        Reply reply = new Reply(srcCircle, destCircle, srcPaths, destPaths, interPaths);
        ObjectOutputStream Oout = new ObjectOutputStream(out);
        Oout.writeObject(reply);
        Oout.close();
        out.close();
    }
}
