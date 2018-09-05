package util;

public class PointNotFoundException extends ServerClientException {

  public PointNotFoundException(MapPoint point) {
    super(point.toString() + " not found");
  }

}
