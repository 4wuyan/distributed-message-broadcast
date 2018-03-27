package messages;

public class LoginMessage extends Message{
    private String username, secret;

    public LoginMessage(String username, String secret) {
        super("LOGIN");
        this.username = username;
        this.secret = secret;
    }

    public LoginMessage() {
        super("LOGIN");
        this.username = "anonymous";
        this.secret = null;
    }

    public String getUsername() {
        return username;
    }
    public String getSecret() {
        return secret;
    }
}
