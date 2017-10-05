package util;

import java.io.Serializable;

public class Reply implements Serializable {
    public final Pair<int[], int[]> mSrcCircle;
    public final Pair<int[], int[]> mDestCircle;
    public final Paths mSrcPaths;
    public final Paths mDestPaths;
    public final Paths mInterPaths;
    public final MapPoint[] mSrcReference;
    public final MapPoint[] mDestReference;

    public Reply(Pair<int[], int[]> srcCircle, Pair<int[], int[]> destCircle, Paths srcPaths, Paths destPaths,
                 Paths interPaths, MapPoint[] srcRef, MapPoint[] destRef) {
        mSrcCircle = srcCircle;
        mDestCircle = destCircle;
        mSrcPaths = srcPaths;
        mDestPaths = destPaths;
        mInterPaths = interPaths;
        mSrcReference = srcRef;
        mDestReference = destRef;
    }
}
