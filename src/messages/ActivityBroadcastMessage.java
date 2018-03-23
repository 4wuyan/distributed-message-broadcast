package messages;

public class ActivityBroadcastMessage extends Message {
    private Activity activity;

    public ActivityBroadcastMessage(Activity activity) {
        super("ACTIVITY_BROADCAST");
        this.activity = activity;
    }
}
