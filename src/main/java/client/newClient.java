package client;

import algorithm.CallBacks;
import algorithm.EdgeIterator;
import algorithm.S2SStrategy;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import util.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;

public class newClient {

    private int mServerPort;
    private PostProcess mPostProcess;
    private GraphHopper mHopper;
    private String mServerIP;

    public newClient(
            String serverIP,
            int serverPort,
            String pbfPath,
            String ghPath,
            PostProcess postProcess
    ) throws IOException {
        mServerPort = serverPort;
        EncodingManager em = new EncodingManager("car");
        mHopper = new GraphHopperOSM()
                .setOSMFile(pbfPath)
                .forDesktop()
                .setGraphHopperLocation(ghPath)
                .setEncodingManager(em)
                .importOrLoad();
        mServerIP = serverIP;
        mPostProcess = postProcess;
    }

    public void setPostProcess(PostProcess postProcess) {
        mPostProcess = postProcess;
    }

    private int findNearest(Pair<Double, Double> original) {
        int closest = mHopper.getLocationIndex()
                .findClosest(original.mFirst, original.mSecond, EdgeFilter.ALL_EDGES)
                .getClosestNode();
        return closest;
    }

    /**
     * Compute the route and visualize it. **Note** currently doesn't have random shift
     *
     * @param startPoint The start point
     * @param endPoint The end point
     */
    public void compute(
            MapPoint startPoint,
            MapPoint endPoint) throws
            IOException,
            ClassNotFoundException,
            NoSuchStrategyException,
            NoEdgeIteratorException,
            MainPathEmptyException {
        /*TODO: add random shift*/
        MapPoint shiftStart = startPoint;
        MapPoint shiftEnd = endPoint;
        Pair<MapPoint,MapPoint> source_region=new Pair<>(new MapPoint(startPoint.mFirst-0.05,startPoint.mSecond-0.05),new MapPoint(startPoint.mFirst+0.05,startPoint.mSecond+0.05));
        Pair<MapPoint,MapPoint> destination_region=new Pair<>(new MapPoint(endPoint.mFirst-0.05,endPoint.mSecond-0.05),new MapPoint(endPoint.mFirst+0.05,endPoint.mSecond+0.05));
        Socket client = new Socket(mServerIP, mServerPort);
        OutputStream socketOut = client.getOutputStream();
        DataOutputStream out = new DataOutputStream(socketOut);
        ObjectOutputStream oOut = new ObjectOutputStream(out);

        oOut.writeObject(source_region);
        oOut.writeObject(destination_region);

        out.writeDouble(shiftStart.getLat());
        out.writeDouble(shiftStart.getLon());
        out.writeDouble(shiftEnd.getLat());
        out.writeDouble(shiftEnd.getLon());

        InputStream socketIn = client.getInputStream();
        DataInputStream in = new DataInputStream(socketIn);

        ObjectInputStream oIn = new ObjectInputStream(in);
        Reply reply = (Reply) oIn.readObject();
        oIn.close();
        in.close();
        out.close();

        AdjacencyList<Integer> graph;
        try {
            graph = reply.parse();
        } catch (ReplyOnErrorException e) {
            throw new MainPathEmptyException();
        }

        S2SStrategy strategy = S2SStrategy.strategyFactory(S2SStrategy.DIJKSTRA, new CallBacks() {
            @Override
            public EdgeIterator getIterator(int current, int prevEdgeID) {
                return new ClientEdgeIterator(current, graph);
            }

            @Override
            public double getPotential(int current, HashSet<Integer> targets) {
                return 0;
            }
        });

        Profiler profiler = new Profiler().start();
        Paths paths = strategy.compute(reply.getSrcPoints(), reply.getDstPoints());
        profiler.endAndPrint().start();

        // path recovery and visualization

        int start = findNearest(startPoint);
        int end = findNearest(endPoint);
        ArrayList<MapPoint> mainPath =
                reply.recoveryPath(paths.findPath(start, end),
                        mHopper.getGraphHopperStorage().getNodeAccess());
        mPostProcess.setMainPath(mainPath);

        for (Integer[] path : paths.getPaths()) {
            ArrayList<MapPoint> otherPath = reply
                    .recoveryPath(path, mHopper.getGraphHopperStorage().getNodeAccess());
            mPostProcess.addPath(otherPath);
        }
        profiler.endAndPrint();
        mPostProcess.done();
    }
}
