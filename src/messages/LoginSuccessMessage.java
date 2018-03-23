package messages;

public class LoginSuccessMessage extends Message {
    private String info;

    public LoginSuccessMessage(String info) {
        super("LOGIN_SUCCESS");
        this.info = info;
    }
}
