package util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Paths implements Serializable {

  /*TODO: fix the potential hash collision*/
  // values are distance and weight
  private HashMap<Pair<Integer, Integer>, Triple<Integer[], Double, Double>> mPaths;

  public Paths() {
    mPaths = new HashMap<>();
  }

  public Integer[] findPath(int start, int end) {
    Pair<Integer, Integer> queryPoint = new Pair<>(start, end);
    if (!mPaths.containsKey(queryPoint)) {
      return null;
    }
    return mPaths.get(queryPoint).mFirst;
  }

  public double findDistance(int start, int end) {
    Pair<Integer, Integer> queryPoint = new Pair<>(start, end);
    if (!mPaths.containsKey(queryPoint)) {
      return -1;
    }
    return mPaths.get(queryPoint).mThird;
  }

  public double findWeight(int start, int end) {
    Pair<Integer, Integer> queryPoint = new Pair<>(start, end);
    if (!mPaths.containsKey(queryPoint)) {
      return -1;
    }
    return mPaths.get(queryPoint).mSecond;
  }

  public void addPath(int start, int end, double distance, double weight, Integer[] path) {
    Pair<Integer, Integer> queryPoint = new Pair<>(start, end);
    if (mPaths.containsKey(queryPoint.hashCode())) {
      return;
    }
    Triple<Integer[], Double, Double> info = new Triple<>(path, weight, distance);
    mPaths.put(queryPoint, info);
  }

  public void addAll(Paths paths) {
    mPaths.putAll(paths.mPaths);
  }

  public int size() {
    return mPaths.size();
  }

  public ArrayList<Integer[]> getPaths() {
    ArrayList<Integer[]> paths = new ArrayList<>();
    for (Pair<Integer, Integer> key : mPaths.keySet()) {
      paths.add(mPaths.get(key).mFirst);
    }
    return paths;
  }
}
