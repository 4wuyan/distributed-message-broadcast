package messages;

import org.json.simple.JSONObject;

public class ActivityBroadcastMessage extends Message {
    private JSONObject activity;

    public ActivityBroadcastMessage(JSONObject activity) {
        super("ACTIVITY_BROADCAST");
        this.activity = activity;
    }

    public JSONObject getActivity() {
        return activity;
    }
}
