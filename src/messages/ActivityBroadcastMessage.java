package messages;

import org.json.simple.JSONObject;

public class ActivityBroadcastMessage extends Message {
    private JSONObject activity;
    private String id;

    public ActivityBroadcastMessage(JSONObject activity, String id) {
        super("ACTIVITY_BROADCAST");
        this.activity = activity;
        this.id = id;
    }

    public JSONObject getActivity() {
        return activity;
    }

    public String getId() {
        return id;
    }
}
