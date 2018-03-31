package activitystreamer.server;

import messages.Message;

import java.util.HashSet;

class LockManager {
    private HashSet<Connection> waitForApproval;
    private Connection upstream;
    private Message successMessage, failedMessage;

    LockManager(HashSet<Connection> connections, Connection upstream,
        Message successMessage) {
        this.upstream = upstream;
        this.successMessage = successMessage;
        this.failedMessage = null;

        waitForApproval = new HashSet<>();
        for(Connection connection : connections) {
            if(connection != upstream)
                waitForApproval.add(connection);
        }
    }

    public boolean allApproved() {
        return waitForApproval.isEmpty();
    }

    public void addApproval(Connection connection) {
        waitForApproval.remove(connection);
    }

    public void sendFailMessage() {
        if (failedMessage != null) upstream.sendMessage(failedMessage);
    }

    public void sendSuccessMessage() {
        upstream.sendMessage(successMessage);
    }

    public void setFailedMessage(Message failedMessage) {
        this.failedMessage = failedMessage;
    }
}
