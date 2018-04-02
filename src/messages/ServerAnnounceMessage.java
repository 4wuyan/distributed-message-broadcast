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

    public int getLoad() {
        return load;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return id;
    }
}
