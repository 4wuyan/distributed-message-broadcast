package messages;

public class RegisterSuccessMessage extends Message {
    private String info;

    public RegisterSuccessMessage(String info) {
        super("REGISTER_SUCCESS");
        this.info = info;
    }
}
