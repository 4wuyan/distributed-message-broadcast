package messages;

public class InvalidMessageMessage extends Message{
    private String info;

    public InvalidMessageMessage(String info) {
        super("INVALID_MESSAGE");
        this.info = info;
    }
}
