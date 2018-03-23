package messages;

public class AuthenticationFailMessage extends Message {
    private String info;

    public AuthenticationFailMessage(String info) {
        super("AUTHENTICATION_FAIL");
        this.info = info;
    }

}
