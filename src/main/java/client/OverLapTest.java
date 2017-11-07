package client;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.NodeAccess;
import util.FlagParser;
import util.MapPoint;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.System.exit;

public class OverLapTest {
    private static FlagParser mFlagParser;
    private static Client mClient;

    public static void main(String args[]) throws Exception {
        setupArgs();
        mFlagParser.parseArgs(args);
        String ip = mFlagParser.getArg("--ip");
        int port  = Integer.parseInt(mFlagParser.getArg("--port"));
        int iterations = Integer.parseInt(mFlagParser.getArg("--iterations"));
        String outPfx = mFlagParser.getArg("--outFilePfx");
        String osmPath = mFlagParser.getArg("--osmPath");
        String ghPath  = mFlagParser.getArg("--ghPath");
        GraphHopper hopper = new GraphHopperOSM()
                .setOSMFile(osmPath)
                .forDesktop()
                .setGraphHopperLocation(ghPath)
                .setEncodingManager(new EncodingManager("car"))
                .importOrLoad();

        try {
            mClient = new Client(
                    ip,
                    port,
                    osmPath,
                    ghPath,
                    null);
        } catch (IOException e) {
            e.printStackTrace();
            exit(1);
        }

        int numNodes = hopper.getGraphHopperStorage().getNodes();
        NodeAccess nodeAccess = hopper.getGraphHopperStorage().getNodeAccess();

        for (int i = 0; i < iterations; i++) {
            PostProcess overLapCounter = new OverLapCounter(outPfx + i + ".csv");
            mClient.setPostProcess(overLapCounter);
            int from = ThreadLocalRandom.current().nextInt(0, numNodes);
            int to = ThreadLocalRandom.current().nextInt(0, numNodes);
            if (from != to) {
                MapPoint src = new MapPoint(nodeAccess.getLat(from), nodeAccess.getLon(from));
                MapPoint dst = new MapPoint(nodeAccess.getLat(to), nodeAccess.getLon(to));
                try {
                    mClient.compute(src, dst);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void setupArgs() {
        if (mFlagParser == null) {
            mFlagParser = new FlagParser();
        }
        mFlagParser.addFlag("--ip", "The ip address of the server", "127.0.0.1");
        mFlagParser.addFlag("--port", "The port of the server", "");
        mFlagParser.addFlag("--iterations", "The number of iterations", "");
        mFlagParser.addFlag("--outFilePfx", "The prefix of generated csv file", "data/client");
        mFlagParser.addFlag("--osmPath", "The path of the osm file", "");
        mFlagParser.addFlag("--ghPath", "The location of the Graph hopper data directory", "");
    }

}
