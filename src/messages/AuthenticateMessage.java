package messages;

public class AuthenticateMessage extends Message {
    private String secret;
    public AuthenticateMessage(String secret) {
        super("AUTHENTICATE");
        this.secret = secret;
    }

    public String getSecret() {
        return secret;
    }
}
