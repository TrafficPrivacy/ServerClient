import java.io.Serializable;

public class Reply implements Serializable{
    private final Pair<int[], int[]> mSrcCircle;
    private final Pair<int[], int[]> mDestCircle;
    private final Paths srcPaths;
    private final Paths destPaths;
    private final Paths interPaths;

    public Reply(Pair<int[], int[]> mSrcCircle, Pair<int[], int[]> mDestCircle, Paths srcPaths, Paths destPaths, Paths interPaths) {
        this.mSrcCircle = mSrcCircle;
        this.mDestCircle = mDestCircle;
        this.srcPaths = srcPaths;
        this.destPaths = destPaths;
        this.interPaths = interPaths;
    }
}

