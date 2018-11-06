package server;

import algorithm.*;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DistanceCalcEarth;
import util.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
  private HashMap<Integer,ArrayList<Pair<Integer, Double>>> edges;
  // the above is for partitioning the graph

  public MatrixComputer(String osmPath, String osmFolder, String strategy) {
    /*TODO: change this hard coded encoding*/
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
  private ArrayList<Integer> FindNeighbors(Integer cur,double distance)
  {
    visited.put(cur,true);
    ArrayList<Integer> result=new ArrayList<>();
    LinkedList<Integer> q;q=new LinkedList<>();
    q.add(cur);
    while(!q.isEmpty() && result.size()<=50) {
      Integer current_point=q.getFirst();q.pop();
      result.add(current_point);
      ArrayList<Pair<Integer,Double>> edge=edges.get(current_point);
      boolean flag=false;
      for (int i = 0; i < edge.size(); ++i) {
        Pair<Integer, Double> tmp = edge.get(i);
       if (!visited.get(tmp.mFirst) && tmp.mSecond <= distance && result.size()<=50) {
       // if (!visited.get(tmp.mFirst)  && result.size()<=50) {
          flag = true;
          visited.put(tmp.mFirst, true);
          q.add(tmp.mFirst);
        //  ArrayList<Integer> sub_result = FindNeighbors(tmp.mFirst, distance);
        //  result.addAll(sub_result);
       }
      }
      if(!flag)border.put(current_point,true);
    }

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
    Integer min_size=new Integer(20);
    ArrayList<Pair<int[],int[]>>result=new ArrayList();
    Pair<int[],int[]>points = null;
    try {
      points=getPrivacyRegion(center,inRegionTest);
    } catch (PointNotFoundException e) {
      e.printStackTrace();
    }
    HashSet<Integer> allpoints_set=new HashSet<>();
    allpoints = points.mFirst;
    border=new HashMap<>();
    visited=new HashMap<>();
    for(int i=0;i<allpoints.length;++i)
    {
      border.put(allpoints[i],false);
      visited.put(allpoints[i],false);
    }
    for(int i=0;i<allpoints.length;++i)allpoints_set.add(allpoints[i]);
    System.out.print("Total Number of Points in this graph is: "+allpoints.length+"\n");
    edges=new HashMap<>();

    FileOutputStream moutput;
    File outputfile = new File("distance_distribution.txt");
    moutput=new FileOutputStream(outputfile);
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
        String dist=Double.toString(distance);
        dist+="\n";
        moutput.write(dist.getBytes());
        edge.add(new Pair<>(NextId,distance));
      }
      edges.put(NodeId,edge);
    }
    double distance=2000.0;
    group=new HashMap<>();
    for(int i=0;i<allpoints.length;++i)
    {
      if(!visited.get(allpoints[i]))
      {
        ArrayList<Integer> sub_result=FindNeighbors(allpoints[i],distance);
        ArrayList<Integer> boarder_result=new ArrayList<>();
        for(int j=0;j<sub_result.size();++j)
        {
          int id=sub_result.get(j);
          if(border.get(id))
          {
            boarder_result.add(id);
          }
        }
        int [] allArray=new int[sub_result.size()];
        int [] borderArray=new int[boarder_result.size()];
        for(int j=0;j<sub_result.size();++j)
        {
          allArray[j]=sub_result.get(j);
        }
        for(int j=0;j<boarder_result.size();++j)
        {
          borderArray[j]=boarder_result.get(j);
        }
        result.add(new Pair<>(allArray,borderArray));
      }
    }

    result=extension_Kernighan_Lin(result);
    System.out.print("The number of group is "+result.size()+"\n");
    return result;
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

