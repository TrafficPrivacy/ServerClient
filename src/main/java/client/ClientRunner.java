package client;

import util.*;

import java.io.IOException;

public class ClientRunner {

  private static FlagParser mFlagParser;

  public static void main(String[] args) throws
      IOException,
      NoSuchFlagException,
      NoSuchStrategyException,
      NoEdgeIteratorException,
      ClassNotFoundException,
      MainPathEmptyException {
    addFlags();
    mFlagParser.parseArgs(args);

    String[] coords = mFlagParser.getUnflagged();
    String ip = mFlagParser.getArg("--ip");
    int port = Integer.parseInt(mFlagParser.getArg("--port"));
    String osmPath = mFlagParser.getArg("--osmPath");
    String ghPath = mFlagParser.getArg("--ghPath");
    String mapPath = mFlagParser.getArg("--mapPath");
    PostProcess mapUI = new MapUI(mapPath, "Client");
    Client client = new Client(ip, port, osmPath, ghPath, mapUI);
    client.compute(
        new MapPoint(Double.parseDouble(coords[0]), Double.parseDouble(coords[1])),
        new MapPoint(Double.parseDouble(coords[2]), Double.parseDouble(coords[3])));
  }

  private static void addFlags() {
    if (mFlagParser == null) {
      mFlagParser = new FlagParser();
    }
    mFlagParser.addFlag("--ip", "The ip address of the server", "127.0.0.1");
    mFlagParser.addFlag("--port", "The port of the server", "");
    mFlagParser.addFlag("--osmPath", "The path of the osm file)", "");
    mFlagParser.addFlag("--ghPath", "The location of the graphhopper data directory", "");
    mFlagParser.addFlag("--mapPath", "The location of the .map file. Used by mapsforge", "");
    mFlagParser.addUnflagged("srclat", "the latitude of the source point");
    mFlagParser.addUnflagged("srclon", "the longitude of the source point");
    mFlagParser.addUnflagged("dstlat", "the latitude of the destination point");
    mFlagParser.addUnflagged("dstlon", "the longitude of the destination point");
  }
}
