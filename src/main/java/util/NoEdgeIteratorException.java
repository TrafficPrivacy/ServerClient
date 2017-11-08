package util;

public class NoEdgeIteratorException extends Exception {

    public NoEdgeIteratorException() {
        super("No edge provider provided");
    }

}
