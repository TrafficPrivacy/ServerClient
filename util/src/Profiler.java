/**
 *  Profiler to measure running time in a block of code
 */
public class Profiler {
    private int mLogLevel;
    private long mStart;
    private long mEnd;
    private StackTraceElement mBeginCall;
    private StackTraceElement mEndCall;

    /**
     * Create a profiler with log level of DEBUG
     */
    public Profiler() {
        this(Logger.DEBUG);
    }

    /**
     * Create a profiler with specified log level
     * @param logLevel Appropriate log level
     */
    public Profiler(int logLevel) {
        mLogLevel = logLevel;
    }

    public void start() {
        mStart = System.nanoTime();
        mBeginCall = Thread.currentThread().getStackTrace()[2];
    }

    public long timeElapsed() {
        return mEnd - mStart;
    }

    public void endAndPrint() {
        mEnd = System.nanoTime();
        mEndCall = Thread.currentThread().getStackTrace()[2];
        if (!mEndCall.getMethodName().equals(mBeginCall.getMethodName())) {
            Logger.printf(Logger.WARN, "start() and end() should be called in the same method. " +
                    "Profile started at: %s: %d and ends at: %s: %d\n",
                    mBeginCall.getFileName(), mBeginCall.getLineNumber(),
                    mEndCall.getFileName(), mEndCall.getLineNumber());
        }
        print();
    }

    public void print() {
        Logger.printf(mLogLevel, "Start: %20s: %-6d End: %20s: %-6d Total Time: %-15d ns\n",
                mBeginCall.getFileName(), mBeginCall.getLineNumber(),
                mEndCall.getFileName(), mEndCall.getLineNumber(),
                timeElapsed());
    }

}
