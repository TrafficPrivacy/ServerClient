package server;

import util.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

/**
 * The server is responsible to compute three matrices: 1. The all to border shortest path in the
 * start region 2. The all to border shortest path in the destination region 3. The shortest path
 * between any pair of border points (start region, destination region)
 */
public class newserver {

    private ServerSocket mServer;
    private ArrayList<Pair<int[],int[]>> allRegion;
    private ArrayList<HashSet<Integer>> allRegionSet;
    private MatrixComputer mMatrixComputer;
    private FileOutputStream moutput_parition;

    private final double RADIUS = 1000.0;
    private final double LATRANGE = 2.0;
    private final double LONRANGE = 2.0;

    public newserver(int port, String osmPath, String osmFolder, String strategy) throws IOException {
        mServer = new ServerSocket(port);
        mMatrixComputer = new MatrixComputer(osmPath, osmFolder, strategy);
    }
    public void run() throws IOException {
        MapPoint cankao=new MapPoint(40.734955, -73.921738);
        RegionCheck regioncheck = new RegionCheck(LATRANGE, LONRANGE, cankao);
        allRegion = mMatrixComputer.getAllRegions(cankao, regioncheck);
        allRegionSet=new ArrayList<>();
        for(int i=0;i<allRegion.size();++i)
        {
            allRegionSet.add(new HashSet<>());
            int [] allpoints=allRegion.get(i).mFirst;
            for(int j=0;j<allpoints.length;++j)
            {
                allRegionSet.get(i).add(allpoints[j]);
            }
        }
        //output partition
        String output_partition="Partition_Size_Distribution.txt";
        moutput_parition=new FileOutputStream(output_partition);
        for(int i=0;i<allRegion.size();++i)
        {
            String tmp=allRegion.get(i).mFirst.length+" "+allRegion.get(i).mSecond.length+"\n";
            moutput_parition.write(tmp.getBytes());
        }
        moutput_parition.flush();
        moutput_parition.close();
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

        Logger.printf(Logger.DEBUG, "Handle one\n");

        Profiler profiler = new Profiler().start();
        Reply reply = calculate(new MapPoint(srcLat, srcLon), new MapPoint(dstLat, dstLon));
        profiler.endAndPrint();
        ObjectOutputStream oOut = new ObjectOutputStream(out);
        oOut.writeObject(reply);
        oOut.close();
        out.close();
    }
    private Reply calculate(MapPoint src, MapPoint dst) throws IOException {
        int i=mMatrixComputer.getspecific_region(src,allRegionSet);
        int j=mMatrixComputer.getspecific_region(dst,allRegionSet);
        if (i==-1 || j==-1) {
            System.out.print("Error for getting regions: "+src+"->"+dst+"\n");
            return new Reply(null, null, null, null, null, Reply.ERROR);
        }
      //  System.out.print(i+" "+j+"\n");
        Pair<int[],int[]>srcRegion = allRegion.get(i);
        Pair<int[],int[]>dstRegion = allRegion.get(j);
     //   System.out.print(srcRegion.mFirst.length+" "+srcRegion.mSecond.length+"\n");
     //   System.out.print(dstRegion.mFirst.length+" "+dstRegion.mSecond.length+"\n");
        int endCenter = mMatrixComputer.getmSurroundings().lookupNearest(dst.getLat(), dst.getLon());
        /*TODO: refactor this*/
        //System.out.print("src->src"+"\n");
        Paths srcPaths = mMatrixComputer
                .set2Set(srcRegion.mFirst, srcRegion.mSecond, false, endCenter, RADIUS);
       // System.out.print("des->des"+"\n");
        Paths dstPaths = mMatrixComputer
                .set2Set(dstRegion.mSecond, dstRegion.mFirst, false, endCenter, RADIUS);
      //  System.out.print("src->des"+"\n");
        Paths interPaths = mMatrixComputer
                .set2Set(srcRegion.mSecond, dstRegion.mSecond, true, endCenter, RADIUS);
       // System.out.print("Paths");
        return new Reply(srcRegion, dstRegion, srcPaths, dstPaths, interPaths, Reply.OK);
    }

    class RegionCheck implements InRegionTest {

        private double mLeftBorder;
        private double mRightBorder;
        private double mUpperBorder;
        private double mLowerBorder;

        public RegionCheck(double gridLatRange, double gridLonRange, MapPoint center) {
            /*
            int adjustedLatRange = (int) Math.floor(gridLatRange * 1000);
            int adjustedLonRange = (int) Math.floor(gridLonRange * 1000);
            double lon = center.getLon() * 1000;
            double lat = center.getLat() * 1000;
            mLeftBorder = floor(adjustedLonRange, lon) / 1000.0;
            mRightBorder = ceil(adjustedLonRange, lon) / 1000.0;
            mLowerBorder = floor(adjustedLatRange, lat) / 1000.0;
            mUpperBorder = ceil(adjustedLatRange, lat) / 1000.0;
            */
            double lon = center.getLon() ;
            double lat = center.getLat() ;
            mLeftBorder = lon-gridLonRange;
            mRightBorder = lon+gridLonRange;
            mLowerBorder = lat-gridLatRange;
            mUpperBorder = lat+gridLatRange;
            Logger.printf(Logger.DEBUG, "left border: %f, right border: %f\n", mLeftBorder, mRightBorder);
            Logger
                    .printf(Logger.DEBUG, "lower border: %f, upper border: %f\n", mLowerBorder, mUpperBorder);
        }

        private int floor(int unit, double val) {
            int numUnits = (int) Math.floor(val / unit);
            return numUnits * unit;
        }

        private int ceil(int unit, double val) {
            int numUnits = (int) Math.ceil(val / unit);
            return numUnits * unit;
        }

        @Override
        public boolean isInRegion(double distance, MapPoint current) {
            double lat = current.getLat();
            double lon = current.getLon();

            return lat <= mUpperBorder && lat >= mLowerBorder
                    && lon <= mRightBorder && lon >= mLeftBorder;
        }
    }
}
