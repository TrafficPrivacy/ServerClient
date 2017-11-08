package util;

public class NoSuchStrategyException extends Exception {

    public NoSuchStrategyException(String strategy) {
        super(strategy);
    }

}
