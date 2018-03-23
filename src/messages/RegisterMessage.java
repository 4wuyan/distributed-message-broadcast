package messages;

public class RegisterMessage extends Message {
    private String username, secret;

    public RegisterMessage(String username, String secret) {
        super("REGISTER");
        this.username = username;
        this.secret = secret;
    }
}
