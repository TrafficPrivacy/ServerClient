import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.shapes.GHPoint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

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

        Logger.printf(Logger.INFO, "from: (%.05f, %.05f), to: (%.05f, %.05f)\n",
                srcLat, srcLon, destLat, destLon);

        Profiler profiler = new Profiler().start();

        Pair<int[], int[]> srcCircle = mMatrixComputer.getCircle(new GHPoint(srcLat, srcLon), RADIUS);
        Pair<int[], int[]> destCircle = mMatrixComputer.getCircle(new GHPoint(destLat, destLon), RADIUS);
        Paths srcPaths = new Paths(),
              destPaths = new Paths(),
              interPaths = new Paths();
        try {
            srcPaths = mMatrixComputer.set2Set(srcCircle.mFirst, srcCircle.mSecond);
            destPaths = mMatrixComputer.set2Set(destCircle.mSecond, destCircle.mFirst);
            interPaths = mMatrixComputer.set2Set(srcCircle.mSecond, destCircle.mSecond);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // generate the index to Geo-location reference
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
        profiler.endAndPrint();
    }
}
