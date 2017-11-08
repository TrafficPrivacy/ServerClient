package client;

import util.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class NewYorkExperiment {

    public static void main(String args[]) throws
            IOException,
            NoSuchFlagException,
            NoSuchStrategyException,
            NoEdgeIteratorException,
            ClassNotFoundException,
            MainPathEmptyException {
        FlagParser flagParser = new FlagParser();
        addFlags(flagParser);
        flagParser.parseArgs(args);
        String ip = flagParser.getArg("--ip");
        int port  = Integer.parseInt(flagParser.getArg("--port"));
        String osmPath = flagParser.getArg("--osmPath");
        String ghPath = flagParser.getArg("--ghPath");
        String tripCSV = flagParser.getArg("--tripCSV");
        int numTrips = Integer.parseInt(flagParser.getArg("--numTrips"));
        String outPathCSV = flagParser.getArg("--outPathCSV");
        String outSegmentCSV = flagParser.getArg("--outSegmentCSV");

        NewYorkExpmPostProcess postProcess = new NewYorkExpmPostProcess(outPathCSV, outSegmentCSV);
        Client client = new Client(ip, port, osmPath, ghPath, postProcess);

        BufferedReader reader = new BufferedReader(new FileReader(tripCSV));
        reader.readLine();  // skip the header
        int i = 0;

        for (String line = reader.readLine(); line != null && i < numTrips; i++, line = reader.readLine()) {
            String[] elements = line.split(",");
            double startLat = Double.parseDouble(elements[6]);
            double startLon = Double.parseDouble(elements[5]);
            double endLat = Double.parseDouble(elements[10]);
            double endLon = Double.parseDouble(elements[9]);
            /* Just to skip some dirty data */
            if (startLat < 20 || startLon > -20 || endLat < 20 || endLon > -20) {
                i--;
                continue;
            }
            try {
                client.compute(
                        new MapPoint(startLat, startLon),
                        new MapPoint(endLat, endLon));
            } catch (MainPathEmptyException e) {
                Logger.printf(Logger.ERROR, "No such path: (%f, %f), (%f, %f)\n",
                        startLat, startLon, endLat, endLon);
                i--;
            }
        }

        postProcess.finishUp();
    }

    private static void addFlags(FlagParser flagParser) {
        if (flagParser == null) {
            flagParser = new FlagParser();
        }
        flagParser.addFlag("--ip", "The ip address of the server", "127.0.0.1");
        flagParser.addFlag("--port", "The port of the server", "");
        flagParser.addFlag("--osmPath", "The path of the osm file)", "");
        flagParser.addFlag("--ghPath", "The location of the graph hopper data directory", "");
        flagParser.addFlag("--tripCSV", "The location of the trip data", "");
        flagParser.addFlag("--numTrips", "Run experiment on how many trips", "100");
        flagParser.addFlag("--outPathCSV", "The CSV contains path overlap info", "data/path.csv");
        flagParser.addFlag("--outSegmentCSV", "The segment overlap count", "data/segment.csv");
    }

}
