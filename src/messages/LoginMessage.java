package messages;

public class LoginMessage extends Message{
    private String username, secret;

    public LoginMessage(String username, String secret) {
        super("LOGIN");
        this.username = username;
        this.secret = secret;
    }
}
