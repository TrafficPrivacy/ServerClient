package server;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.NodeAccess;
import org.json.JSONArray;
import org.json.JSONObject;
import util.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * The server is responsible to compute three matrices: 1. The all to border shortest path in the
 * start region 2. The all to border shortest path in the destination region 3. The shortest path
 * between any pair of border points (start region, destination region)
 */
public class Server {

  private ServerSocket mServer;
  private MatrixComputer mMatrixComputer;
  private FileOutputStream moutput_parition;
  private FileOutputStream moutput_partition_two;
  private final double RADIUS = 1000.0;
  private final double LATRANGE = 0.005;
  private final double LONRANGE = 0.007;
  private int number=0;

  public Server(int port, String osmPath, String osmFolder, String strategy) throws IOException {
    mServer = new ServerSocket(port);
    mMatrixComputer = new MatrixComputer(osmPath, osmFolder, strategy);
    moutput_parition= new FileOutputStream("group_size_for_square_partition.txt");
    moutput_partition_two= new FileOutputStream("poi_size_for_square_partition.txt");
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
    Pair<int[], int[]> srcRegion;
    Pair<int[], int[]> dstRegion;
    try {
      RegionCheck srcRegionCheck = new RegionCheck(LATRANGE, LONRANGE, src);
      RegionCheck dstRegionCheck = new RegionCheck(LATRANGE, LONRANGE, dst);
      srcRegion = mMatrixComputer.getPrivacyRegion(src, srcRegionCheck);
      dstRegion = mMatrixComputer.getPrivacyRegion(dst, dstRegionCheck);
      if(srcRegionCheck.isInRegion(0,dst) || dstRegionCheck.isInRegion(0,src))
      {
        number+=1;
        System.out.print("missed segment: "+number+"\n");
        return new Reply(null, null, null, null, null, Reply.ERROR);
      }
    } catch (PointNotFoundException e) {
      e.printStackTrace();
      return new Reply(null, null, null, null, null, Reply.ERROR);
    }

    int endCenter = mMatrixComputer.getmSurroundings().lookupNearest(dst.getLat(), dst.getLon());
    /*TODO: refactor this*/




    Paths srcPaths = mMatrixComputer
        .set2Set(srcRegion.mFirst, srcRegion.mSecond, false, endCenter, RADIUS);
    Paths dstPaths = mMatrixComputer
        .set2Set(dstRegion.mSecond, dstRegion.mFirst, false, endCenter, RADIUS);
    Paths interPaths = mMatrixComputer
        .set2Set(srcRegion.mSecond, dstRegion.mSecond, true, endCenter, RADIUS);

    String tmp=srcRegion.mFirst.length+"\n";
    moutput_parition.write(tmp.getBytes());
    tmp=dstRegion.mFirst.length+"\n";
    moutput_parition.write(tmp.getBytes());

   // tmp=mMatrixComputer.POI_count(srcRegion.mFirst,mMatrixComputer.nodeAccess_for_out_usage)+"\n";
   // moutput_partition_two.write(tmp.getBytes());


    return new Reply(srcRegion, dstRegion, srcPaths, dstPaths, interPaths, Reply.OK);
  }

  class RegionCheck implements InRegionTest {

    private double mLeftBorder;
    private double mRightBorder;
    private double mUpperBorder;
    private double mLowerBorder;

    public RegionCheck(double gridLatRange, double gridLonRange, MapPoint center) {
      int adjustedLatRange = (int) Math.floor(gridLatRange * 1000);
      int adjustedLonRange = (int) Math.floor(gridLonRange * 1000);
      double lon = center.getLon() * 1000;
      double lat = center.getLat() * 1000;
      mLeftBorder = floor(adjustedLonRange, lon) / 1000.0;
      mRightBorder = ceil(adjustedLonRange, lon) / 1000.0;
      mLowerBorder = floor(adjustedLatRange, lat) / 1000.0;
      mUpperBorder = ceil(adjustedLatRange, lat) / 1000.0;
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
