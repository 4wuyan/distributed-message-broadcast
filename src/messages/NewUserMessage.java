package messages;

public class NewUserMessage extends Message{
    private String username, secret;

    public NewUserMessage(String username, String secret) {
        super("NEW_USER");
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
