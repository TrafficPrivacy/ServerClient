package util;

import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

public class Convex {

  public static ArrayList<MapPoint> getConvex(ArrayList<MapPoint> dots) {
    int n = dots.size();
    HashMap<Integer,Boolean>m=new HashMap<>();
    if (n < 3) {
      return new ArrayList<>();
    }
    ArrayList<MapPoint> convex = new ArrayList<>();
    int leftMost = 0;
    for (int i = 1; i < n; i++) {
      if (dots.get(i).mSecond < dots.get(leftMost).mSecond) {
        leftMost = i;
      }
    }
    int p = leftMost, q, counter = 0;
    do {
      q = (p + 1) % n;
      for (int i = 0; i < n; i++) {
        if (convexHelper(dots.get(p), dots.get(i), dots.get(q))) {
          q = i;
        }
      }
      if(!m.containsKey(p)) {
        convex.add(dots.get(p));
        m.put(p,true);
      }
      p = q;
      counter++;
    } while (p != leftMost && counter <= dots.size());
    if(!m.containsKey(leftMost))convex.add(dots.get(leftMost));
    return convex;
  }

  private static boolean convexHelper(MapPoint p, MapPoint q, MapPoint r) {
    double val =
        (q.mFirst - p.mFirst) * (r.mSecond - q.mSecond) - (q.mSecond - p.mSecond) * (r.mFirst
            - q.mFirst);
    return !(val >= 0);
  }
  public static Boolean check(ArrayList<MapPoint> dots,MapPoint candidate)
  {
    ArrayList<MapPoint>tmp=new ArrayList<>(dots);
    tmp.add(candidate);
    ArrayList<MapPoint>newborder=getConvex(tmp);
    for(int i=0;i<newborder.size();++i)
    {
      if(candidate==newborder.get(i))return false;
    }
    return true;
  }
  private static double getX(double lat,double lon)
  {
    return cos(toRadians(lat)) * cos(toRadians(lon));

  }
  private static double getY(double lat,double lon)
  {
    return  cos(toRadians(lat)) * sin(toRadians(lon));
  }

  public static Boolean check_2(ArrayList<MapPoint> dots,MapPoint candidate)
  {
      boolean result = false;
      double x=getX(candidate.getLat(),candidate.getLon());
      double y=getY(candidate.getLat(),candidate.getLon());
    for (int i = 0, j = dots.size() - 1; i < dots.size(); j = i++) {
      double point_i_x=getX(dots.get(i).getLat(),dots.get(i).getLon());
      double point_i_y=getY(dots.get(i).getLat(),dots.get(i).getLon());
      double point_j_x=getX(dots.get(j).getLat(),dots.get(j).getLon());
      double point_j_y=getY(dots.get(j).getLat(),dots.get(j).getLon());
      if ((point_i_y > y) != (point_j_y > y) &&
              (x < (point_j_x - point_i_x) * (y - point_i_y) / (point_j_y-point_i_y) + point_i_x)) {
        result = !result;
      }
    }
      return result;
  }

  public static double getConvexArea(ArrayList<MapPoint> dots) {
    /* http://www.mathopenref.com/coordpolygonarea2.html */
    double area = 0.0;
    int j = dots.size() - 1;
    for (int i = 0; i < dots.size(); i++) {
      MapPoint mapPointi = dots.get(i);
      MapPoint mapPointj = dots.get(j);
      area += (mapPointi.getLat() + mapPointj.getLat()) * (mapPointj.getLon() - mapPointj.getLat());
    }
    return area / -2;
  }
}

