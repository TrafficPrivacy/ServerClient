package client;

import util.MainPathEmptyException;
import util.MapPoint;

import java.util.ArrayList;

public interface PostProcess {

  void setMainPath(ArrayList<MapPoint> path) throws MainPathEmptyException;

  void addPath(ArrayList<MapPoint> path) throws MainPathEmptyException;

  void done();

}
