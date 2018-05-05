package messages;

public class ServerAnnounceMessage extends Message {
    private String hostname;
    private int load, port;

    public ServerAnnounceMessage(int load, String hostname, int port) {
        super("SERVER_ANNOUNCE");
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
}
