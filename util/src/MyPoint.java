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
        ArrayList<MyPoint> result = new ArrayList<>();
        for (LatLong latLong : input) {
            result.add(new MyPoint(latLong.getLatitude(), latLong.getLongitude()));
        }
        return result;
    }

    public static ArrayList<MyPoint> convertFromGHPoint(ArrayList<GHPoint> input) {
        ArrayList<MyPoint> result = new ArrayList<>();
        for (GHPoint ghPoint : input) {
            result.add(new MyPoint(ghPoint.getLat(), ghPoint.getLon()));
        }
        return result;
    }

    public static ArrayList<LatLong> convertToLatlong(ArrayList<MyPoint> input) {
        ArrayList<LatLong> result = new ArrayList<>();
        for (MyPoint myPoint : input) {
            result.add(new LatLong(myPoint.mFirst, myPoint.mSecond));
        }
        return result;
    }

    public static ArrayList<GHPoint> convertToGHPoint(ArrayList<MyPoint> input) {
        ArrayList<GHPoint> result = new ArrayList<>();
        for (MyPoint myPoint : input) {
            result.add(new GHPoint(myPoint.mFirst, myPoint.mSecond));
        }
        return result;
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
