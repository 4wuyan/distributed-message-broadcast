package messages;

public class RedirectMessage extends Message{
    private String hostname;
    private int port;

    public RedirectMessage(String hostname, int port) {
        super("REDIRECT");
        this.hostname = hostname;
        this.port = port;
    }
}
