import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import org.mapsforge.core.model.LatLong;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;

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
                if (reply.mDestPaths.findWeight(dstIdx, dstBorderIdx) > 0) {
                    graph.insertEdge(dstIdx, dstBorderIdx)
                            .setWeight(dstIdx, dstBorderIdx, reply.mDestPaths.findWeight(dstIdx, dstBorderIdx))
                            .setDistance(dstIdx, dstBorderIdx, reply.mDestPaths.findDistance(dstIdx, dstBorderIdx));
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
        System.out.println("client received");
        oIn.close();
        System.out.println("1");
        in.close();
        System.out.println("2");
        out.close();
        System.out.println("3");

        AdjacencyList<Integer> graph = parseReply(reply);

        mStrategy = S2SStrategy.strategyFactory(S2SStrategy.DIJKSTRA, new S2SStrategy.EdgeProvider() {
            @Override
            public S2SStrategy.EdgeIter getIterator(int current, int prevEdgeID) {
                return new ClientEdgeIterator(current, graph);
            }
        });

        Paths paths = mStrategy.compute(reply.mSrcCircle.mFirst, reply.mDestCircle.mFirst);

        Pair<HashMap<MyPoint, Integer>, HashMap<Integer, MyPoint>> references = mapNodes(reply);

        System.out.printf("main path weight: %f\n", paths.findWeight(reply.mSrcCircle.mFirst[0], reply.mDestCircle.mFirst[0]));

        // reconstruct main path
        Integer[] mainPath = paths.findPath(reply.mSrcCircle.mFirst[0], reply.mDestCircle.mFirst[0]);
        PointList mainList = new PointList();

        for (int i = 0; i < mainPath.length; i++) {
            MyPoint myPoint = references.mSecond.get(mainPath[i]);
            System.out.printf("%d: (%f, %f)\n", i, myPoint.mFirst, myPoint.mSecond);
        }

        Integer[] srcCirPath = reply.mSrcPaths.findPath(mainPath[2], mainPath[3]);
        Integer[] interPath = reply.mInterPaths.findPath(mainPath[2], mainPath[1]);
        Integer[] destPath = reply.mDestPaths.findPath(mainPath[0], mainPath[1]);

        // some debug checks
        // check mainPath[2] is on the source border
        boolean on_2 = false;
        for (int i = 0; i < reply.mSrcCircle.mSecond.length; i++) {
            if (mainPath[2] == reply.mSrcCircle.mSecond[i]) {
                on_2 = true;
                break;
            }
        }
        System.out.println("Mainpath[2] on border: " + on_2);

        // check mainPath[1] is on the dest border
        boolean on_1 = false;
        for (int i = 0; i < reply.mDestCircle.mSecond.length; i++) {
            if (mainPath[1] == reply.mDestCircle.mSecond[i]) {
                on_1 = true;
                break;
            }
        }
        System.out.println("Mainpath[1] on border: " + on_1);

        NodeAccess nodeAccess = mHopper.getGraphHopperStorage().getNodeAccess();

        for (int idx : srcCirPath) {
            mainList.add(new GHPoint(nodeAccess.getLat(idx), nodeAccess.getLon(idx)));
        }

        for (int i = interPath.length - 1; i >= 0; i--) {
            mainList.add(new GHPoint(nodeAccess.getLat(interPath[i]), nodeAccess.getLon(interPath[i])));
        }

        for (int idx : destPath) {
            mainList.add(new GHPoint(nodeAccess.getLat(idx), nodeAccess.getLon(idx)));
        }

        System.out.println("main path length: " + mainList.size());
        //mUI.addPath(mainList);
        mUI.setVisible(true);

        for (int idx : reply.mSrcCircle.mFirst) {
            LatLong dot = new LatLong(nodeAccess.getLat(idx), nodeAccess.getLon(idx));
            mUI.createDot(dot, new java.awt.Color(6, 0, 133, 255).getRGB(), 6);
        }

        for (int idx : reply.mSrcCircle.mSecond) {
            LatLong dot = new LatLong(nodeAccess.getLat(idx), nodeAccess.getLon(idx));
            mUI.createDot(dot, new java.awt.Color(133, 22, 9, 255).getRGB(), 6);
        }

        mUI.setMainPath(mainList);
        mUI.showUpdate();
    }
}
