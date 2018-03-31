package messages;

public class LockAllowedMessage extends Message {
    private String username, secret;

    public LockAllowedMessage(String username, String secret) {
        super("LOCK_ALLOWED");
        this.username = username;
        this.secret = secret;
    }

    public String getUsername() {
        return username;
    }
}
