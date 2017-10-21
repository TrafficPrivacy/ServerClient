package client;

import algorithm.CallBacks;
import algorithm.EdgeIter;
import algorithm.S2SStrategy;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.NodeAccess;
import util.*;

import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Client {
    private int mServerPort;
    private MapUI mUI;
    private GraphHopper mHopper;
    private String mServerIP;
    private S2SStrategy mStrategy;

    public Client(String serverIP, int serverPort, String pbfPath, String ghPath, String mapPath) throws Exception{
        mServerPort = serverPort;
        mUI = new MapUI(mapPath, "Client");
        EncodingManager em = new EncodingManager("car");
        mHopper = new GraphHopperOSM()
                .setOSMFile(pbfPath)
                .forDesktop()
                .setGraphHopperLocation(ghPath)
                .setEncodingManager(em)
                .importOrLoad();
        mServerIP = serverIP;
    }

    private int findNearest(Pair<Double, Double> original) {
        int closest = mHopper.getLocationIndex()
                .findClosest(original.mFirst, original.mSecond, EdgeFilter.ALL_EDGES)
                .getClosestNode();
        return closest;
    }

    /**
     * Compute the route and visualize it.
     * **Note** currently doesn't have random shift
     * @param startPoint The start point
     * @param endPoint The end point
     */
    public void compute(Pair<Double, Double> startPoint, Pair<Double, Double> endPoint) throws Exception{
        /*TODO: add random shift*/
        Pair<Double, Double> shiftStart = startPoint;
        Pair<Double, Double> shiftEnd = endPoint;

        Socket client = new Socket(mServerIP, mServerPort);
        OutputStream socketOut = client.getOutputStream();
        DataOutputStream out = new DataOutputStream(socketOut);

        out.writeDouble(shiftStart.mFirst);
        out.writeDouble(shiftStart.mSecond);
        out.writeDouble(shiftEnd.mFirst);
        out.writeDouble(shiftEnd.mSecond);

        InputStream socketIn = client.getInputStream();
        DataInputStream in = new DataInputStream(socketIn);

        ObjectInputStream oIn = new ObjectInputStream(in);
        Reply reply = (Reply) oIn.readObject();
        oIn.close();
        in.close();
        out.close();

        AdjacencyList<Integer> graph = reply.parse();

        mStrategy = S2SStrategy.strategyFactory(S2SStrategy.DIJKSTRA, new CallBacks() {
            @Override
            public EdgeIter getIterator(int current, int prevEdgeID) {
                return new ClientEdgeIterator(current, graph);
            }

            @Override
            public double getPotential(int current, HashSet<Integer> targets) {
                return 0;
            }
        });

        Profiler profiler = new Profiler().start();
        Paths paths = mStrategy.compute(reply.getSrcPoints(), reply.getDstPoints());
        profiler.endAndPrint().start();

        // path recovery and visualization

        mUI.setVisible(true);

        int start = findNearest(startPoint);
        int end = findNearest(endPoint);
        ArrayList<MapPoint> mainPath =
                reply.recoveryPath(paths.findPath(start, end), mHopper.getGraphHopperStorage().getNodeAccess());
        mUI.setMainPath(mainPath);

        for (Integer[] path : paths.getPaths()) {
            ArrayList<MapPoint> otherPath = reply.recoveryPath(path, mHopper.getGraphHopperStorage().getNodeAccess());
            mUI.addPath(otherPath);
        }
        profiler.endAndPrint();
        mUI.showUpdate();
    }
}

