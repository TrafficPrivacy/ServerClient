package util;

/**
 * Profiler to measure running time in a block of code
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
   *
   * @param logLevel Appropriate log level
   */
  public Profiler(int logLevel) {
    mLogLevel = logLevel;
  }

  public Profiler start() {
    mStart = System.nanoTime();
    mBeginCall = Thread.currentThread().getStackTrace()[2];
    return this;
  }

  public long timeElapsed() {
    return mEnd - mStart;
  }

  public Profiler endAndPrint() {
    mEnd = System.nanoTime();
    mEndCall = Thread.currentThread().getStackTrace()[2];
    if (!mEndCall.getMethodName().equals(mBeginCall.getMethodName())) {
      Logger.printf(Logger.WARN, "start() and end() should be called in the same method. " +
              "Profile started at: %s: %d and ends at: %s: %d\n",
          mBeginCall.getFileName(), mBeginCall.getLineNumber(),
          mEndCall.getFileName(), mEndCall.getLineNumber());
    }
    print();
    return this;
  }

  public Profiler print() {
    Logger.printf(mLogLevel, "Start:[%15s:%-3d] End:[%15s: %-3d]Total Time: %-13d ns\n",
        mBeginCall.getFileName(), mBeginCall.getLineNumber(),
        mEndCall.getFileName(), mEndCall.getLineNumber(),
        timeElapsed());
    return this;
  }

}
