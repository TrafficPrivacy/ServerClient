package util;

public class NoEdgeIteratorException extends ServerClientException {

  public NoEdgeIteratorException() {
    super("No edge provider provided");
  }

}
