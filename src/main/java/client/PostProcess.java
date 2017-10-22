package client;

import util.MapPoint;

import java.util.ArrayList;

public interface PostProcess {

    void setMainPath(ArrayList<MapPoint> path);

    void addPath(ArrayList<MapPoint> path);

    void done();

}
