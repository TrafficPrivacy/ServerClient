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
        AdjacencyList<GHPoint> spTree = mSurroundings.getSurrounding(center.getLat(), center.getLon(), radius);
        System.out.printf("Number of points %d\n", spTree.getNodes().size());
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
        ArrayList<MyPoint> border = Convex.getConvex(MyPoint.convertFromGHPoint(points));
        int[] borderArray = new int[border.size()];
        for (int i = 0; i < borderArray.length; i++) {
            QueryResult closest = mGraphhopper
                                .getLocationIndex()
                                .findClosest(border.get(i).mFirst, border.get(i).mSecond, EdgeFilter.ALL_EDGES);
            borderArray[i] = closest.getClosestEdge().getBaseNode();
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
        System.out.println("received: " + srcLat);
        double srcLon = in.readDouble();
        System.out.println("received: " + srcLon);
        double destLat = in.readDouble();
        System.out.println("received: " + destLat);
        double destLon = in.readDouble();
        System.out.println("received: " + destLon);

        srcLat = 40.111319;
        srcLon = -88.22794;
        destLat = 41.665346;
        destLon = -87.761192;

        System.out.printf("from (%f, %f) to (%f, %f)", srcLat, srcLon, destLat, destLon);

        Pair<int[], int[]> srcCircle = mMatrixComputer.getCircle(new GHPoint(srcLat, srcLon), RADIUS);
        Pair<int[], int[]> destCircle = mMatrixComputer.getCircle(new GHPoint(destLat, destLon), RADIUS);
        Paths srcPaths = new Paths(),
                destPaths = new Paths(),
                interPaths = new Paths();
        try {
            srcPaths = mMatrixComputer.set2Set(srcCircle.mFirst, srcCircle.mSecond);
            destPaths = mMatrixComputer.set2Set(destCircle.mFirst, destCircle.mSecond);
            interPaths = mMatrixComputer.set2Set(srcCircle.mSecond, destCircle.mSecond);
        } catch (Exception e) {
            e.printStackTrace();

        }

        // generate the index to geolocation reference
        ArrayList<MyPoint> srcGeo = new ArrayList<>();
        ArrayList<MyPoint> destGeo = new ArrayList<>();
        NodeAccess nodeAccess = mMatrixComputer.getNodeAccess();
        for (int i = 0; i < srcCircle.mFirst.length; i++) {
            srcGeo.add(new MyPoint(nodeAccess.getLat(srcCircle.mFirst[i]), nodeAccess.getLon(srcCircle.mFirst[i])));
        }
        for (int i = 0; i < destCircle.mFirst.length; i++) {
            destGeo.add(new MyPoint(nodeAccess.getLat(destCircle.mFirst[i]), nodeAccess.getLon(destCircle.mFirst[i])));
        }

        MyPoint[] srcRef  = new MyPoint[srcGeo.size()];
        MyPoint[] destRef = new MyPoint[destGeo.size()];
        srcGeo.toArray(srcRef);
        destGeo.toArray(destRef);

        Reply reply = new Reply(srcCircle, destCircle, srcPaths, destPaths, interPaths, srcRef, destRef);
        ObjectOutputStream oOut = new ObjectOutputStream(out);
        oOut.writeObject(reply);
        oOut.close();
        out.close();
    }
}
