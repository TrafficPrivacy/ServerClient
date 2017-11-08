package util;

public class PointNotFoundException extends Exception {

    public PointNotFoundException(MapPoint point) {
        super(point.toString() + " not found");
    }

}
