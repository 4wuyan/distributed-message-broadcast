package activitystreamer.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class Heartbeat extends Thread {
    private static final Logger log = LogManager.getLogger();
    private Connection connection;
    private int timeoutCount;

    Heartbeat(Connection connection) {
        this.connection = connection;
        timeoutCount = 0;
    }

    @Override
    public void run() {
        while (connection.isOpen()) {
            connection.sendHeartbeat();

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                log.debug("Heartbeat thread interrupted");
            }

            timeoutCount++;
            int timeout = 3;
            if (timeoutCount == timeout) {
                log.info("connection heartbeat timeout...");
                connection.closeCon();
            }
        }
    }

    public synchronized void resetTimeout() {
        timeoutCount = 0;
    }
}
