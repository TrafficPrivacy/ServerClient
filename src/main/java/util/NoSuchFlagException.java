package util;

public class NoSuchFlagException extends ServerClientException {

    public NoSuchFlagException(String flag) {
        super("No such flag: " + flag);
    }

}
