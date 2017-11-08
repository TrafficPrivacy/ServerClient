package server;

import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.shapes.GHPoint;
import util.*;

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
    private ServerSocket mServer;
    private MatrixComputer mMatrixComputer;

    private final double RADIUS = 500.0;

    public Server(int port, String osmPath, String osmFolder, String strategy) throws IOException {
        mServer = new ServerSocket(port);
        mMatrixComputer = new MatrixComputer(osmPath, osmFolder, strategy);
    }

    public void run() {
        while (true) {
            try {
                Socket sock = mServer.accept();
                handleOne(sock);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleOne(Socket sock) throws Exception {
        DataInputStream in = new DataInputStream(sock.getInputStream());
        DataOutputStream out = new DataOutputStream(sock.getOutputStream());

        double srcLat = in.readDouble();
        double srcLon = in.readDouble();
        double dstLat = in.readDouble();
        double dstLon = in.readDouble();

        Logger.printf(Logger.INFO, "from: (%.05f, %.05f), to: (%.05f, %.05f)\n",
                srcLat, srcLon, dstLat, dstLon);

        Profiler profiler = new Profiler().start();
        Reply reply = calculate(srcLat, srcLon, dstLat, dstLon);
        profiler.endAndPrint();
        ObjectOutputStream oOut = new ObjectOutputStream(out);
        oOut.writeObject(reply);
        oOut.close();
        out.close();
    }

    private Reply calculate(double srcLat, double srcLon, double dstLat, double dstLon) {
        Pair<int[], int[]> srcCircle;
        Pair<int[], int[]> dstCircle;
        try {
            srcCircle = mMatrixComputer.getCircle(new GHPoint(srcLat, srcLon), RADIUS);
            dstCircle = mMatrixComputer.getCircle(new GHPoint(dstLat, dstLon), RADIUS);
        } catch (PointNotFoundException e) {
            e.printStackTrace();
            return new Reply(null, null, null, null, null, Reply.ERROR);
        }
        int endCenter = mMatrixComputer.getmSurroundings().lookupNearest(dstLat, dstLon);
        /*TODO: refactor this*/
        Paths srcPaths = mMatrixComputer.set2Set(srcCircle.mFirst, srcCircle.mSecond, false, endCenter, RADIUS);
        Paths dstPaths = mMatrixComputer.set2Set(dstCircle.mSecond, dstCircle.mFirst, false, endCenter, RADIUS);
        Paths interPaths = mMatrixComputer.set2Set(srcCircle.mSecond, dstCircle.mSecond, true, endCenter, RADIUS);

        return new Reply(srcCircle, dstCircle, srcPaths, dstPaths, interPaths, Reply.OK);
    }
}
