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
import com.kitfox.svg.A;
import com.sun.org.apache.xpath.internal.operations.Bool;
import jdk.nashorn.internal.parser.JSONParser;
import org.json.JSONArray;
import util.*;

import java.io.*;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.json.JSONObject;

import static java.lang.Double.max;
import static java.lang.Integer.min;

public class MatrixComputer {

  private String mStrategy;
  private EncodingManager mEm;
  private GraphHopper mHopper;
  private Surroundings mSurroundings;
  //this is for partitioning the graph
  private int [] Allpoints;
  private HashMap<Integer,Boolean> border;
  private HashMap<Integer,Boolean> visited;
  private HashMap<Integer,Integer> group;
  private HashMap<Integer,Integer>group_size;
  private HashMap<String, Boolean>osm_value;
  private HashMap<Integer,ArrayList<Pair<Integer, Double>>> edges;
  private Visualizer visualizer;
  private Integer min_size ;
  private Integer max_size ;
  private Integer poi_size;
  private FileOutputStream moutput_parition;
  private FileOutputStream moutput_partition_two;
  private String poi_file;
  private FileOutputStream moutput_partition_time_distribution;
  private HashSet<Integer> allpoints_set;
  public NodeAccess nodeAccess_for_out_usage;
  // the above is for partitioning the graph

