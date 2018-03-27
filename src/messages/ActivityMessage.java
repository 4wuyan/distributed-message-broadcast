package messages;

import org.json.simple.JSONObject;

public class ActivityMessage extends Message{
    private String username, secret;
    private JSONObject activity;

    public ActivityMessage (String username, String secret, JSONObject activity) {
        super("ACTIVITY_MESSAGE");
        this.username = username;
        this.secret = secret;
        this.activity = activity;
    }
}
