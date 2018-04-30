package messages;

import java.util.HashMap;

public class SyncUserMessage extends Message {
    private HashMap<String, String> users;
    private boolean override;

    public SyncUserMessage(HashMap<String,String> users, boolean override) {
        super("SYNC_USER");
        this.users = new HashMap<>(users);
        this.override = override;
    }

    public HashMap<String, String> getUsers() {
        return new HashMap<>(users);
    }

    public boolean isOverride() {
        return override;
    }
}
