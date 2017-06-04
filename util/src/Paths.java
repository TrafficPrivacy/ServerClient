import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Paths implements Serializable{
    /*TODO: fix the potential hash collision*/
    private HashMap<Integer, Integer[]> mPaths;
    private HashMap<Integer, Double> mDistances;
    private HashMap<Integer, Double> mWeights;

    /*TODO: remove the debug stuff*/
    // debug
    public ArrayList<Pair<Integer, Integer>> paths;

    public Paths() {
        mPaths = new HashMap<>();
        mDistances = new HashMap<>();
        mWeights = new HashMap<>();
        // debug
        paths = new ArrayList<>();
    }

    public Integer[] findPath(int start, int end) {
        Pair<Integer, Integer> queryPoint = new Pair<>(start, end);
        if (!mPaths.containsKey(queryPoint.hashCode()))
            return null;
        return mPaths.get(queryPoint.hashCode());
    }

    public double findDistance(int start, int end) {
        Pair<Integer, Integer> queryPoint = new Pair<>(start, end);
        if (!mDistances.containsKey(queryPoint.hashCode()))
            return -1;
        return mDistances.get(queryPoint.hashCode());
    }

    public double findWeight(int start, int end) {
        Pair<Integer, Integer> queryPoint = new Pair<>(start, end);
        if (!mWeights.containsKey(queryPoint.hashCode()))
            return -1;
        return mWeights.get(queryPoint.hashCode());
    }

    public void addPath(int start, int end, double distance, double weight, Integer[] path) {
        Pair<Integer, Integer> queryPoint = new Pair<>(start, end);
        if (mPaths.containsKey(queryPoint.hashCode()))
            return;
        mPaths.put(queryPoint.hashCode(), path);
        mDistances.put(queryPoint.hashCode(), distance);
        mWeights.put(queryPoint.hashCode(), weight);

        //debug
        paths.add(queryPoint);
    }

    public void addAll(Paths paths) {
        mPaths.putAll(paths.mPaths);
        mDistances.putAll(paths.mDistances);
        mWeights.putAll(paths.mWeights);
        // debug
        this.paths.addAll(paths.paths);
    }

    public int numOfPaths() {
        return mPaths.size();
    }
}
