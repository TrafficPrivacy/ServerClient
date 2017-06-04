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
            if (args[i].startsWith("-")) {
                if (args[i].equals("--help") || args[i].equals("-h")) {
                    printHelp();
                    exit(0);
                }
                String flag = args[i];
                if ((i + 1) < args.length && !args[i + 1].startsWith("-")) {

                    System.out.printf("flag: %s, arg: %s\n", flag, args[i + 1]);

                    if (mFlags.containsKey(flag)) {
                        mFlags.put(flag, args[i + 1]);
                    } else {
                        printHelp();
                        exit(1);
                    }

                    i++;
                }
            } else {
                System.out.println("unflagged: " + args[i]);
                mUnFlagged.add(args[i]);
            }
        }

        if (mUnFlagged.size() != mUnflaggedHelps.size()) {
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
        System.out.println("Argument incorrect");
    }

    public String[] getUnflagged() {
        String[] args = new String[mUnFlagged.size()];
        mUnFlagged.toArray(args);
        return args;
    }
}