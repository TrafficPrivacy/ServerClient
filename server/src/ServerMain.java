public class ServerMain {
    private static FlagParser mFlagParser;

    public static void main(String[] args) throws Exception{
        addFlags();
        mFlagParser.parseArgs(args);
        int port  = Integer.parseInt(mFlagParser.getArg("--port"));
        String osmPath = mFlagParser.getArg("--osmPath");
        String ghPath  = mFlagParser.getArg("--ghPath");

        Server server = new Server(port, osmPath, ghPath, S2SStrategy.ASTAR);
        server.run();
    }

    public static void addFlags() {
        if (mFlagParser == null) {
            mFlagParser = new FlagParser();
        }
        mFlagParser.addFlag("--port", "The port of the server", "");
        mFlagParser.addFlag("--osmPath", "The path of the osm file)", "");
        mFlagParser.addFlag("--ghPath", "The location of the graphhopper data directory", "");
    }
}