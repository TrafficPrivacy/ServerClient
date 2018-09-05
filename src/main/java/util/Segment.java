package util;

public class Segment extends Pair<MapPoint, MapPoint> {

  public Segment(MapPoint first, MapPoint second) {
    super(first, second);
  }

  public MapPoint getStart() {
    return mFirst;
  }

  public MapPoint getEnd() {
    return mSecond;
  }

  public String toString() {
    return mFirst.toString() + "," + mSecond.toString();
  }

}
