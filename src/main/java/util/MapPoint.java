package util;

import com.graphhopper.util.shapes.GHPoint;
import org.mapsforge.core.model.LatLong;

import java.util.ArrayList;

public class MapPoint extends Pair<Double, Double> {

  public MapPoint(Double latitude, Double longitude) {
    super(latitude, longitude);
  }

  public MapPoint(LatLong latLong) {
    this(latLong.getLatitude(), latLong.getLatitude());
  }

  public MapPoint(GHPoint ghPoint) {
    this(ghPoint.getLat(), ghPoint.getLon());
  }

  public double getLat() {
    return mFirst;
  }

  public double getLon() {
    return mSecond;
  }

  public String toString() {
    return mFirst + "," + mSecond;
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
      result.add(new LatLongAdapter(mapPoint));
    }
    return result;
  }

  public static ArrayList<GHPoint> convertToGHPoint(ArrayList<MapPoint> input) {
    ArrayList<GHPoint> result = new ArrayList<>();
    for (MapPoint mapPoint : input) {
      result.add(new GHPointAdapter(mapPoint));
    }
    return result;
  }

  public static class GHPointAdapter extends GHPoint {

    public GHPointAdapter(MapPoint mapPoint) {
      super(mapPoint.mFirst, mapPoint.mSecond);
    }
  }

  public static class LatLongAdapter extends LatLong {

    public LatLongAdapter(MapPoint mapPoint) {
      super(mapPoint.mFirst, mapPoint.mSecond);
    }
  }
}