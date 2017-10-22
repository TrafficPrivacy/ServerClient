package client;

import util.Logger;
import util.MapPoint;
import util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static java.lang.System.exit;

public class OverLapCounter implements PostProcess{
    private String mCsvPath;
    private LinkedHashMap<Pair<MapPoint, MapPoint>, Integer> mMainPathCount;  // for quick check of existence and count

    public OverLapCounter(String csvPath) {
        mCsvPath = csvPath;
        mMainPathCount = new LinkedHashMap<>();
    }

    @Override
    public void setMainPath(ArrayList<MapPoint> path) {
        if (path.size() > 0) {
            MapPoint prev = path.get(0);
            MapPoint curt;
            for (int i = 1; i < path.size(); i++) {
                curt = path.get(i);
                mMainPathCount.put(new Pair<>(prev, curt), 1);
                prev = curt;
            }
        }
    }

    @Override
    public void addPath(ArrayList<MapPoint> path) {
        if (path.size() > 0 && !mMainPathCount.isEmpty()) {
            MapPoint prev = path.get(0);
            MapPoint curt;
            for (int i = 1; i < path.size(); i++) {
                curt = path.get(i);
                Pair segment = new Pair<>(prev, curt);
                if (mMainPathCount.containsKey(segment)) {
                    int curCount = mMainPathCount.get(segment);
                    mMainPathCount.replace(segment, curCount + 1);
                }
                prev = curt;
            }
        }
    }

    @Override
    public void done() {
        File file = new File(mCsvPath);
        if (file.exists()) {
            Logger.printf(Logger.ERROR, "Target csv %s already exists", mCsvPath);
            exit(1);
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write("from Lat, from Lon, to Lat, to Lon, overlap\n".getBytes());
            for (Pair<MapPoint, MapPoint> segment : mMainPathCount.keySet()) {
                int count = mMainPathCount.get(segment);
                MapPoint from = segment.mFirst;
                MapPoint to = segment.mSecond;
                fos.write((from.getLat() + "," + from.getLon() + ",").getBytes());
                fos.write((to.getLat() + "," + to.getLon() + ",").getBytes());
                fos.write((count + "\n").getBytes());
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
