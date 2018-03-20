package messages;

public class ServerAnnounceMessage extends Message {
    private String id, hostname;
    private int load, port;

    public ServerAnnounceMessage(String id, int load, String hostname, int port) {
        super("SERVER_ANNOUNCE");
        this.id = id;
        this.load = load;
        this.hostname = hostname;
        this.port = port;
    }
}
