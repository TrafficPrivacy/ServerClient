package util;

import com.graphhopper.util.shapes.GHPoint;
import org.mapsforge.core.model.LatLong;

import java.util.ArrayList;

public class MapPoint extends Pair<Double, Double>{
    public MapPoint(Double first, Double second) {
        super(first, second);
    }

    public MapPoint(LatLong latLong) {
        this(latLong.getLatitude(), latLong.getLatitude());
    }

    public MapPoint(GHPoint ghPoint) {
        this(ghPoint.getLat(), ghPoint.getLon());
    }

    public static ArrayList<MapPoint> convertFromLatlong(ArrayList<LatLong> input) {
        ArrayList<MapPoint> result = new ArrayList<>();
        for (LatLong latLong : input) {
            result.add(new MapPoint(latLong.getLatitude(), latLong.getLongitude()));
        }
        return result;
    }

    public static ArrayList<MapPoint> convertFromGHPoint(ArrayList<GHPoint> input) {
        ArrayList<MapPoint> result = new ArrayList<>();
        for (GHPoint ghPoint : input) {
            result.add(new MapPoint(ghPoint.getLat(), ghPoint.getLon()));
        }
        return result;
    }

    public static ArrayList<LatLong> convertToLatlong(ArrayList<MapPoint> input) {
        ArrayList<LatLong> result = new ArrayList<>();
        for (MapPoint mapPoint : input) {
            result.add(new LatLong(mapPoint.mFirst, mapPoint.mSecond));
        }
        return result;
    }

    public static ArrayList<GHPoint> convertToGHPoint(ArrayList<MapPoint> input) {
        ArrayList<GHPoint> result = new ArrayList<>();
        for (MapPoint mapPoint : input) {
            result.add(new GHPoint(mapPoint.mFirst, mapPoint.mSecond));
        }
        return result;
    }

    public static class GHAdapter extends GHPoint {
        public GHAdapter(MapPoint mapPoint) {
            super(mapPoint.mFirst, mapPoint.mSecond);
        }
    }

    public static class LatLongAdapter extends LatLong {
        public LatLongAdapter(MapPoint mapPoint) {
            super(mapPoint.mFirst, mapPoint.mSecond);
        }
    }
}