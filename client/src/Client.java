import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.NodeAccess;
import org.mapsforge.core.model.LatLong;

import java.io.*;
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



    private AdjacencyList<Integer> parseReply(Reply reply) {
        AdjacencyList<Integer> graph = new AdjacencyList<>();
        int[] srcAll = reply.mSrcCircle.mFirst;
        int[] srcBorder = reply.mSrcCircle.mSecond;
        int[] dstAll = reply.mDestCircle.mFirst;
        int[] dstBorder = reply.mDestCircle.mSecond;
        // add all the nodes
        for (int index : srcAll) {
            graph.insertNode(index);
        }
        for (int index : dstAll) {
            graph.insertNode(index);
        }
        // add edges in the source circle
        for (int srcIdx : srcAll) {
            for (int srcBorderIdx : srcBorder) {
                if (reply.mSrcPaths.findWeight(srcIdx, srcBorderIdx) > 0) {
                    graph.insertEdge(srcIdx, srcBorderIdx)
                            .setWeight(srcIdx, srcBorderIdx, reply.mSrcPaths.findWeight(srcIdx, srcBorderIdx))
                            .setDistance(srcIdx, srcBorderIdx, reply.mSrcPaths.findDistance(srcIdx, srcBorderIdx));
                }
            }
        }

        // add edges in the destination circle

        for (int dstIdx : dstAll) {

            for (int dstBorderIdx : dstBorder) {
                if (reply.mDestPaths.findWeight(dstBorderIdx, dstIdx) > 0) {        // Notice
                    graph.insertEdge(dstBorderIdx, dstIdx)
                            .setWeight(dstBorderIdx, dstIdx, reply.mDestPaths.findWeight(dstBorderIdx, dstIdx))
                            .setDistance(dstBorderIdx, dstIdx, reply.mDestPaths.findDistance(dstBorderIdx, dstIdx));
                }
            }
        }

        // add edges between borders of two circles
        for (int srcBIdx : srcBorder) {
            for (int dstBIdx : dstBorder) {
                if (reply.mInterPaths.findWeight(srcBIdx, dstBIdx) > 0) {
                    graph.insertEdge(srcBIdx, dstBIdx)
                            .setWeight(srcBIdx, dstBIdx, reply.mInterPaths.findWeight(srcBIdx, dstBIdx))
                            .setDistance(srcBIdx, dstBIdx, reply.mInterPaths.findDistance(srcBIdx, dstBIdx));
                }
            }
        }
        return graph;
    }

    private Pair<HashMap<MyPoint, Integer>, HashMap<Integer, MyPoint>> mapNodes(Reply reply) {
        HashMap<MyPoint, Integer> map1 = new HashMap<>();
        HashMap<Integer, MyPoint> map2 = new HashMap<>();
        int[] srcIdx = reply.mSrcCircle.mFirst;
        int[] destIdx = reply.mDestCircle.mFirst;

        System.out.printf("%d, %d\n", srcIdx.length, destIdx.length);

        for (int i = 0; i < srcIdx.length; i++) {
            map1.put(reply.mSrcReference[i], srcIdx[i]);
            map2.put(srcIdx[i], reply.mSrcReference[i]);
        }

        for (int i = 0; i < destIdx.length; i++) {
            map1.put(reply.mDestReference[i], destIdx[i]);
            map2.put(destIdx[i], reply.mDestReference[i]);
        }

        return new Pair<>(map1, map2);
    }

    private ArrayList<MyPoint> recoveryPath(int start, int end, Paths finalPath, Paths srcPaths,
                                            Paths dstPaths, Paths interPath) {
        NodeAccess nodeAccess = mHopper.getGraphHopperStorage().getNodeAccess();
        ArrayList<MyPoint> path = new ArrayList<>();
        Integer[] metaPath = finalPath.findPath(start, end);
        if (metaPath != null) {
            for (int i = 1; i < metaPath.length; i++) {
            /*TODO: find a better implementation*/
                Integer[] sPath = srcPaths.findPath(metaPath[i - 1], metaPath[i]);
                Integer[] iPath = interPath.findPath(metaPath[i - 1], metaPath[i]);
                Integer[] dPath = dstPaths.findPath(metaPath[i - 1], metaPath[i]);
                if (sPath != null) {
                    for (int idx : sPath) {
                        path.add(new MyPoint(nodeAccess.getLat(idx), nodeAccess.getLon(idx)));
                    }
                } else if (iPath != null) {
                    for (int idx : iPath) {
                        path.add(new MyPoint(nodeAccess.getLat(idx), nodeAccess.getLon(idx)));
                    }
                } else if (dPath != null) {
                    for (int idx : dPath) {
                        path.add(new MyPoint(nodeAccess.getLat(idx), nodeAccess.getLon(idx)));
                    }
                }
            }
        }
        return path;
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

        AdjacencyList<Integer> graph = parseReply(reply);

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
        Paths paths = mStrategy.compute(reply.mSrcCircle.mFirst, reply.mDestCircle.mFirst);
        profiler.endAndPrint().start();

        // path recovery and visualization

        mUI.setVisible(true);

        int start = findNearest(startPoint);
        int end = findNearest(endPoint);
        System.out.printf("%d, %d\n", start, end);
        ArrayList<MyPoint> mainPath = recoveryPath(start, end,
                                        paths, reply.mSrcPaths, reply.mDestPaths, reply.mInterPaths);
        Logger.printf(Logger.DEBUG, "Main path len: %d\n", mainPath.size());
        System.out.println("Main path weight: " + paths.findDistance(start, end));
        mUI.setMainPath(mainPath);

        for (Pair<Integer, Integer> path : paths.getPaths()) {
            ArrayList<MyPoint> otherPath = recoveryPath(path.mFirst, path.mSecond,
                                        paths, reply.mSrcPaths, reply.mDestPaths, reply.mInterPaths);
            mUI.addPath(otherPath);
        }
        profiler.endAndPrint();
        mUI.showUpdate();
    }
}
