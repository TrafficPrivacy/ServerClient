package server;

import algorithm.*;
import client.Visualizer;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DistanceCalcEarth;
import com.sun.org.apache.xpath.internal.operations.Bool;
import util.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import static java.lang.Integer.min;

public class MatrixComputer {

  private String mStrategy;
  private EncodingManager mEm;
  private GraphHopper mHopper;
  private Surroundings mSurroundings;
  //this is for partitioning the graph
  private int [] allpoints;
  private HashMap<Integer,Boolean> border;
  private HashMap<Integer,Boolean> visited;
  private HashMap<Integer,Integer> group;
  private HashMap<Integer,Integer>group_size;
  private HashMap<Integer,ArrayList<Pair<Integer, Double>>> edges;
  private Visualizer visualizer;
  private Integer min_size ;
  private Integer max_size ;
  // the above is for partitioning the graph

  public MatrixComputer(String osmPath, String osmFolder, String strategy) {
    /*TODO: change this hard coded encoding*/
    min_size = new Integer(10);
    max_size = new Integer(40);
    mEm = new EncodingManager("car");
    mHopper = new GraphHopperOSM()
        .setOSMFile(osmPath)
        .forDesktop()
        .setGraphHopperLocation(osmFolder)
        .setEncodingManager(mEm)
        .importOrLoad();

    mStrategy = strategy;
    mSurroundings = new Surroundings(mHopper.getGraphHopperStorage(),
        mHopper.getLocationIndex(), mEm.getEncoder("car"));
    visualizer = new Visualizer("data/new-york.map", "Test",
            mHopper.getGraphHopperStorage().getNodeAccess());
  }

  public Surroundings getmSurroundings() {
    return mSurroundings;
  }

