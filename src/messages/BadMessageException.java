package messages;

public class BadMessageException extends Exception {
    public BadMessageException() {
        super("The message you received is not legal.");
    }
}
