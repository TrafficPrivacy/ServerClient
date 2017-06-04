import com.graphhopper.util.shapes.GHPoint;

public class MyPoint extends Pair<Double, Double>{
    public MyPoint(Double first, Double second) {
        super(first, second);
    }

    public static class GHAdapter extends GHPoint {
        public GHAdapter(MyPoint myPoint) {
            super(myPoint.mFirst, myPoint.mSecond);
        }
    }
}
