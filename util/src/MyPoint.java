import com.graphhopper.util.shapes.GHPoint;
import org.mapsforge.core.model.LatLong;
import sun.misc.resources.Messages_pt_BR;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class MyPoint extends Pair<Double, Double>{
    public MyPoint(Double first, Double second) {
        super(first, second);
    }

    public MyPoint(LatLong latLong) {
        this(latLong.getLatitude(), latLong.getLatitude());
    }

    public MyPoint(GHPoint ghPoint) {
        this(ghPoint.getLat(), ghPoint.getLon());
    }

    public static ArrayList<MyPoint> convertFromLatlong(ArrayList<LatLong> input) {
        return (ArrayList<MyPoint>) (input.stream().map(i -> new MyPoint(i)).collect(Collectors.toList()));
    }

    public static ArrayList<MyPoint> convertFromGHPoint(ArrayList<GHPoint> input) {
        return (ArrayList<MyPoint>) (input.stream().map(i -> new MyPoint(i)).collect(Collectors.toList()));
    }

    public static ArrayList<LatLong> convertToLatlong(ArrayList<MyPoint> input) {
        return (ArrayList<LatLong>) (input.stream().map(i -> (LatLong)new LatLongAdapter(i)).collect(Collectors.toList()));
    }

    public static ArrayList<GHPoint> convertToGHPoint(ArrayList<MyPoint> input) {
        return (ArrayList<GHPoint>) (input.stream().map(i -> (GHPoint)new GHAdapter(i)).collect(Collectors.toList()));
    }

    public static class GHAdapter extends GHPoint {
        public GHAdapter(MyPoint myPoint) {
            super(myPoint.mFirst, myPoint.mSecond);
        }
    }

    public static class LatLongAdapter extends LatLong {
        public LatLongAdapter(MyPoint myPoint) {
            super(myPoint.mFirst, myPoint.mSecond);
        }
    }
}
