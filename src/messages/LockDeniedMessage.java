package messages;

public class LockDeniedMessage extends Message {
    private String username, secret;

    public LockDeniedMessage(String username, String secret) {
        super("LOCK_DENIED");
        this.username = username;
        this.secret = secret;
    }
}