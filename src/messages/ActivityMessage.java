package messages;

public class ActivityMessage extends Message{
    private String username, secret;
    private Activity activity;

    public ActivityMessage (String username, String secret, Activity activity) {
        super("ACTIVITY_MESSAGE");
        this.username = username;
        this.secret = secret;
        this.activity = activity;
    }
}
