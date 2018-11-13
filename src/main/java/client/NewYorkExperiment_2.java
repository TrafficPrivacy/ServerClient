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

public class NewYorkExperiment_2 {
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
        PostProcess postProcess = new Calculate_biclique();
        System.out.print("Ok");
        Client client = new Client(ip, port, osmPath, ghPath, postProcess);
        System.out.print("Ok2");

        //file reader for trip information
        BufferedReader reader = new BufferedReader(new FileReader(tripCSV));

        //for calculating distance
        DistanceCalculator cal=new DistanceCalculator();
        HashMap<Pair<MapPoint,MapPoint>,String> distances=new HashMap<>();
        //for considering coverage
        HashMap<Pair<MapPoint,MapPoint>,String> converage_study=new HashMap<>();
        //some settings like threshold
        double threshold=Double.parseDouble(flagParser.getArg("--threshold"));
        output+="_number="+numTrips+".txt";
        output+="_threshold="+threshold+".txt";
        HashMap<Pair<MapPoint,MapPoint>,Pair<Integer,Integer>> comparision=new HashMap<>();// for comparison

        //for getting time
        String output_time_comparision="time_comparision_real.txt";
        FileOutputStream moutput_partition_time;
        moutput_partition_time=new FileOutputStream(output_time_comparision);


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
                client.compute(
                        new MapPoint(startLat, startLon),
                        new MapPoint(endLat, endLon));
                ArrayList<MapPoint> path=((Calculate_biclique) postProcess).get_mainpath();
               // converage_study.put(source_destination,source_destination.mFirst.toString()+"->"+source_destination.mSecond.toString()+":");

                if(path.size()==0)continue;

                //for getting real_time
                String result_time="";
                result_time+=elements[1]+"->"+elements[2]+"\n";
                moutput_partition_time.write(result_time.getBytes());

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
                        distances.put(segment,"");
                        comparision.put(segment,new Pair<>(0,1));
                    }
                    pre=cur;
                }
                ResultOfBiclique r=((Calculate_biclique) postProcess).get_result();
                HashMap<Pair<MapPoint,MapPoint>,Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>> result=r.get_result();
                HashMap<Pair<MapPoint,MapPoint>,Pair<HashSet<Integer>,HashSet<Integer>>> result_detailed=r.get_detailed_result();

                for (int k=1;k<=r.get_sequence().size();++k)
                {
                    Pair<MapPoint,MapPoint>segement=r.get_sequence().get(k);
                    double a=result.get(segement).mFirst.mFirst;
                    double b=result.get(segement).mFirst.mSecond;
                    double c=result.get(segement).mSecond.mFirst;
                    double d=result.get(segement).mSecond.mSecond;
                    if (version_number==1)
                    {
                        a=(a+c)/(b+d);
                    }
                    else
                    {
                        if(version_number==2)
                        {
                            a=min(a/b,c/d);
                        }
                        else
                        {
                            a=(a*c)/(b*d);
                        }
                    }
                    if(a>=threshold)
                    {

                        /*
                        String tmp=converage_study.get(source_destination)+' '+segement.mFirst.toString()+"->"+segement.mSecond.toString()+"yes:";
                        tmp+="[";
                        Pair<HashSet<Integer>,HashSet<Integer>>  tmp2=result_detailed.get(segement);
                        for (Integer integer: tmp2.mFirst)
                        {
                            tmp+=integer+";";
                        }
                        tmp+="]";
                        tmp+="[";
                        for (Integer integer: tmp2.mSecond)
                        {
                            tmp+=integer+";";
                        }
                        tmp+="]";
                        converage_study.put(source_destination,tmp);
                        */
                        comparision.get(segement).mFirst+=1;
                      //  double left_distance=cal.getdistance(startLat,startLon,segement.mFirst.mFirst,segement.mFirst.mSecond,new String("K"));
                      //  double right_distance=cal.getdistance(segement.mSecond.mFirst,segement.mSecond.mSecond,endLat,endLon,new String("K"));
//                      tmp=distances.get(segement).concat(" "+String.valueOf(left_distance)+","+String.valueOf(right_distance));
 //                     distances.put(segement,tmp);
                    }
                    else
                    {
                       // String tmp=converage_study.get(source_destination)+' '+segement.mFirst.toString()+"->"+segement.mSecond.toString()+"no";
                       // converage_study.put(source_destination,tmp);
                    }
                }
            } catch (MainPathEmptyException e) {
                Logger.printf(Logger.ERROR, "No such path: (%f, %f), (%f, %f)\n",
                        startLat, startLon, endLat, endLon);
               // i--;
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


        /*
        File outputfile2=new File(output);
        moutput=new FileOutputStream("Source_Destination_Coverage_Study_version="+version_number+"_threshold="+threshold+".txt");
        for(Pair<MapPoint,MapPoint> segment:converage_study.keySet())
        {
            String a=converage_study.get(segment)+'\n';
            moutput.write(a.getBytes());
        }
        moutput.close();
        */
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
