package messages;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    public static Message getMessageFromJson(String jsonString) throws BadMessageException{
        JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        String command = jsonObject.get("command").getAsString();

        Gson gson = new Gson();
        if (command.equals("ACTIVITY_BROADCAST")) {
            return gson.fromJson(jsonString, ActivityBroadcastMessage.class);
        }
        else if (command.equals("ACTIVITY_MESSAGE")) {
            return gson.fromJson(jsonString, ActivityMessage.class);
        }
        else if (command.equals("AUTHENTICATE")) {
            return gson.fromJson(jsonString, AuthenticateMessage.class);
        }
        else if (command.equals("AUTHENTICATION_FAIL")) {
            return gson.fromJson(jsonString, AuthenticationFailMessage.class);
        }
        else if (command.equals("INVALID_MESSAGE")) {
            return gson.fromJson(jsonString, InvalidMessageMessage.class);
        }
        else if (command.equals("LOCK_ALLOWED")) {
            return gson.fromJson(jsonString, LockAllowedMessage.class);
        }
        else if (command.equals("LOCK_DENIED")) {
            return gson.fromJson(jsonString, LockDeniedMessage.class);
        }
        else if (command.equals("LOCK_REQUEST")) {
            return gson.fromJson(jsonString, LockRequestMessage.class);
        }
        else if (command.equals("LOGIN_FAILED")) {
            return gson.fromJson(jsonString, LoginFailedMessage.class);
        }
        else if (command.equals("LOGIN")) {
            return gson.fromJson(jsonString, LoginMessage.class);
        }
        else if (command.equals("LOGIN_SUCCESS")) {
            return gson.fromJson(jsonString, LoginSuccessMessage.class);
        }
        else if (command.equals("LOGOUT")) {
            return gson.fromJson(jsonString, LogoutMessage.class);
        }
        else if (command.equals("REDIRECT")) {
            return gson.fromJson(jsonString, RedirectMessage.class);
        }
        else if (command.equals("REGISTER_FAILED")) {
            return gson.fromJson(jsonString, RegisterFailedMessage.class);
        }
        else if (command.equals("REGISTER")) {
            return gson.fromJson(jsonString, RegisterMessage.class);
        }
        else if (command.equals("REGISTER_SUCCESS")) {
            return gson.fromJson(jsonString, RegisterSuccessMessage.class);
        }
        else if (command.equals("SERVER_ANNOUNCE")) {
            return gson.fromJson(jsonString, ServerAnnounceMessage.class);
        }
        else {
            throw new BadMessageException();
        }
    }
}
