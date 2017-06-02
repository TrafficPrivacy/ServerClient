import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.EdgeExplorer;

public class FloydWarshall extends S2SStrategy{

    public FloydWarshall(EdgeExplorer edgeExplorer, Weighting weighting) {
        super(edgeExplorer, weighting);
    }

    @Override
    public Paths compute(int[] set1, int[] set2) {
        return null;
    }
}
