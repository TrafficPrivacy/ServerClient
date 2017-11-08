package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static java.lang.System.exit;

public class FlagParser {
    private HashMap<String, String> mFlags;
    private LinkedHashMap<String, String> mHelps;
    private ArrayList<String> mUnFlagged;
    private ArrayList<Pair<String, String>> mUnflaggedHelps;


    public FlagParser() {
        mFlags = new HashMap<>();
        mHelps = new LinkedHashMap<>();
        mUnFlagged = new ArrayList<>();
        mUnflaggedHelps = new ArrayList<>();
    }

    /**
     * Add a flag. The order of adding will be preserved when printing out help
     * @param flag The flag. starts with "--"
     * @param help The help message
     * @param defaultArg The default value. "" means no default
     */
    public void addFlag(String flag, String help, String defaultArg) {
        if (!defaultArg.equals("")) {
            mFlags.put(flag, defaultArg);
            help = help + " (default: " + defaultArg + ")";
        }
        mHelps.put(flag, help);
    }

    public void addUnflagged(String name, String help) {
        mUnflaggedHelps.add(new Pair<>(name, help));
    }

    public void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--") || args[i] == "-h") {
                if (args[i].equals("--help") || args[i].equals("-h")) {
                    printHelp();
                    exit(0);
                }
                String flag = args[i];
                if ((i + 1) < args.length && !args[i + 1].startsWith("-")) {

                    if (mHelps.containsKey(flag)) {
                        mFlags.put(flag, args[i + 1]);
                    } else {
                        System.err.printf( "Error, unknown flag: %s\n", flag);
                        printHelp();
                        exit(1);
                    }

                    i++;
                }
            } else {
                mUnFlagged.add(args[i]);
            }
        }

        if (mFlags.size() != mHelps.size()) {
            printHelp();
            exit(1);
        }

        if (mUnFlagged.size() != mUnflaggedHelps.size()) {
            System.err.printf("Error: expected %d unflagged parameters, got %d\n",
                    mUnflaggedHelps.size(),
                    mUnFlagged.size());
            printHelp();
            exit(1);
        }
    }

    public String getArg(String flag) throws NoSuchFlagException {
        if (!mFlags.containsKey(flag))
            throw new NoSuchFlagException(flag);
        return mFlags.get(flag);
    }

    /**
     * Printing out the helps will the following order:
     * Unflagged, flagged (in order with addFlag)
     */
    public void printHelp() {
        System.out.print("Arguments: ");
        for (Pair p : mUnflaggedHelps) {
            System.out.print(p.mFirst + " ");
        }
        for (String flag : mHelps.keySet()) {
            System.out.print(flag + " ");
        }
        System.out.println("\n\nDetails:");
        for (Pair p : mUnflaggedHelps) {
            System.out.printf("    %-12s:%s\n", p.mFirst, p.mSecond);
        }
        for (String flag : mHelps.keySet()) {
            System.out.printf("    %-12s:%s\n", flag, mHelps.get(flag));
        }
    }

    public String[] getUnflagged() {
        String[] args = new String[mUnFlagged.size()];
        mUnFlagged.toArray(args);
        return args;
    }
}