  public MatrixComputer(String osmPath, String osmFolder, String strategy) {
    /*TODO: change this hard coded encoding*/
    allpoints_set=new HashSet<>();
    min_size = new Integer(10);
    max_size = new Integer(15);
    poi_size = new Integer(0);
    osm_value=new HashMap<>();
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
    nodeAccess_for_out_usage=mHopper.getGraphHopperStorage().getNodeAccess();
  }
  public Integer POI_count(int []allpoints,NodeAccess nodeAccess) throws IOException {
    HashSet<Integer> all_points=new HashSet<>();
    for(int i=0;i<allpoints.length;++i)all_points.add(allpoints[i]);
    double lat=0.;
    double lon=0.;
    for(int i=0;i<allpoints.length;++i)
    {
      lat+=nodeAccess.getLat(allpoints[i]);
      lon+=nodeAccess.getLon(allpoints[i]);
    }
    lat/=allpoints.length;
    lon/=allpoints.length;
    String website="https://graphhopper.com/api/1/geocode?point=";
    website+=lat+","+lon;
    website+="&reverse=true&key=988335ea-d791-43a0-ad3f-3fd17c0c743e";
    URL url = new URL(website);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setRequestMethod("GET");
    con.setRequestProperty("User-Agent", "Mozilla/5.0");
    BufferedReader in = new BufferedReader(
            new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuffer response = new StringBuffer();
    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }
    in.close();
    JSONObject jsonObj = new JSONObject(response.toString());
    JSONArray pois = jsonObj.getJSONArray("hits");
    ArrayList<JSONObject> candidate=new ArrayList<>();
    ArrayList<JSONObject> real_candidate=new ArrayList<>();
    for(int i=0;i<pois.length();++i)
    {
      candidate.add((JSONObject) pois.get(i));
    }
    Integer result=0;
    for(int i=0;i<candidate.size();++i)
    {
      JSONObject element= (JSONObject)(candidate.get(i).get("point"));
      double lat_of_poi=element.getDouble("lat");
      double lon_of_poi=element.getDouble("lng");
      int closest=mHopper.getLocationIndex()
              .findClosest(lat_of_poi, lon_of_poi, EdgeFilter.ALL_EDGES)
              .getClosestNode();
      if(all_points.contains(closest)) {
       // String type=candidate.get(i).getString("osm_value")+"\n";
      //  moutput_partition_two.write(type.getBytes());
        result += 1;
      }
    }
    return result;
  }
  public Double Time_distribution(int []allpoints)
  {
    double result=0;
    HashSet<Integer>exist=new HashSet<>();
    for(int i=0;i<allpoints.length;++i)exist.add(allpoints[i]);
    for(int i=0;i<allpoints.length;++i) {
      Integer cur=allpoints[i];
      PriorityQueue<NodeWrapper> mQueue = new PriorityQueue<>();
      HashMap<Integer, NodeWrapper> mNodeReference = new HashMap<>();
      mNodeReference.put(cur, new NodeWrapper(cur, 0, cur, -1, 0));
      mQueue.add(mNodeReference.get(cur));

      while (!mQueue.isEmpty()) {
        NodeWrapper current = mQueue.poll();
        if (current.mNodeID == -1) {
          continue;
        }
        result=max(result,current.mCost);
        DefaultEdgeIterator nextNodes = new DefaultEdgeIterator(current.mNodeID, current.mPreviousEdgeID, mHopper.getGraphHopperStorage()
                .createEdgeExplorer(new DefaultEdgeFilter(mEm.getEncoder("car"), false, true)));
        while (nextNodes.next()) {
          int nextID = nextNodes.getNext();
          if(!exist.contains(nextID))continue;
          double tempCost = current.mCost + nextNodes.getCost();
          NodeWrapper next;
          if (mNodeReference.containsKey(nextID)) {
            next = mNodeReference.get(nextID);
            /* Decrease Key */
            if (next.mCost > tempCost) {
              mQueue.remove(next);
              next.mCost = tempCost;
              next.mParent = current.mNodeID;
              next.mDistance = current.mDistance + nextNodes.getDistance();
              mQueue.add(next);
            }
          } else {
            next = new NodeWrapper(nextID, tempCost, current.mNodeID,
                    nextNodes.getEdge(), current.mDistance + nextNodes.getDistance());
            mNodeReference.put(nextID, next);
            mQueue.add(next);
          }
        }
      }
    }
    return result;
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
  private ArrayList<Integer> FindNeighbors_based_on_Time(Integer cur) {
    visited.put(cur, true);
    ArrayList<Integer> result = new ArrayList<>();result.add(cur);
    PriorityQueue<NodeWrapper> mQueue = new PriorityQueue<>();
    HashMap<Integer, NodeWrapper> mNodeReference = new HashMap<>();
    mNodeReference.put(cur, new NodeWrapper(cur, 0, cur, -1, 0));
    mQueue.add(mNodeReference.get(cur));


    while (!mQueue.isEmpty()) {
      NodeWrapper current = mQueue.poll();
      if (current.mNodeID == -1) {
        continue;
      }
      if (allpoints_set.contains(current.mNodeID) && !visited.get(current.mNodeID)) {
        result.add(current.mNodeID);
        visited.put(current.mNodeID,true);
      }
      if (result.size()>=max_size) break;
      DefaultEdgeIterator nextNodes = new DefaultEdgeIterator(current.mNodeID, current.mPreviousEdgeID, mHopper.getGraphHopperStorage()
              .createEdgeExplorer(new DefaultEdgeFilter(mEm.getEncoder("car"), false, true)));
      boolean flag = false;
      while (nextNodes.next()) {
        int nextID = nextNodes.getNext();
        if(!allpoints_set.contains(nextID) || visited.get(nextID))continue;
        double tempCost = current.mCost + nextNodes.getCost();
        if(tempCost>100.0)continue;
        NodeWrapper next;
        if (mNodeReference.containsKey(nextID)) {
          next = mNodeReference.get(nextID);
          /* Decrease Key */
          if (next.mCost > tempCost) {
            mQueue.remove(next);
            next.mCost = tempCost;
            next.mParent = current.mNodeID;
            next.mDistance = current.mDistance + nextNodes.getDistance();
            mQueue.add(next);
            flag=true;
          }
        } else {
          next = new NodeWrapper(nextID, tempCost, current.mNodeID,
                  nextNodes.getEdge(), current.mDistance + nextNodes.getDistance());
          mNodeReference.put(nextID, next);
          mQueue.add(next);
          flag=true;
        }
      }
      if (!flag) border.put(current.mNodeID, true);
    }
    return result;
  }
  private Integer merge(ArrayList<Integer>points)
  {
    Integer tmp=-1;
    double cost=0;
    for(int i=0;i<points.size();++i)
    {
      Integer cur=points.get(i);
      NodeWrapper current=new NodeWrapper(cur, 0, cur, -1, 0);
      DefaultEdgeIterator nextNodes = new DefaultEdgeIterator(current.mNodeID, current.mPreviousEdgeID, mHopper.getGraphHopperStorage()
              .createEdgeExplorer(new DefaultEdgeFilter(mEm.getEncoder("car"), false, true)));
      while (nextNodes.next()) {
        int nextID = nextNodes.getNext();
        if(!allpoints_set.contains(nextID) || group.get(current.mNodeID).equals(group.get(nextID)))continue;
        double tempCost = current.mCost + nextNodes.getCost();
        if(tmp==-1)
        {
          tmp=nextID;
          cost=tempCost;
        }
        else
        {
          if(tempCost<cost)
          {
            cost=tempCost;
            tmp=nextID;
          }
        }
      }
    }
    return tmp;
  }
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
          if (!visited.get(tmp.mFirst)  && result.size()<=max_size &&tmp.mSecond <= distance) {
          flag = true;
          visited.put(tmp.mFirst, true);
          q.add(tmp.mFirst);
        }
      }
      if (!flag) border.put(current_point, true);
    }

    /*
    Integer presize=-1;
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
    */
    return result;
  }

  public ArrayList<Pair<int[],int[]>> extension_Kernighan_Lin(ArrayList<Pair<int[],int[]>> candidate)
  {
    NodeAccess nodeAccess = mHopper.getGraphHopperStorage().getNodeAccess();
    DistanceCalcEarth distanceCalcEarth = new DistanceCalcEarth();
    for(int i=0;i<candidate.size();++i)
    {
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
  private Pair<Double,Double>returnmid(NodeAccess nodeAccess, ArrayList<Integer> allpoints)
  {
    double lat=0.0;
    double lon=0.0;
    for(int i=0;i<allpoints.size();++i)
    {
       lat+=nodeAccess.getLat(allpoints.get(i));
       lon+=nodeAccess.getLon(allpoints.get(i));
    }
    return new Pair<>(lat/allpoints.size(),lon/allpoints.size());
  }
  public ArrayList<Pair<int[],int[]>>getAllRegions(MapPoint center, InRegionTest inRegionTest) throws IOException {
    ArrayList<Pair<int[],int[]>>result=new ArrayList();
    Pair<int[],int[]>points = null;
    NodeAccess nodeAccess = mHopper.getGraphHopperStorage().getNodeAccess();
    try {
      points=getPrivacyRegion(center,inRegionTest);
    } catch (PointNotFoundException e) {
      e.printStackTrace();
    }

    Allpoints=points.mFirst;
    border=new HashMap<>();
    visited=new HashMap<>();
    for(int i=0;i<Allpoints.length;++i)
    {
      border.put(Allpoints[i],false);
      visited.put(Allpoints[i],false);
      allpoints_set.add(Allpoints[i]);
    }
    System.out.print("Total Number of Points in this graph is: "+Allpoints.length+"\n");


    edges=new HashMap<>();
    for(int i=0;i<Allpoints.length;++i)
    {
      int NodeId=Allpoints[i];
      com.graphhopper.util.EdgeIterator iter = mSurroundings.getEdgeout().setBaseNode(NodeId);
      ArrayList<Pair<Integer,Double>> edge= new ArrayList<>();
      while(iter.next())
      {
        int NextId=iter.getAdjNode();
        if(!allpoints_set.contains(NextId)) {
          continue;
        }
        double distance=iter.getDistance();
        edge.add(new Pair<>(NextId,distance));
      }
      edges.put(NodeId,edge);
    }
    double distance=1000.0;
    group=new HashMap<>();
    group_size=new HashMap<>();
    Integer group_num=new Integer(0);
    ArrayList<ArrayList<Integer>>all_result=new ArrayList<>();
    for(int i=0;i<Allpoints.length;++i)
    {
      if(!visited.get(Allpoints[i]))
      {
        group_num+=1;
        //ArrayList<Integer> sub_result=FindNeighbors(Allpoints[i],distance);
        ArrayList<Integer> sub_result=FindNeighbors_based_on_Time(Allpoints[i]);
        for(int j=0;j<sub_result.size();++j)
        {
          group.put(sub_result.get(j),group_num);
        }
        group_size.put(group_num,sub_result.size());
        all_result.add(sub_result);
      }
    }



    for(int j=0;j<all_result.size();++j)
    {
      ArrayList<Integer>new_result=new ArrayList<>();
      for(Integer tmp:all_result.get(j))
      {
        if(group.get(tmp)!=j+1) {
          System.out.print("BIG ERROR!\n");
          continue;
        }
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
        if(border.containsKey(all_result.get(i).get(j)))
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

    System.out.print("Initially partitioned\n");
/*
    for(int i=0;i<result.size();++i)
    {
      int [] allpoints = result.get(i).mFirst;
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
      result.get(i).mSecond=borderpoints;
    }
    System.out.print("Finish border\n");

    ArrayList<Pair<int[], int[]>> regions = new ArrayList<>();
    int shown=10000;
    for (int i=0;i<result.size() && shown>0;++i)
    {
      int [] empty=new int[0];
      Pair<int[],int[]>newregion=new Pair<>(result.get(i).mFirst,result.get(i).mSecond);
      regions.add(newregion);
      shown-=1;
    }
    visualizer.drawRegions(regions);
    visualizer.show();
*/

    Integer remain=0;
    Integer ill=0;
    for(int i=0;i<result.size();++i)
    {
      if(result.get(i).mFirst.length<min_size)++remain;
    }
    System.out.print("Start to merge\n");
    System.out.print(remain+" out of "+result.size()+" need to be merged\n");
    ArrayList<ArrayList<Integer>>points_tmp=new ArrayList<>();
    for(int i=0;i<result.size();++i)
    {
      ArrayList<Integer>partition=new ArrayList<>();
      for(int j=0;j<result.get(i).mFirst.length;++j)partition.add(result.get(i).mFirst[j]);
      points_tmp.add(partition);
    }


    DistanceCalcEarth distanceCalcEarth = new DistanceCalcEarth();
    ArrayList<Integer>number_of_poi=new ArrayList<>();
    for(int i=0;i<result.size();++i)
    {
      //number_of_poi.add(POI_count(result.get(i).mFirst,nodeAccess));
       number_of_poi.add(10);
    }
   // moutput_partition_two.flush();
   // moutput_partition_two.close();


    Boolean [] visited= new Boolean[result.size()];
    for(int i=0;i<result.size();++i) {
      visited[i]=Boolean.FALSE;
    }


      for (int i = 0; i < result.size(); ++i) {
        if (points_tmp.get(i).size() < min_size || number_of_poi.get(i) < poi_size) {
          visited[i]=Boolean.TRUE;
          remain--;
          if(remain%100==0)
          {
            System.out.print(remain+" out of "+result.size()+"\n");
          }
          Integer id=merge(points_tmp.get(i));
          if(id==-1) {
            ill++;
            continue;
          }

          Integer group_id=group.get(id);
          points_tmp.get(group_id).addAll(points_tmp.get(i));
          for(int j=0;j<points_tmp.get(i).size();++j)group.put(points_tmp.get(i).get(j),group_id);

          /*
          Pair<Double, Double> coordinate = returnmid(nodeAccess,points_tmp.get(i));
          double start_lat = coordinate.mFirst;
          double start_lon = coordinate.mSecond;
          double tmp = -1;
          int index = -1;
          for (int j = 0; j < result.size(); ++j) {
            if (i == j || visited[j]==true) continue;
            coordinate = returnmid(nodeAccess,points_tmp.get(j));
            double end_lat = coordinate.mFirst;
            double end_lon = coordinate.mSecond;
            double compare = distanceCalcEarth.calcDist(start_lat, start_lon, end_lat, end_lon);
            if (index == -1 || compare < tmp) {
              index = j;
              tmp = compare;
            }
          }
          points_tmp.get(index).addAll(points_tmp.get(i));
          number_of_poi.set(index,number_of_poi.get(index)+number_of_poi.get(i));
          */
        }
      }
    System.out.print("Ill: "+ill+"\n");
    System.out.print("Finish Merging\n");



    ArrayList<Pair<int[],int[]>>newresult=new ArrayList<>();
    for(int i=0;i<points_tmp.size();++i)
    {
      //if(!visited[i])
      if(group.get(points_tmp.get(i).get(0))==i+1)
      {
        int [] allpoints=new int[points_tmp.get(i).size()];
        int [] borderpoints=new int[0];
        for(int j=0;j<allpoints.length;++j)
        {
          allpoints[j]=points_tmp.get(i).get(j);
        }
        Pair<int[],int[]>newpair=new Pair<>(allpoints,borderpoints);
        newresult.add(newpair);
      }
    }

    String output_partition="poi_size_distribution.txt";
    moutput_parition=new FileOutputStream(output_partition);
    for(int i=0;i<points_tmp.size();++i)
    {
        if(visited[i])continue;
        String tmp=number_of_poi.get(i)+"\n";
        moutput_parition.write(tmp.getBytes());
    }
    moutput_parition.flush();
    moutput_parition.close();



    System.out.print("Start to do KL algorithm\n");
    newresult=extension_Kernighan_Lin(newresult);
    System.out.print("Finish KL algorithm\n");
    System.out.print("The number of group is "+newresult.size()+"\n");


    /*for getting time distribution in partition*/
    output_partition="time_partition_distribution.txt";
    moutput_partition_time_distribution=new FileOutputStream(output_partition);
    for(int i=0;i<newresult.size();++i)
    {
       Double tmp=Time_distribution(newresult.get(i).mFirst);
       String tmp2=tmp+"\n";
       moutput_partition_time_distribution.write(tmp2.getBytes());
    }
    moutput_partition_time_distribution.flush();
    moutput_partition_time_distribution.close();


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
    System.out.print("Finish finding border\n");

    /*
    ArrayList<Pair<int[], int[]>> regions = new ArrayList<>();
    int shown=10000;

    for (int i=0;i<newresult.size() && shown>0;++i)
    {
      int [] empty=new int[0];
      Pair<int[],int[]>newregion=new Pair<>(newresult.get(i).mFirst,newresult.get(i).mSecond);
      regions.add(newregion);
      shown-=1;
    }
    visualizer.drawRegions(regions);
    visualizer.show();
*/
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

