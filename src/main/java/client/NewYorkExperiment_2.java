package client;

import util.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Double.min;

public class NewYorkExperiment_2 {

    public static void main(String args[]) throws
            IOException,
            NoSuchFlagException,
            NoSuchStrategyException,
            NoEdgeIteratorException,
            ClassNotFoundException
            {
        FlagParser flagParser = new FlagParser();
        addFlags(flagParser);
        flagParser.parseArgs(args);
        String ip = flagParser.getArg("--ip");
        int port = Integer.parseInt(flagParser.getArg("--port"));
        String osmPath = flagParser.getArg("--osmPath");
        String ghPath = flagParser.getArg("--ghPath");
        String tripCSV = flagParser.getArg("--tripCSV");
        int numTrips = Integer.parseInt(flagParser.getArg("--numTrips"));
        String output = flagParser.getArg("--output");

        //for client initialization
        PostProcess postProcess = new Calculate_biclique();
        System.out.print("Ok");
        Client client = new Client(ip, port, osmPath, ghPath, postProcess);
        System.out.print("Ok2");

        //file reader for trip information
        BufferedReader reader = new BufferedReader(new FileReader(tripCSV));

        //some settings like threshold
        double threshold=0.9;//threshold
        HashMap<Pair<MapPoint,MapPoint>,Pair<Integer,Integer>> comparision=new HashMap<>();// for comparison

        reader.readLine();  // skip the header
        int i = 0;
        for (String line = reader.readLine(); line != null && i < numTrips;
             i++, line = reader.readLine()) {
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
                ArrayList<MapPoint> path=((Calculate_biclique) postProcess).get_mainpath();
                if(path.size()==0)continue;
                MapPoint pre=path.get(0);
                for(int j=1;j<path.size();++j)
                {
                    MapPoint cur=path.get(j);
                    Pair<MapPoint,MapPoint> segment=new Pair<>(pre,cur);
                    if(comparision.containsKey(segment))
                    {
                        comparision.get(segment).mSecond+=1;
                    }
                    else
                    {
                        comparision.put(segment,new Pair<>(0,1));
                    }
                    pre=cur;
                }
                HashMap<Pair<MapPoint,MapPoint>,Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>> result=((Calculate_biclique) postProcess).get_result();
                for (Pair<MapPoint,MapPoint> segement: result.keySet())
                {

                    double a=result.get(segement).mFirst.mFirst+result.get(segement).mFirst.mSecond;
                    double b=result.get(segement).mSecond.mFirst+result.get(segement).mSecond.mSecond;
                    a=min(a,b);
                    if(a>=threshold)
                    {
                        comparision.get(segement).mFirst+=1;
                    }
                }

            } catch (MainPathEmptyException e) {
                Logger.printf(Logger.ERROR, "No such path: (%f, %f), (%f, %f)\n",
                        startLat, startLon, endLat, endLon);
                i--;
            }
        }

        FileOutputStream moutput;
        File outputfile = new File(output);
        moutput=new FileOutputStream(outputfile);
        moutput.write("Segment count_1 count_2\n".getBytes());
        for(Pair<MapPoint,MapPoint> segment:comparision.keySet())
        {
            String a=segment.mFirst.toString()+"->"+segment.mSecond.toString()+": "+comparision.get(segment).mFirst.toString()+" "+comparision.get(segment).mSecond.toString()+"\n";
            moutput.write(a.getBytes());
        }

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
        flagParser.addFlag("--output", "Where to store the counting information", "./data/count.txt");
    }

}
