package messages;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedList;
import java.util.List;

public class BundleMessage extends Message {
    private LinkedList<JsonObject> messages;

    public BundleMessage(List<? extends Message> messages) {
        super("BUNDLE");
        this.messages = new LinkedList<>();
        for (Message m: messages) {
            JsonObject json = new JsonParser().parse(m.toString()).getAsJsonObject();
            this.messages.add(json);
        }
    }

    public LinkedList<String> getMessages() {
        LinkedList<String> answer = new LinkedList<>();
        for (JsonObject message : messages) {
            answer.add(message.toString());
        }
        return answer;
    }
}
