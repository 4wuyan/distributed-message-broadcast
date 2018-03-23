package messages;

public class RegisterFailedMessage extends Message {
    private String info;

    public RegisterFailedMessage(String info) {
        super("REGISTER_FAILED");
        this.info = info;
    }
}
