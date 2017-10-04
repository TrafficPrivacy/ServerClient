package util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Paths implements Serializable {
    /*TODO: fix the potential hash collision*/
    private HashMap<Pair<Integer, Integer>, Triple<Integer[], Double, Double>> mPaths;

    /*TODO: remove the debug stuff*/
    // debug
    public ArrayList<Pair<Integer, Integer>> paths;

    public Paths() {
        mPaths = new HashMap<>();
        // debug
        paths = new ArrayList<>();
    }

    public Integer[] findPath(int start, int end) {
        Pair<Integer, Integer> queryPoint = new Pair<>(start, end);
        if (!mPaths.containsKey(queryPoint))
            return null;
        return mPaths.get(queryPoint).mFirst;
    }

    public double findDistance(int start, int end) {
        Pair<Integer, Integer> queryPoint = new Pair<>(start, end);
        if (!mPaths.containsKey(queryPoint))
            return -1;
        return mPaths.get(queryPoint).mThird;
    }

    public double findWeight(int start, int end) {
        Pair<Integer, Integer> queryPoint = new Pair<>(start, end);
        if (!mPaths.containsKey(queryPoint))
            return -1;
        return mPaths.get(queryPoint).mSecond;
    }

    public void addPath(int start, int end, double distance, double weight, Integer[] path) {
        Pair<Integer, Integer> queryPoint = new Pair<>(start, end);
        if (mPaths.containsKey(queryPoint.hashCode()))
            return;
        Triple<Integer[], Double, Double> info = new Triple<>(path, weight, distance);
        mPaths.put(queryPoint, info);

        //debug
        paths.add(queryPoint);
    }

    public void addAll(Paths paths) {
        mPaths.putAll(paths.mPaths);
        // debug
        this.paths.addAll(paths.paths);
    }

    public int numOfPaths() {
        return mPaths.size();
    }

    public Set<Pair<Integer, Integer>> getPaths() {
        return mPaths.keySet();
    }
}
