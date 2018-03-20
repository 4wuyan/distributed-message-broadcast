package messages;

public class InvalidMessage extends Message{
    private String info;

    public InvalidMessage(String info) {
        super("INVALID_MESSAGE");
        this.info = info;
    }
}
