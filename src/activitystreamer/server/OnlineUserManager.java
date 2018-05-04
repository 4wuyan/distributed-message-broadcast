package activitystreamer.server;

import java.util.HashMap;
import java.util.HashSet;

class OnlineUserManager {
    private HashMap<Connection, String> connectionToUsername;
    private HashMap<String, HashSet<Connection>> usernameToConnections;

    OnlineUserManager() {
        connectionToUsername = new HashMap<>();
        usernameToConnections = new HashMap<>();
    }

    public void login(String username, Connection connection) {
        connectionToUsername.put(connection, username);

        if(! usernameToConnections.containsKey(username)) {
            usernameToConnections.put(username, new HashSet<>());
        }
        usernameToConnections.get(username).add(connection);
    }

    public void logout(String username) {
        if (usernameToConnections.containsKey(username)) {
            HashSet<Connection> connections = usernameToConnections.get(username);
            usernameToConnections.remove(username);

            for (Connection connection : connections) {
                connectionToUsername.remove(connection);
                connection.closeCon();
            }
        }
    }

    public void remove(Connection connection) {
        if (connectionToUsername.containsKey(connection)) {
            String username = connectionToUsername.get(connection);
            connectionToUsername.remove(connection);

            HashSet<Connection> connections = usernameToConnections.get(username);
            connections.remove(connection);
            if (connections.isEmpty()) {
                usernameToConnections.remove(username);
            }
        }
    }
}
