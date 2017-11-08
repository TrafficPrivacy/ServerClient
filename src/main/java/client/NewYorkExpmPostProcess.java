package client;

import util.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static java.lang.System.exit;

public class NewYorkExpmPostProcess implements PostProcess {
    /* Counts how many main paths go through a segment */
    private HashMap<Segment, MutableInt> mSegPathCounter;
    /* Keep track of the segment id */
    private LinkedHashMap<Segment, Integer> mSegID;
    /* Counts the overlaps for current path */
    private LinkedHashMap<Segment, MutableInt> mOverlapCounter;
    /* Out stream for Segment count CSV */
    private FileOutputStream mSegCSVOut;
    /* Out stream for main path overlap CSV */
    private FileOutputStream mPathCSVOut;

    public NewYorkExpmPostProcess(String mainPathCSV, String segmentCSV) {
        mSegPathCounter = new HashMap<>();
        mSegID = new LinkedHashMap<>();
        mOverlapCounter = new LinkedHashMap<>();
        File mainPathFile = new File(mainPathCSV);
        File segmentFile = new File(segmentCSV);
        if (mainPathFile.exists()) {
            Logger.printf(Logger.ERROR, "Target csv %s already exists", mainPathCSV);
            exit(1);
        }
        if (segmentFile.exists()) {
            Logger.printf(Logger.ERROR, "Target csv %s already exists", segmentCSV);
            exit(1);
        }
        try {
            mSegCSVOut = new FileOutputStream(segmentFile);
            mPathCSVOut = new FileOutputStream(mainPathFile);
        } catch (Exception e) {
            e.printStackTrace();
            exit(1);
        }
    }

    @Override
    public void setMainPath(ArrayList<MapPoint> path) throws MainPathEmptyException {
        if (path.size() == 0) {
            throw new MainPathEmptyException();
        }

        MapPoint prev = path.get(0);
        MapPoint curr;
        for (int i = 1; i < path.size(); i++) {
            curr = path.get(i);
            Segment segment = new Segment(prev, curr);
            MutableInt count = mSegPathCounter.get(segment);
            if (count == null) {
                count = new MutableInt();
                mSegPathCounter.put(segment, count);
                mSegID.put(segment, mSegID.size());
            }
            count.increment();

            mOverlapCounter.put(segment, new MutableInt(1));
            prev = curr;
        }
    }

    @Override
    public void addPath(ArrayList<MapPoint> path) throws MainPathEmptyException {
        if (mOverlapCounter.isEmpty()) {
            throw new MainPathEmptyException();
        }
        if (path.size() == 0) {
            return;
        }
        MapPoint prev = path.get(0);
        MapPoint curr;

        for (int i = 1; i < path.size(); i++) {
            curr = path.get(i);
            Segment segment = new Segment(prev, curr);
            MutableInt count = mOverlapCounter.get(segment);
            if (count != null) {
                count.increment();
            }
            prev = curr;
        }
    }

    @Override
    public void done() {
        StringBuilder pathInfoBuilder = new StringBuilder();
        pathInfoBuilder.append(mOverlapCounter.size());
        for (Segment segment : mOverlapCounter.keySet()) {
            int segID = mSegID.get(segment);
            int overlap = mOverlapCounter.get(segment).get();
            pathInfoBuilder.append(",");
            pathInfoBuilder.append(segID);
            pathInfoBuilder.append(",");
            pathInfoBuilder.append(overlap);
        }
        pathInfoBuilder.append("\n");
        try {
            mPathCSVOut.write(pathInfoBuilder.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mOverlapCounter.clear();
    }

    public void finishUp() throws IOException {
        mSegCSVOut.write("ID, Start Lat, Start Lon, End Lat, End Lon, num of paths\n".getBytes());
        int id = 0;
        for (Segment segment : mSegID.keySet()) {
            String segInfo = id + "," + segment.toString() + mSegPathCounter.get(segment).get() + "\n";
            mSegCSVOut.write(segInfo.getBytes());
            id ++;
        }
        mSegCSVOut.close();
        mPathCSVOut.close();
    }

}
