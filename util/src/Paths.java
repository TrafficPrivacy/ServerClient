import java.io.Serializable;
import java.util.HashMap;

public class Paths implements Serializable{
    private HashMap<Pair<Integer, Integer>, Pair<Double, Integer[]>> mData;

    public Paths() {
        mData = new HashMap<>();
    }

    public Pair<Double, Integer[]> findPath(int start, int end) {
        return mData.get(new Pair<>(start, end));
    }

    public void addPath(int start, int end, double distance, Integer[] path) {
        mData.put(new Pair<>(start, end), new Pair<>(distance, path));
    }

    public void addAll(Paths paths) {
        mData.putAll(paths.mData);
    }
}
