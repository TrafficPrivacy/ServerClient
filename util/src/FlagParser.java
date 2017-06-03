import java.util.HashMap;

import static java.lang.System.exit;

public class FlagParser {
    private HashMap<String, String> mFlags;
    private HashMap<String, String> mHelps;


    public FlagParser() {
        mFlags = new HashMap<>();
        mHelps = new HashMap<>();
    }

    public void addFlag(String flag, String help, String defaultArg) {
        mFlags.put(flag, defaultArg);
        mHelps.put(flag, help);
    }

    public void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = "";
            if (args[i].startsWith("-")) {
                String flag = args[i];
                if ((i + 1) < args.length && !args[i + 1].startsWith("-")) {
                    arg = args[i + 1];
                    i ++;
                }
                mFlags.put(flag, arg);
            } else {
                printHelp();
                exit(1);
            }
        }

        if (mFlags.size() < mHelps.size()) {
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
}