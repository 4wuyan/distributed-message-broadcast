package messages;

import org.json.simple.JSONObject;

public class ActivityMessageMessage extends Message{
    private String username, secret;
    private JSONObject activity;

    public ActivityMessageMessage(String username, String secret, JSONObject activity) {
        super("ACTIVITY_MESSAGE");
        this.username = username;
        this.secret = secret;
        this.activity = activity;
    }

    public String getUsername() {
        return username;
    }

    public String getSecret() {
        return secret;
    }

    public JSONObject getActivity() {
        return activity;
    }
}
