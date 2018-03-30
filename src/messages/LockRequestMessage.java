package messages;

public class LockRequestMessage extends Message {
    private String username, secret;

    public LockRequestMessage(String username, String secret) {
        super("LOCK_REQUEST");
        this.username = username;
        this.secret = secret;
    }

    public String getSecret() {
        return secret;
    }

    public String getUsername() {
        return username;
    }
}
