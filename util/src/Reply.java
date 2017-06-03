import java.io.Serializable;

public class Reply implements Serializable{
    public final Pair<int[], int[]> mSrcCircle;
    public final Pair<int[], int[]> mDestCircle;
    public final Paths srcPaths;
    public final Paths destPaths;
    public final Paths interPaths;

    public Reply(Pair<int[], int[]> mSrcCircle, Pair<int[], int[]> mDestCircle, Paths srcPaths, Paths destPaths, Paths interPaths) {
        this.mSrcCircle = mSrcCircle;
        this.mDestCircle = mDestCircle;
        this.srcPaths = srcPaths;
        this.destPaths = destPaths;
        this.interPaths = interPaths;
    }
}

