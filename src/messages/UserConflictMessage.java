package messages;

public class UserConflictMessage extends Message {
    private String username;

    public UserConflictMessage(String username) {
        super("USER_CONFLICT");
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
