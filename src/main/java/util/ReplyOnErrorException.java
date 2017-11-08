package util;

public class ReplyOnErrorException extends ServerClientException {

    public ReplyOnErrorException() {
        super("Reply has error");
    }

}
