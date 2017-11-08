package util;

public class NoSuchFlagException extends Exception {

    public NoSuchFlagException(String flag) {
        super("No such flag: " + flag);
    }

}
