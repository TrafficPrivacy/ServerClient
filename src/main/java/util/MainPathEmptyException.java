package util;

public class MainPathEmptyException extends ServerClientException {

    public MainPathEmptyException() {
        super("Main path is empty");
    }

}
