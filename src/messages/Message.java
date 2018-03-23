package messages;

import com.google.gson.Gson;
public abstract class Message {
    private String command;

    public Message(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
