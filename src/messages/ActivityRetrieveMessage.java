package messages;

public class ActivityRetrieveMessage extends Message {
    private String after;

    public ActivityRetrieveMessage(String messageId) {
        super("ACTIVITY_RETRIEVE");
        after = messageId;
    }

    public String getAfter() {
        return after;
    }
}
