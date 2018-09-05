package util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Log formatted message to STDOUT
 */
public class Logger {

  private static int mVerboseLevel = 4;

  private static final String FORMAT = "%-5s:[%15s:%-4d] -- ";
  private static final String[] TYPES = {"ERROR", "WARN", "INFO", "DEBUG", "USER"};

  public static final int ERROR = 0;
  public static final int WARN = 1;
  public static final int INFO = 2;
  public static final int DEBUG = 3;

  /**
   * Initialize the logger with the verbose level.
   *
   * @param verboseLevel Default is 4
   */
  public static void initialize(int verboseLevel) {
    mVerboseLevel = verboseLevel;
  }

  private static void formatPrint(int level, String format, Object... vars) {
    if (level < mVerboseLevel) {
      StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
      if (level > 3) {
        level = 4;
      }
      ArrayList<Object> args = new ArrayList<>(Arrays.asList(TYPES[level],
          stackTraceElement.getFileName(),
          stackTraceElement.getLineNumber()
      ));
      args.addAll(Arrays.asList(vars));
      System.out.printf(FORMAT + format, args.toArray());
    }
  }

  /**
   * Print the log message and a new line
   *
   * @param level Integer representing the log level. If higher than 3 will be printed as USER
   * @param msg Log message
   */
  public static void println(int level, String msg) {
    formatPrint(level, "%s\n", msg);
  }

  /**
   * Print log with format
   *
   * @param level Integer representing the log level. If higher than 3 will be printed as USER
   * @param format Format string. Rules are the same as in System.out.printf
   * @param vars parameters
   */
  public static void printf(int level, String format, Object... vars) {
    formatPrint(level, format, vars);
  }
}
