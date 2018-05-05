package activitystreamer.server;

import activitystreamer.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

class Reconnect extends Thread {
    private static final Logger log = LogManager.getLogger();
    private String hostname;
    private int port;
    private int interval; // millisecond
    private int attempts;

    Reconnect() {
        hostname = Settings.getRemoteHostname();
        port = Settings.getRemotePort();
        interval = Settings.getActivityInterval();
        attempts = Settings.getMaxReconnectAttempts();
    }

    @Override
    public void run(){
        Socket socket;
        for (int i = 0; i < attempts; i++) {
            try {
                socket = new Socket(hostname, port);
                log.info("connection to "+hostname+":"+port+" fixed");
                Control.getInstance().connectionRecover(socket);
                break;
            } catch (IOException e) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e2) {
                    log.info("reconnection thread interrupted");
                    break;
                }
            }
        }
    }
}
