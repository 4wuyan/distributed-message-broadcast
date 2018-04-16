package activitystreamer.server;
import messages.*;

class LockManager {
    private int approvalsNeeded;
    private String secret;
    private Connection connectionToClient;
    private Message successMessage, failMessage;

    LockManager(String secret, Connection client, int approvalsNeeded,
                Message success, Message fail) {
        this.secret = secret;
        this.connectionToClient = client;
        this.approvalsNeeded = approvalsNeeded;
        this.successMessage = success;
        this.failMessage = fail;
    }

    public void acceptApproval(LockAllowedMessage message) {
        if(message.getSecret().equals(secret)) {
            approvalsNeeded--;
        }
    }

    public boolean allApproved() {
        return approvalsNeeded == 0;
    }

    public void sendFailMessage() {
        connectionToClient.sendMessage(failMessage);
    }

    public void sendSuccessMessage() {
        connectionToClient.sendMessage(successMessage);
    }
}
