import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.EdgeExplorer;

public class BellmanFord extends S2SStrategy{

    public BellmanFord(EdgeProvider edgeProvider) {
        super(edgeProvider);
    }

    @Override
    public Paths compute(int[] set1, int[] set2) {
        return null;
    }
}