  /**
   * Find all the points inside a circle of certain radius
   *
   * @param center The center point
   * @param inRegionTest The in region check call back
   * @return A pair of integer arrays. The first in the pair represents all the points in the
   * circle. The second represents the border points.
   */
  private ArrayList<Integer> FindNeighbors(Integer cur,double distance) {
    visited.put(cur, true);
    ArrayList<Integer> result = new ArrayList<>();
    LinkedList<Integer> q;
    q = new LinkedList<>();
    q.add(cur);
    while (!q.isEmpty() && result.size() <= max_size) {
      Integer current_point = q.getFirst();
      q.pop();
      result.add(current_point);
      ArrayList<Pair<Integer, Double>> edge = edges.get(current_point);
      boolean flag = false;
      for (int i = 0; i < edge.size(); ++i) {
        Pair<Integer, Double> tmp = edge.get(i);
       // if (!visited.get(tmp.mFirst) && tmp.mSecond <= distance && result.size() <= max_size) {
          if (!visited.get(tmp.mFirst)  && result.size()<=max_size) {
          flag = true;
          visited.put(tmp.mFirst, true);
          q.add(tmp.mFirst);
          //  ArrayList<Integer> sub_result = FindNeighbors(tmp.mFirst, distance);
          //  result.addAll(sub_result);
        }
      }
      if (!flag) border.put(current_point, true);
    }

    Integer presize=-1;
    //System.out.print(result.size()+" ");
    while (result.size() < min_size)
    {
        presize=result.size();
        for(int j=0;j<presize;++j) {
          if (border.get(result.get(j))) {
            ArrayList<Pair<Integer, Double>> edge = edges.get(result.get(j));
            for (int k = 0; k < edge.size(); ++k) {
              if (group.containsKey(edge.get(k).mFirst)) {
                Integer index = group.get(edge.get(k).mFirst);
                if (group_size.get(index) > min_size) ;
                else continue;
                result.add(edge.get(k).mFirst);
                border.put(result.get(j), false);
                border.put(edge.get(k).mFirst, true);
                group.remove(edge.get(k).mFirst);
                group_size.put(index, group_size.get(index) - 1);
              } else {
                if (!visited.get(edge.get(k).mFirst)) {
                  result.add(edge.get(k).mFirst);
                  border.put(result.get(j), false);
                  border.put(edge.get(k).mFirst, true);
                  visited.put(edge.get(k).mFirst, true);
                }
              }
            }
          }
        }
        if(presize==result.size())break;
    }
   // System.out.print(result.size()+"\n");
    return result;
  }
  public ArrayList<Pair<int[],int[]>> extension_Kernighan_Lin(ArrayList<Pair<int[],int[]>> candidate)
  {
    NodeAccess nodeAccess = mHopper.getGraphHopperStorage().getNodeAccess();
    DistanceCalcEarth distanceCalcEarth = new DistanceCalcEarth();
    System.out.print(candidate.size()+"\n");
    for(int i=0;i<candidate.size();++i)
    {
     // if(i%5==0)System.out.print(i+"\n");
      for(int j=i+1;j<min(candidate.size(),i+100);++j)
      {
        Pair<int[],int[]>RegionOne=candidate.get(i);
        Pair<int[],int[]>RegionTwo=candidate.get(j);
        double fromLat = nodeAccess.getLat(RegionOne.mFirst[0]);
        double fromLon = nodeAccess.getLon(RegionOne.mFirst[0]);
        double toLat = nodeAccess.getLat(RegionTwo.mFirst[0]);
        double toLon = nodeAccess.getLon(RegionTwo.mFirst[0]);
        if(distanceCalcEarth.calcDist(fromLat, fromLon, toLat, toLon)>=1000)
        {
          continue;
        }
        else {

          // the following is for algorithm
          Integer iteration=10;
          while (iteration>0) {
            --iteration;
            // the following is for preprocessing
            HashMap<Pair<Integer,Integer>,Boolean>connected=new HashMap<>();
            HashMap<Integer, Integer> D = new HashMap<>();
            HashMap<Integer, Boolean> point_one = new HashMap<Integer, Boolean>();
            HashMap<Integer, Boolean> point_two = new HashMap<Integer, Boolean>();
            HashMap<Integer, Boolean> visited= new HashMap<>();

            for (int k = 0; k < RegionOne.mFirst.length; ++k) {
              point_one.put(RegionOne.mFirst[k], true);
              D.put(RegionOne.mFirst[k], 0);
            }
            for (int k = 0; k < RegionTwo.mFirst.length; ++k) {
              point_two.put(RegionTwo.mFirst[k], true);
              D.put(RegionTwo.mFirst[k], 0);
            }


            for (int k = 0; k < RegionOne.mFirst.length; ++k) {
              ArrayList<Pair<Integer, Double>> edge = edges.get(RegionOne.mFirst[k]);
              for (int l = 0; l < edge.size(); ++l) {
                if (point_two.containsKey(edge.get(l).mFirst)) {
                  Pair<Integer, Integer> key = new Pair<>(RegionOne.mFirst[k], edge.get(l).mFirst);
                  connected.put(key, true);
                }
              }
            }
            for (int k = 0; k < RegionTwo.mFirst.length; ++k) {
              ArrayList<Pair<Integer, Double>> edge = edges.get(RegionTwo.mFirst[k]);
              for (int l = 0; l < edge.size(); ++l) {
                if (point_one.containsKey(edge.get(l).mFirst)) {
                  Pair<Integer, Integer> key = new Pair<>(RegionTwo.mFirst[k], edge.get(l).mFirst);
                  connected.put(key, true);
                }
              }
            }

            ArrayList<Integer> left_points = new ArrayList<>();
            ArrayList<Integer> right_points = new ArrayList<>();
            ArrayList<Integer> g_lists = new ArrayList<>();
            for (int t = 0; t < min(RegionOne.mFirst.length, RegionTwo.mFirst.length); ++t) {

              for (int k = 0; k < RegionOne.mFirst.length; ++k) {
                point_one.put(RegionOne.mFirst[k], true);
                D.put(RegionOne.mFirst[k], 0);
              }
              for (int k = 0; k < RegionTwo.mFirst.length; ++k) {
                point_two.put(RegionTwo.mFirst[k], true);
                D.put(RegionTwo.mFirst[k], 0);
              }
              //get D for region_one
              for (int k = 0; k < RegionOne.mFirst.length; ++k) {
                if(visited.containsKey(RegionOne.mFirst[k]))continue;
                ArrayList<Pair<Integer, Double>> edge = edges.get(RegionOne.mFirst[k]);
                for (int l = 0; l < edge.size(); ++l) {
                  if(!visited.containsKey(edge.get(l).mFirst) && point_one.containsKey(edge.get(l).mFirst) )D.put(RegionOne.mFirst[k], D.get(RegionOne.mFirst[k]) - 1);
                  if(!visited.containsKey(edge.get(l).mFirst) && point_two.containsKey(edge.get(l).mFirst) )D.put(RegionOne.mFirst[k], D.get(RegionOne.mFirst[k]) + 1);
                }
              }
              //get D for region_two
              for (int k = 0; k < RegionTwo.mFirst.length; ++k) {
                if(visited.containsKey(RegionTwo.mFirst[k]))continue;
                ArrayList<Pair<Integer, Double>> edge = edges.get(RegionTwo.mFirst[k]);
                for (int l = 0; l < edge.size(); ++l) {
                  if(!visited.containsKey(edge.get(l).mFirst) && point_two.containsKey(edge.get(l).mFirst) )D.put(RegionTwo.mFirst[k], D.get(RegionTwo.mFirst[k]) - 1);
                  if(!visited.containsKey(edge.get(l).mFirst) && point_one.containsKey(edge.get(l).mFirst))D.put(RegionTwo.mFirst[k], D.get(RegionTwo.mFirst[k]) + 1);
                }
              }

              //this is for getting G (from D)
              HashMap<Pair<Integer, Integer>, Integer> g = new HashMap<>();
              for (int k = 0; k < RegionOne.mFirst.length; ++k) {
                for (int l = 0; l < RegionTwo.mFirst.length; ++l) {
                  if(visited.containsKey(RegionOne.mFirst[k]) || visited.containsKey(RegionTwo.mFirst[l]))continue;
                  Pair<Integer, Integer> p = new Pair<>(RegionOne.mFirst[k], RegionTwo.mFirst[l]);
                  Pair<Integer, Integer> inversep = new Pair<>(RegionTwo.mFirst[l], RegionOne.mFirst[k]);
                  g.put(p, D.get(RegionOne.mFirst[k]) + D.get(RegionTwo.mFirst[l]));
                  if (connected.containsKey(p)) {
                    g.put(p, g.get(p) - 1);
                  }
                  if (connected.containsKey(inversep)) {
                    g.put(p, g.get(p) - 1);
                  }
                }
              }

              // get the max g
              Integer maxresult = -1000000000;
              Pair<Integer, Integer> p = new Pair<>(-1, -1);
              for (int k = 0; k < RegionOne.mFirst.length; ++k) {
                for (int l = 0; l < RegionTwo.mFirst.length; ++l) {
                  if(visited.containsKey(RegionOne.mFirst[k]) || visited.containsKey(RegionTwo.mFirst[l]))continue;
                  Pair<Integer, Integer> newp = new Pair<>(RegionOne.mFirst[k], RegionTwo.mFirst[l]);
                  if (g.get(newp) > maxresult) {
                    maxresult = g.get(newp);
                    p = newp;
                  }
                }
              }
              g_lists.add(maxresult);
              left_points.add(p.mFirst);
              right_points.add(p.mSecond);
              visited.put(p.mFirst,true);
              visited.put(p.mSecond,true);
            }
            Integer g_max=0;
            Integer index=0;
            Integer tmp=0;
            for(int k=0;k<g_lists.size();++k)
            {
              tmp+=g_lists.get(k);
              if(tmp>g_max)
              {
                g_max=tmp;
                index=k;
              }
            }
            if(g_max>0)
            {
              for(int k=0;k<left_points.size();++k)
              {
                Integer left=left_points.get(k);
                Integer right=right_points.get(k);
                for(int l=0;l<RegionOne.mFirst.length;++l)
                {
                  if(RegionOne.mFirst[l]==left)
                  {
                    RegionOne.mFirst[l]=right;
                  }
                }
                for(int l=0;l<RegionOne.mSecond.length;++l)
                {
                  if(RegionOne.mSecond[l]==left)
                  {
                    RegionOne.mSecond[l]=right;
                  }
                }
                for(int l=0;l<RegionTwo.mFirst.length;++l)
                {
                  if(RegionTwo.mFirst[l]==right)
                  {
                    RegionTwo.mFirst[l]=left;
                  }
                }
                for(int l=0;l<RegionTwo.mSecond.length;++l)
                {
                  if(RegionTwo.mSecond[l]==right)
                  {
                    RegionTwo.mSecond[l]=left;
                  }
                }
              }
            }
            else break;
          }
          candidate.set(i,RegionOne);
          candidate.set(j,RegionTwo);
        }
      }
    }
    return candidate;
  }
  public ArrayList<Pair<int[],int[]>>getAllRegions(MapPoint center, InRegionTest inRegionTest) throws IOException {
    ArrayList<Pair<int[],int[]>>result=new ArrayList();
    Pair<int[],int[]>points = null;

    try {
      points=getPrivacyRegion(center,inRegionTest);
    } catch (PointNotFoundException e) {
      e.printStackTrace();
    }

    HashSet<Integer> allpoints_set=new HashSet<>();

    allpoints=points.mFirst;

    border=new HashMap<>();
    visited=new HashMap<>();
    for(int i=0;i<allpoints.length;++i)
    {
      border.put(allpoints[i],false);
      visited.put(allpoints[i],false);
      allpoints_set.add(allpoints[i]);
    }
    System.out.print("Total Number of Points in this graph is: "+allpoints.length+"\n");
    edges=new HashMap<>();

   // FileOutputStream moutput;
  //  File outputfile = new File("distance_distribution.txt");
  //  moutput=new FileOutputStream(outputfile);
    for(int i=0;i<allpoints.length;++i)
    {
      int NodeId=allpoints[i];
      com.graphhopper.util.EdgeIterator iter = mSurroundings.getEdgeout().setBaseNode(NodeId);
      ArrayList<Pair<Integer,Double>> edge= new ArrayList<>();
      while(iter.next())
      {
        int NextId=iter.getAdjNode();
        if(!allpoints_set.contains(NextId))continue;
        double distance=iter.getDistance();
    //    String dist=Double.toString(distance);
    //    dist+="\n";
    //    moutput.write(dist.getBytes());
        edge.add(new Pair<>(NextId,distance));
      }
      edges.put(NodeId,edge);
    }
    double distance=1000000.0;
    group=new HashMap<>();
    group_size=new HashMap<>();
    Integer group_num=new Integer(0);
    ArrayList<ArrayList<Integer>>all_result=new ArrayList<>();
    for(int i=0;i<allpoints.length;++i)
    {
      if(!visited.get(allpoints[i]))
      {
        group_num+=1;
        ArrayList<Integer> sub_result=FindNeighbors(allpoints[i],distance);
        for(int j=0;j<sub_result.size();++j)
        {
          group.put(sub_result.get(j),group_num);
        }
        group_size.put(group_num,sub_result.size());
        all_result.add(sub_result);
      }
    }
    System.out.print("Partitioned\n");
    for(int j=0;j<all_result.size();++j)
    {
      ArrayList<Integer>new_result=new ArrayList<>();
      for(Integer tmp:all_result.get(j))
      {
        if(group.get(tmp)!=j+1)continue;
        else
        {
          new_result.add(tmp);
        }
      }
      all_result.set(j,new_result);
    }
    for(int i=0;i<all_result.size();++i)
    {
      int [] allArray=new int[all_result.get(i).size()];
      ArrayList<Integer>border_result=new ArrayList<>();
      for(int j=0;j<all_result.get(i).size();++j)
      {
        allArray[j]=all_result.get(i).get(j);
        if(border.get(all_result.get(i).get(j)))
        {
          border_result.add(all_result.get(i).get(j));
        }
      }
      int []borderArray=new int[border_result.size()];
      for(int j=0;j<border_result.size();++j)
      {
        borderArray[j]=border_result.get(j);
      }
      result.add(new Pair<>(allArray,borderArray));
    }



    /*
    DistanceCalcEarth distanceCalcEarth = new DistanceCalcEarth();
    NodeAccess nodeAccess = mHopper.getGraphHopperStorage().getNodeAccess();
    ArrayList<Integer>mapping=new ArrayList<>();
    for(int i=0;i<result.size();++i)
    {
      if(result.get(i).mFirst.length<min_size)
      {
          int index=-1;
          int compare_distance=-1;
          double start_lat=nodeAccess.getLat(result.get(i).mFirst[0]);
          double start_lon=nodeAccess.getLon(result.get(i).mFirst[0]);
          for(int j=0;j<result.size();++j)
          {
              if(result.get(j).mFirst.length>=min_size)
              {
                 double end_lat=nodeAccess.getLat(result.get(j).mFirst[0]);
                 double end_lon=nodeAccess.getLon(result.get(j).mFirst[0]);
                 int tmp= (int) distanceCalcEarth.calcDist(start_lat,start_lon,end_lat,end_lon);
                 if(index==-1 || tmp<compare_distance)
                 {
                   index=j;
                   compare_distance=tmp;
                 }
              }
          }
          mapping.add(index);
      }
      else
      {
          mapping.add(-1);
      }
    }
    ArrayList<Pair<int[],int[]>>newresult=new ArrayList();
    for(int i=0;i<result.size();++i)
    {
      if(mapping.get(i)==-1)continue;
      else
      {
        int index=mapping.get(i);
        int [] allpoints=new int[result.get(i).mFirst.length+result.get(index).mFirst.length];
        for(int j=0;j<result.get(i).mFirst.length;++j)allpoints[j]=result.get(i).mFirst[j];
        for(int j=0;j<result.get(index).mFirst.length;++j)allpoints[j+result.get(i).mFirst.length]=result.get(index).mFirst[j];
        Pair<int[],int[]>newpair=new Pair<>(allpoints,result.get(index).mSecond);
        result.set(index,newpair);
      }
    }
    for(int i=0;i<result.size();++i)
    {
      if(mapping.get(i)==-1)
      {
        newresult.add(result.get(i));
      }
    }

    newresult=extension_Kernighan_Lin(newresult);
    */

    DistanceCalcEarth distanceCalcEarth = new DistanceCalcEarth();
    NodeAccess nodeAccess = mHopper.getGraphHopperStorage().getNodeAccess();
    int []existed=new int[result.size()];
    int []size=new int[result.size()];
    for(int i=0;i<result.size();++i) {
      size[i]=result.get(i).mFirst.length;
      existed[i] = -1;
    }
    for(int i=0;i<result.size();++i)
    {
      if(size[i]<min_size)
      {
        double start_lat=nodeAccess.getLat(result.get(i).mFirst[0]);
        double start_lon=nodeAccess.getLon(result.get(i).mFirst[0]);
        double tmp=-1;
        int index=-1;
        for(int j=0;j<result.size();++j)
          {
            if(size[j]<=50)
            {
              double end_lat=nodeAccess.getLat(result.get(j).mFirst[0]);
              double end_lon=nodeAccess.getLon(result.get(j).mFirst[0]);
              double compare=distanceCalcEarth.calcDist(start_lat,start_lon,end_lat,end_lon);
              if(index==-1 || compare<tmp)
              {
                index=j;
                tmp=compare;
              }
            }
          }
         existed[i]=index;
         size[index]+=size[i];
      }
    }

    ArrayList<Pair<int[],int[]>>newresult=new ArrayList();
    ArrayList<Pair<ArrayList<Integer>,ArrayList<Integer>>>tmp_result=new ArrayList<>();
    for(int i=0;i<result.size();++i)
    {
      ArrayList<Integer> a=new ArrayList<>();
      ArrayList<Integer> b=new ArrayList<>();
      Pair<ArrayList<Integer>,ArrayList<Integer>>tmp=new Pair<>(a,b);
      tmp_result.add(tmp);
    }

    for(int i=0;i<result.size();++i)
    {
      if(existed[i]==-1)
      {
        for(int j=0;j<result.get(i).mFirst.length;++j)
        {
          tmp_result.get(i).mFirst.add(result.get(i).mFirst[j]);
        }
        for(int j=0;j<result.get(i).mSecond.length;++j)
        {
          tmp_result.get(i).mSecond.add(result.get(i).mSecond[j]);
        }
      }
      else
      {
        int index=existed[i];
        for(int j=0;j<result.get(i).mFirst.length;++j)
        {
          tmp_result.get(index).mFirst.add(result.get(i).mFirst[j]);
        }
        for(int j=0;j<result.get(i).mSecond.length;++j)
        {
          tmp_result.get(index).mSecond.add(result.get(i).mSecond[j]);
        }

/*
        int [] allpoints=new int[result.get(i).mFirst.length+result.get(index).mFirst.length];
        int [] borderpoints=new int[result.get(i).mSecond.length+result.get(index).mSecond.length];

        for(int j=0;j<result.get(i).mFirst.length;++j)allpoints[j]=result.get(i).mFirst[j];
        for(int j=0;j<result.get(index).mFirst.length;++j)allpoints[j+result.get(i).mFirst.length]=result.get(index).mFirst[j];

        for(int j=0;j<result.get(i).mSecond.length;++j)borderpoints[j]=result.get(i).mSecond[j];
        for(int j=0;j<result.get(index).mSecond.length;++j)borderpoints[j+result.get(i).mSecond.length]=result.get(index).mSecond[j];


        Pair<int[],int[]>newpair=new Pair<>(allpoints,borderpoints);
        result.set(index,newpair);
        */
      }
    }
    System.out.print("Finish Merging\n");

    for(int i=0;i<tmp_result.size();++i)
    {
      if(existed[i]==-1)
      {
        int [] allpoints=new int[tmp_result.get(i).mFirst.size()];
        int [] borderpoints=new int[tmp_result.get(i).mSecond.size()];
        for(int j=0;j<tmp_result.get(i).mFirst.size();++j)
        {
          allpoints[j]=tmp_result.get(i).mFirst.get(j);
        }
        for(int j=0;j<tmp_result.get(i).mSecond.size();++j)
        {
          borderpoints[j]=tmp_result.get(i).mSecond.get(j);
        }
        Pair<int[],int[]>newpair=new Pair<>(allpoints,borderpoints);
        newresult.add(newpair);
      }
    }

    /*
    for(int i=0;i<newresult.size();++i)
    {
      int [] allpoints = newresult.get(i).mFirst;
      ArrayList<MapPoint>m=new ArrayList<>();
      for(int j=0;j<allpoints.length;++j) {
        double lat=nodeAccess.getLat(allpoints[j]);
        double lon=nodeAccess.getLon(allpoints[j]);
        m.add(new MapPoint(lat,lon));
      }
      ArrayList<MapPoint>outer=Convex.getConvex(m);
      int []borderpoints=new int[outer.size()];
      for(int j=0;j<borderpoints.length;++j)borderpoints[j]=mHopper.getLocationIndex()
              .findClosest(outer.get(j).mFirst,outer.get(j).mSecond, EdgeFilter.ALL_EDGES)
              .getClosestNode();
      newresult.get(i).mSecond=borderpoints;
    }
    System.out.print("Finish border\n");

    ArrayList<Pair<int[], int[]>> regions = new ArrayList<>();
    int shown=newresult.size();
    for (int i=0;i<newresult.size() && shown>0;++i)
    {
      int [] empty=new int[0];
      Pair<int[],int[]>newregion=new Pair<>(empty,newresult.get(i).mSecond);
      regions.add(newregion);
      shown-=1;
    }
    visualizer.drawRegions(regions);
    visualizer.show();

*/
    newresult=extension_Kernighan_Lin(newresult);
    System.out.print("Finish KL algorithm\n");
    /*
    for(int i=0;i<newresult.size();++i)
    {
      int [] allpoints = newresult.get(i).mFirst;
      ArrayList<MapPoint>m=new ArrayList<>();
      for(int j=0;j<allpoints.length;++j) {
        double lat=nodeAccess.getLat(allpoints[j]);
        double lon=nodeAccess.getLon(allpoints[j]);
        m.add(new MapPoint(lat,lon));
      }
      ArrayList<MapPoint>outer=Convex.getConvex(m);
      int []borderpoints=new int[outer.size()];
      for(int j=0;j<borderpoints.length;++j)borderpoints[j]=mHopper.getLocationIndex()
              .findClosest(outer.get(j).mFirst,outer.get(j).mSecond, EdgeFilter.ALL_EDGES)
              .getClosestNode();
      newresult.get(i).mSecond=borderpoints;
    }
    */

    System.out.print("The number of group is "+newresult.size()+"\n");

/*
    ArrayList<Pair<int[], int[]>> regions = new ArrayList<>();
    int shown=result.size();
    for (int i=0;i<result.size() && shown>0;++i)
    {
      if(result.get(i).mFirst.length>20)
      {
        int [] empty=new int[0];
        Pair<int[],int[]>newregion=new Pair<>(empty,result.get(i).mSecond);
        regions.add(newregion);
        shown-=1;
      }
    }

    visualizer.drawRegions(regions);
    visualizer.show();
*/
    return newresult;
  }
  public Pair<int[], int[]> getPrivacyRegion(MapPoint center, InRegionTest inRegionTest)
      throws PointNotFoundException {
    Pair<ArrayList<MapPoint>, ArrayList<MapPoint>> pointsAndBorder = mSurroundings
        .getSurroundingAndBoundary(center.getLat(), center.getLon(), inRegionTest);
    ArrayList<MapPoint> points = pointsAndBorder.mFirst;
    if (points.size() == 0) {
      throw new PointNotFoundException(center);
    }
    int[] allArray = new int[points.size()];
    // find all points
    for (int i = 0; i < allArray.length; i++) {
      QueryResult closest = mHopper
          .getLocationIndex()
          .findClosest(points.get(i).getLat(), points.get(i).getLon(), EdgeFilter.ALL_EDGES);
      allArray[i] = closest.getClosestNode();
    }
    // find all the border points
    ArrayList<MapPoint> border = pointsAndBorder.mSecond;
    int[] borderArray = new int[border.size()];
    for (int i = 0; i < borderArray.length; i++) {
      QueryResult closest = mHopper
          .getLocationIndex()
          .findClosest(border.get(i).mFirst, border.get(i).mSecond, EdgeFilter.ALL_EDGES);
      borderArray[i] = closest.getClosestNode();
    }
    return new Pair<>(allArray, borderArray);
  }
  public int getspecific_region(MapPoint point,ArrayList<HashSet<Integer>> allRegionSet)
  {
    QueryResult closest = mHopper
            .getLocationIndex()
            .findClosest(point.getLat(), point.getLon(), EdgeFilter.ALL_EDGES);
    int NodeId=closest.getClosestNode();
    Set<Integer> set = new HashSet<>();
    for (int i=0;i<allRegionSet.size();++i)
    {
      HashSet<Integer> TmpRegionSet=allRegionSet.get(i);
      if(TmpRegionSet.contains(NodeId))return i;
      else continue;
    }
    return -1;
  }
  public Paths set2Set(int[] sourceSet, int[] targetSet, boolean hasCenter, int targetCenter,
      double radius) {
    try {
      /*TODO: change the weight*/
      S2SStrategy strategy = S2SStrategy.strategyFactory(mStrategy, new CallBacks() {
        @Override
        public EdgeIterator getIterator(int current, int prevEdgeID) {
          /*TODO: change the hard coded name too*/
          if (mStrategy.equalsIgnoreCase(S2SStrategy.ASTAR)) {
            return new AStarEdgeIterator(current, prevEdgeID, mHopper.getGraphHopperStorage()
                .createEdgeExplorer(new DefaultEdgeFilter(mEm.getEncoder("car"), false, true)));
          }
          return new DefaultEdgeIterator(current, prevEdgeID, mHopper.getGraphHopperStorage()
              .createEdgeExplorer(new DefaultEdgeFilter(mEm.getEncoder("car"), false, true)));
        }

        /**
         * Get the minimum potential among all the targets
         * @param current current node
         * @param targets the targets
         * @return the minimum potential
         */
        @Override
        public double getPotential(int current, HashSet<Integer> targets) {
          if (!mStrategy.equalsIgnoreCase(S2SStrategy.ASTAR)) {
            return 0.0;
          }
          NodeAccess nodeAccess = mHopper.getGraphHopperStorage().getNodeAccess();
          DistanceCalcEarth distanceCalcEarth = new DistanceCalcEarth();
          double fromLat = nodeAccess.getLat(current);
          double fromLon = nodeAccess.getLon(current);
          double toLat = nodeAccess.getLat(targetCenter);
          double toLon = nodeAccess.getLon(targetCenter);
          double toCenter = distanceCalcEarth.calcDist(fromLat, fromLon, toLat, toLon);
          if (!hasCenter || toCenter < radius) {
            double minDist = 1e100;
            for (int target : targets) {
              toLat = nodeAccess.getLat(target);
              toLon = nodeAccess.getLon(target);
              double distance = distanceCalcEarth.calcDist(fromLat, fromLon, toLat, toLon);
              if (distance < minDist) {
                minDist = distance;
              }
            }
            return minDist;
          }
          return toCenter - radius;
        }
      });
      return strategy.compute(sourceSet, targetSet);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

}

