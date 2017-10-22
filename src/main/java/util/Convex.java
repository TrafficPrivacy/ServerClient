package util;

import java.util.ArrayList;

public class Convex {

    public static ArrayList<MapPoint> getConvex(ArrayList<MapPoint> dots) {
        int n = dots.size();
        if (n < 3)
            return new ArrayList<>();
        ArrayList<MapPoint> convex = new ArrayList<>();
        int leftMost = 0;
        for (int i = 1; i < n; i++) {
            if (dots.get(i).mSecond < dots.get(leftMost).mSecond)
                leftMost = i;
        }
        int p = leftMost, q, counter = 0;
        do {
            q = (p + 1) % n;
            for (int i = 0; i < n; i++) {
                if (convexHelper(dots.get(p), dots.get(i), dots.get(q)))
                    q = i;
            }
            convex.add(dots.get(p));
            p = q;
            counter ++;
        } while (p != leftMost && counter <= dots.size());
        convex.add(dots.get(leftMost));
        return convex;
    }

    private static boolean convexHelper(MapPoint p, MapPoint q, MapPoint r) {
        double val = (q.mFirst - p.mFirst) * (r.mSecond - q.mSecond) - (q.mSecond - p.mSecond) * (r.mFirst - q.mFirst);
        return !(val >= 0);
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

