
public class ClientMain {
    private static FlagParser mFlagParser;
    private static Client mClient;
    private final static double SRCLAT = 40.111319;
    private final static double SRCLON = -88.22794;
    private final static double DSTLAT = 41.665346;
    private final static double DSTLON = -87.761192;

    public static void main(String[] args) throws Exception{
        addFlags();


        System.out.println("Working Directory = " +
                System.getProperty("user.dir"));

        mFlagParser.parseArgs(args);
        String[] coords = mFlagParser.getUnflagged();
        String ip = mFlagParser.getArg("--ip");
        int port  = Integer.parseInt(mFlagParser.getArg("--port"));
        String osmPath = mFlagParser.getArg("--osmPath");
        String ghPath  = mFlagParser.getArg("--ghPath");
        String mapPath = mFlagParser.getArg("--mapPath");
        mClient = new Client(ip, port, osmPath, ghPath, mapPath);
        mClient.compute(new Pair<>(SRCLAT, SRCLON), new Pair<>(DSTLAT, DSTLON));
        System.out.println("Going to return");
    }

    public static void addFlags() {
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
