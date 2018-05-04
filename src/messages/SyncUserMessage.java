package messages;

import java.util.HashMap;

public class SyncUserMessage extends Message {
    private HashMap<String, String> users;

    public SyncUserMessage(HashMap<String,String> users, boolean override) {
        super("SYNC_USER");
        this.users = new HashMap<>(users);
    }

    public HashMap<String, String> getUsers() {
        return new HashMap<>(users);
    }
}
