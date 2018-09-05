package util;

public class NoSuchStrategyException extends ServerClientException {

  public NoSuchStrategyException(String strategy) {
    super(strategy);
  }

}
