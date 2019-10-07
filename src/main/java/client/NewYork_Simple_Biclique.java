package client;

import com.graphhopper.routing.util.EdgeFilter;
import util.*;

import javax.swing.plaf.synth.SynthEditorPaneUI;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static java.lang.Double.doubleToLongBits;
import static java.lang.Double.min;

public class NewYork_3 {
    private static boolean check_lat(double latitude)
    {
        return latitude>=40 && latitude<=42;
    }
    private static boolean check_lon(double longitude)
    {
        return longitude>=-75 && longitude<=-72;
    }
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
        int version_number=Integer.parseInt(flagParser.getArg("--version"));
        output+="_version="+version_number;
        //for client initialization
        PostProcess postProcess = new Simple_biclique();
        System.out.print("Ok");
        Client client = new Client(ip, port, osmPath, ghPath, postProcess);
        System.out.print("Ok2");

        //file reader for trip information
        BufferedReader reader = new BufferedReader(new FileReader(tripCSV));
        HashMap<Pair<MapPoint,MapPoint>,String> distances=new HashMap<>();

        //some settings like threshold
        double threshold=Double.parseDouble(flagParser.getArg("--threshold"));
        output+="_number="+numTrips+".txt";
        output+="_threshold="+threshold+".txt";
        HashMap<Pair<MapPoint,MapPoint>,Pair<Integer,Integer>> comparision=new HashMap<>();// for comparison

        //for getting time
        String output_time_comparision="time_comparision_real.txt";
        FileOutputStream moutput_partition_time;
        moutput_partition_time=new FileOutputStream(output_time_comparision);

        String output_computation_cost="computation_cost.txt";
        FileOutputStream moutput_partition_computation_cost;
        moutput_partition_computation_cost=new FileOutputStream(output_computation_cost);

        String output_firstpoint_cost="first_point_distribution.txt";
        FileOutputStream moutput_partition_first_point;
        moutput_partition_first_point=new FileOutputStream(output_firstpoint_cost);

        //checking whether every path is reported
        int total=0;
        int reported=0;
        Boolean flag_reported=true;


        reader.readLine();  // skip the header
        int i = 0;
        for (String line = reader.readLine(); line != null && i < numTrips;
             i++, line = reader.readLine()) {
            String[] elements = line.split(",");
            double startLat = Double.parseDouble(elements[6]);
            double startLon = Double.parseDouble(elements[5]);
            double endLat = Double.parseDouble(elements[10]);
            double endLon = Double.parseDouble(elements[9]);
            Pair<MapPoint,MapPoint> source_destination=new Pair<>(new MapPoint(startLat, startLon),new MapPoint(endLat, endLon));
            /* Just to skip some dirty data */
            if(!check_lat(startLat) || !check_lat(endLat) || !check_lon(startLon) || !check_lon(endLon))
            {
                i--;
                continue;
            }
            try {
                final long startTime = System.currentTimeMillis();
                client.compute(
                        new MapPoint(startLat, startLon),
                        new MapPoint(endLat, endLon));
                final long endTime = System.currentTimeMillis();
                String rs=endTime-startTime+"\n";
                moutput_partition_computation_cost.write(rs.getBytes());

                ArrayList<MapPoint> path=((Simple_biclique) postProcess).get_mainpath();
                if(path.size()==0)continue;
                boolean flag=true;
                //for getting real_time
                String result_time="";
                result_time+=elements[1]+"->"+elements[2]+"\n";
                moutput_partition_time.write(result_time.getBytes());

                MapPoint pre=path.get(0);
                total+=1;
                flag_reported=false;
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
                        distances.put(segment,"");
                        comparision.put(segment,new Pair<>(0,1));
                    }
                    pre=cur;
                }
                HashMap<Pair<MapPoint,MapPoint>,Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>> result=((Simple_biclique) postProcess).get_result();
                for (Pair<MapPoint,MapPoint> segement:result.keySet()) {
                    double a = result.get(segement).mFirst.mFirst;
                    double b = result.get(segement).mFirst.mSecond;
                    double c = result.get(segement).mSecond.mFirst;
                    double d = result.get(segement).mSecond.mSecond;
                    if (version_number == 1) {
                        a = (a + c) / (b + d);
                    } else {
                        if (version_number == 2) {
                            a = min(a / b, c / d);
                        } else {
                            a = (a * c) / (b * d);
                        }
                    }
                    if (a >= threshold) {
                        if (flag) {
                            double fromlat = startLat;
                            double fromlon = startLon;
                            double tolat = segement.mFirst.getLat();
                            double tolon = segement.mFirst.getLon();
                            Integer from = client.mHopper.getLocationIndex()
                                    .findClosest(fromlat, fromlon, EdgeFilter.ALL_EDGES)
                                    .getClosestNode();
                            Integer to = client.mHopper.getLocationIndex().findClosest(tolat, tolon, EdgeFilter.ALL_EDGES).getClosestNode();
                            GetTime g = new GetTime(from, to, client.mEm, client.mHopper);
                            String result_time_first = g.GetTime() + "\n";
                            moutput_partition_first_point.write(result_time_first.getBytes());
                            flag = false;
                        }
                        comparision.get(segement).mFirst += 1;
                        if(!flag_reported) {
                            reported += 1;
                            flag_reported=true;
                        }
                    } else {

                    }
                }
            } catch (MainPathEmptyException e) {
                Logger.printf(Logger.ERROR, "No such path: (%f, %f), (%f, %f)\n",
                        startLat, startLon, endLat, endLon);
            }
        }

        FileOutputStream moutput;
        File outputfile = new File(output);
        moutput=new FileOutputStream(outputfile);
        moutput.write("Segment Count_1 Count_2 Distances\n".getBytes());
        for(Pair<MapPoint,MapPoint> segment:comparision.keySet())
        {
            String a=segment.mFirst.toString()+"->"+segment.mSecond.toString()+": "+comparision.get(segment).mFirst.toString()+" "+comparision.get(segment).mSecond.toString()+"\n";
            moutput.write(a.getBytes());
        }
        moutput.close();
        System.out.print(reported+"/"+total+"\n");
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
        flagParser.addFlag("--output", "Where to store the counting information", "./data/count");
        flagParser.addFlag("--threshold", "Client should choose a threshold", "0.9");
        flagParser.addFlag("--version", "Client should choose the version for computing threshold", "3");
    }

}
