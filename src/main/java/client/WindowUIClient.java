package client;

import client.ProcessedData.PathPart;
import com.alee.laf.WebLookAndFeel;
import com.graphhopper.GraphHopper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import util.FlagParser;
import util.MapPoint;
import util.NoSuchFlagException;
import util.Pair;

public class WindowUIClient {

  private static FlagParser mFlagParser;

  private String mServerIP;
  private int mServerPort;
  private GraphHopper mHopper;

  public static void main(String args[]) {
    WebLookAndFeel.install();

    FlagParser mFlagParser = new FlagParser();
    mFlagParser.addFlag("--ip", "The ip address of the server", "127.0.0.1");
    mFlagParser.addFlag("--port", "The port of the server", "");
    mFlagParser.addFlag("--osmPath", "The path of the osm file)", "");
    mFlagParser.addFlag("--ghPath", "The location of the graphhopper data directory", "");
    mFlagParser.addFlag("--mapPath", "The location of the .map file. Used by mapsforge", "");
    mFlagParser.parseArgs(args);

    try {
      Client client = new Client(mFlagParser.getArg("--ip"),
          Integer.parseInt(mFlagParser.getArg("--port")), mFlagParser.getArg("--osmPath"),
          mFlagParser.getArg("--ghPath"), null);

      RequestHandler requestHandler = new RequestHandler(client);
      WindowUI newUI = new WindowUI(1200, 800, mFlagParser.getArg("--mapPath"),
          "Test", requestHandler);
      newUI.run();
    } catch (NoSuchFlagException e) {
      mFlagParser.printHelp();
    } catch (IOException e) {
      System.err.println(e.toString());
    }
  }
}

class RequestHandler extends OnMapRequest {

  private Client mClient;

  public RequestHandler(Client client) {
    mClient = client;
  }

  @Override
  public ProcessedData fulfillRequest(MapPoint source, MapPoint destination) {
    OverLapPathCounter overLapPathCounter = new OverLapPathCounter();
    mClient.setPostProcess(overLapPathCounter);
    try {
      mClient.compute(source, destination);
    } catch (Exception e) {
      return null;
    }
    return overLapPathCounter.getPathParts();
  }

}

class OverLapPathCounter implements PostProcess {

  // A hash map from segments to the index of the disguise paths. The values will be in increasing
  // order
  private LinkedHashMap<Pair<MapPoint, MapPoint>, ArrayList<Integer>> mMainPathSegments;
  private ArrayList<Pair<MapPoint, MapPoint>> mDisguisePathStartEnds;
  private ProcessedData mProcessedData;

  public OverLapPathCounter() {
    mMainPathSegments = new LinkedHashMap<>();
    mDisguisePathStartEnds = new ArrayList<>();
    mProcessedData = new ProcessedData();
  }

  @Override
  public void setMainPath(ArrayList<MapPoint> path) {
    for (int i = 1; i < path.size(); ++i) {
      mMainPathSegments.put(new Pair<>(path.get(i - 1), path.get(i)), new ArrayList<>());
    }
  }

  @Override
  public void addPath(ArrayList<MapPoint> path) {
    if (path.size() < 2) {
      return;
    }
    int currentNumOfPaths = mDisguisePathStartEnds.size();
    mDisguisePathStartEnds.add(new Pair<>(path.get(0), path.get(path.size() - 1)));
    for (int i = 1; i < path.size(); ++i) {
      Pair segment = new Pair<>(path.get(i - 1), path.get(i));
      if (mMainPathSegments.containsKey(segment)) {
        mMainPathSegments.get(segment).add(currentNumOfPaths);
      }
    }
  }

  @Override
  public void done() {
    ArrayList<Entry<Pair<MapPoint, MapPoint>, ArrayList<Integer>>> entries = new ArrayList<>(
        mMainPathSegments.entrySet());
    mProcessedData.mNumSegments = mMainPathSegments.size();
    for (int i = 0; i < entries.size(); ++i) {
      PathPart pathPart = mProcessedData.new PathPart();
      pathPart.mPathPoints.add(entries.get(i).getKey().mFirst);
      pathPart.mPathPoints.add(entries.get(i).getKey().mSecond);
      int j;
      // Make sure the merged segments have same paths overlap on.
      for (j = i + 1;
          j < entries.size() && sameOverLap(entries.get(i).getValue(), entries.get(j).getValue());
          ++j) {
        pathPart.mPathPoints.add(entries.get(j).getKey().mSecond);
      }
      for (int idx : entries.get(i).getValue()) {
        pathPart.mSourcePoints.add(mDisguisePathStartEnds.get(i).mFirst);
        pathPart.mDestPoints.add(mDisguisePathStartEnds.get(i).mSecond);
      }
      i = j;
      mProcessedData.mPathParts.add(pathPart);
    }
  }

  private boolean sameOverLap(ArrayList<Integer> first, ArrayList<Integer> second) {
    if (first.size() != second.size()) {
      return false;
    }
    for (int i = 0; i < first.size(); ++i) {
      if (first.get(i) != second.get(i)) {
        return false;
      }
    }
    return true;
  }

  public ProcessedData getPathParts() {
    return mProcessedData;
  }
}
