import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.EdgeExplorer;

/*TODO: finish up the algorithm*/
public class FloydWarshall extends S2SStrategy{

    public FloydWarshall(EdgeProvider edgeProvider) {
        super(edgeProvider);
    }

    @Override
    public Paths compute(int[] set1, int[] set2) {
        return null;
    }
}
