package server;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.shapes.GHPoint;
import me.tongfei.progressbar.ProgressBar;
import util.FlagParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.System.exit;

public class ServerProfiler {
    private static GraphHopper mHopper;
    private static FlagParser mFlagParser;
    private static Server mServer;

    public static void main(String[] args) {
        setupArgs();
        mFlagParser.parseArgs(args);
        mHopper = new GraphHopperOSM()
                .setOSMFile(mFlagParser.getArg("--osmPath"))
                .forDesktop()
                .setGraphHopperLocation(mFlagParser.getArg("--ghPath"))
                .setEncodingManager(new EncodingManager("car"))
                .importOrLoad();
        try {
            mServer = new Server(0,
                    mFlagParser.getArg("--osmPath"),
                    mFlagParser.getArg("--ghPath"),
                    mFlagParser.getArg("--strategy"));
        } catch (IOException e) {
            e.printStackTrace();
            exit(1);
        }
        run(Integer.parseInt(mFlagParser.getArg("--iterations")),
                mFlagParser.getArg("--outFilePath"));
    }

    private static void setupArgs() {
        if (mFlagParser == null) {
            mFlagParser = new FlagParser();
        }
        mFlagParser.addFlag("--osmPath", "The path of the osm file", "");
        mFlagParser.addFlag("--ghPath", "The location of the graphhopper data directory", "");
        mFlagParser.addFlag("--iterations", "The number of iterations", "");
        mFlagParser.addFlag("--outFilePath", "The generated csv file", "data/server.csv");
        mFlagParser.addFlag("--strategy", "The strategy to use", "");
    }

    private static void run(int numIterations, String outPath) {
        File file = new File(outPath);
        NodeAccess nodeAccess = mHopper.getGraphHopperStorage().getNodeAccess();
        try (FileOutputStream fout = new FileOutputStream(file)) {
            fout.write("from lat, from lon, to lat, to lon, distance, time(ns)\n".getBytes());
            Method method = mServer.getClass().getDeclaredMethod("calculate",
                    double.class, double.class, double.class, double.class);
            method.setAccessible(true);
            int numNodes = mHopper.getGraphHopperStorage().getNodes();
            ProgressBar progressBar = new ProgressBar("Server Profile Test", numIterations);
            progressBar.start();
            for (int i = 0; i < numIterations; i++) {
                int from = ThreadLocalRandom.current().nextInt(0, numNodes);
                int to = ThreadLocalRandom.current().nextInt(0, numNodes);
                if (from != to) {
                    double fromLat = nodeAccess.getLat(from);
                    double fromLon = nodeAccess.getLon(from);
                    double toLat = nodeAccess.getLat(to);
                    double toLon = nodeAccess.getLon(to);

                    GHRequest request = new GHRequest(
                            new GHPoint(fromLat, fromLon),
                            new GHPoint(toLat, toLon)
                    );
                    GHResponse response = mHopper.route(request);
                    if (response.hasErrors()) {
                        i --;
                        continue;
                    }

                    long startTime = System.nanoTime();
                    method.invoke(mServer, fromLat, fromLon, toLat, toLon);
                    long runTime = System.nanoTime() - startTime;

                    fout.write((fromLat + ",").getBytes());
                    fout.write((fromLon + ",").getBytes());
                    fout.write((toLat + ",").getBytes());
                    fout.write((toLon + ",").getBytes());
                    fout.write((response.getBest().getDistance() + ",").getBytes());
                    fout.write((runTime + "\n").getBytes());
                    progressBar.step();
                } else {
                    i--;
                }
            }
            progressBar.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
