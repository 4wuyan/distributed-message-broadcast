package messages;

public class LoginFailedMessage extends Message {
    private String info;

    public LoginFailedMessage(String info) {
        super("LOGIN_FAILED");
        this.info = info;
    }
}
