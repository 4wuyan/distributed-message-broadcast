package messages;

import com.google.gson.Gson;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public abstract class Message {
    private String command;

    public Message(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public JSONObject toJSONObject() {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) parser.parse(toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
