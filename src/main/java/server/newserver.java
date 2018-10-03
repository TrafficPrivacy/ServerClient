package server;

import util.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class newserver {

    private ServerSocket mServer;
    private MatrixComputer mMatrixComputer;

    private final double RADIUS = 1000.0;

    public newserver(int port, String osmPath, String osmFolder, String strategy) throws IOException {
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
        ObjectInputStream oIn = new ObjectInputStream(in);

        Pair<MapPoint,MapPoint> source_region=(Pair<MapPoint,MapPoint>)oIn.readObject();
        Pair<MapPoint,MapPoint> destination_region=(Pair<MapPoint,MapPoint>)oIn.readObject();

        double srcLat = in.readDouble();
        double srcLon = in.readDouble();
        double dstLat = in.readDouble();
        double dstLon = in.readDouble();

        Logger.printf(Logger.INFO, "from: (%.05f, %.05f), to: (%.05f, %.05f)\n",
                srcLat, srcLon, dstLat, dstLon);

        Logger.printf(Logger.DEBUG, "Handle one\n");

        Profiler profiler = new Profiler().start();
        Reply reply = calculate(source_region,destination_region);
        profiler.endAndPrint();
        ObjectOutputStream oOut = new ObjectOutputStream(out);
        oOut.writeObject(reply);
        oOut.close();
        out.close();
    }

    private Reply calculate(Pair<MapPoint,MapPoint> source_region,Pair<MapPoint,MapPoint> destination_region) {
        Pair<int[], int[]> srcRegion;
        Pair<int[], int[]> dstRegion;
        MapPoint src_center=new MapPoint((source_region.mFirst.mFirst+source_region.mSecond.mFirst)/2.,(source_region.mFirst.mSecond+source_region.mSecond.mSecond)/2.);
        MapPoint des_center=new MapPoint((destination_region.mFirst.mFirst+destination_region.mSecond.mFirst)/2.,(destination_region.mFirst.mSecond+destination_region.mSecond.mSecond)/2.);
        try {
            newserver.RegionCheck srcRegionCheck = new newserver.RegionCheck(source_region);
            newserver.RegionCheck dstRegionCheck = new newserver.RegionCheck(source_region);
            srcRegion = mMatrixComputer.getPrivacyRegion(src_center, srcRegionCheck);
            dstRegion = mMatrixComputer.getPrivacyRegion(des_center, dstRegionCheck);
        } catch (PointNotFoundException e) {
            e.printStackTrace();
            return new Reply(null, null, null, null, null, Reply.ERROR);
        }
        int endCenter = mMatrixComputer.getmSurroundings().lookupNearest(des_center.getLat(), des_center.getLon());
        /*TODO: refactor this*/
        Paths srcPaths = mMatrixComputer
                .set2Set(srcRegion.mFirst, srcRegion.mSecond, false, endCenter, RADIUS);
        Paths dstPaths = mMatrixComputer
                .set2Set(dstRegion.mSecond, dstRegion.mFirst, false, endCenter, RADIUS);
        Paths interPaths = mMatrixComputer
                .set2Set(srcRegion.mSecond, dstRegion.mSecond, true, endCenter, RADIUS);

        return new Reply(srcRegion, dstRegion, srcPaths, dstPaths, interPaths, Reply.OK);
    }

    class RegionCheck implements InRegionTest {

        private MapPoint leftdown;
        private MapPoint rightup;

        public RegionCheck(Pair<MapPoint,MapPoint> region) {
            leftdown=new MapPoint(region.mFirst.mFirst,region.mFirst.mSecond);
            rightup=new MapPoint(region.mSecond.mFirst,region.mSecond.mSecond);
        }

        @Override
        public boolean isInRegion(double distance, MapPoint current) {
            double lat = current.getLat();
            double lon = current.getLon();

            return lat <= rightup.mFirst && lat >= leftdown.mFirst
                    && lon <= rightup.mSecond && lon >= leftdown.mSecond;
        }
    }
}
