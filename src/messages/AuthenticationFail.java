package messages;

public class AuthenticationFail extends Message {
    private String info;

    public AuthenticationFail(String info) {
        super("AUTHENTICATION_FAIL");
        this.info = info;
    }

}
