package activitystreamer.server;


class ServerStatus {
    private int load;
    private String hostname;
    private int port;

    ServerStatus(int load, String hostname, int port) {
        this.load = load;
        this.hostname = hostname;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getHostname() {
        return hostname;
    }

    public int getLoad() {
        return load;
    }
}
