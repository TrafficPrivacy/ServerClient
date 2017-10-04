package util;

import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.System.exit;

public class FlagParser {
    private HashMap<String, String> mFlags;
    private HashMap<String, String> mHelps;
    private ArrayList<String> mUnFlagged;
    private ArrayList<Pair<String, String>> mUnflaggedHelps;


    public FlagParser() {
        mFlags = new HashMap<>();
        mHelps = new HashMap<>();
        mUnFlagged = new ArrayList<>();
        mUnflaggedHelps = new ArrayList<>();
    }

    public void addFlag(String flag, String help, String defaultArg) {
        mFlags.put(flag, defaultArg);
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

                    if (mFlags.containsKey(flag)) {
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

        if (mUnFlagged.size() != mUnflaggedHelps.size()) {
            System.err.printf("Error: expected %d unflagged parameters, got %d\n",
                    mUnflaggedHelps.size(),
                    mUnFlagged.size());
            printHelp();
            exit(1);
        }
    }

    public String getArg(String flag) {
        if (!mFlags.containsKey(flag))
            return null;
        return mFlags.get(flag);
    }

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
