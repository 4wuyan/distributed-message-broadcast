package activitystreamer.client;

import messages.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;

public class Receiver extends Thread {
    private static final Logger log = LogManager.getLogger();
    private BufferedReader in;

    Receiver(Socket socket) {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        start();
    }

    @Override
    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        while (true) {
            String response, command;
            try {
                response = in.readLine();
            } catch (IOException e) {
                log.debug("socket closed");
                break;
            }
            try {
                command = Message.getCommandFromJson(response);
            } catch (NullPointerException e) {
                log.debug("the server disconnected");
                break;
            } catch (IllegalStateException e) {
                log.debug("failed to parse an incoming message in json");
                continue;
            }

            log.info(response);
            if (command.equals("ACTIVITY_BROADCAST")) {
                ClientSkeleton.getInstance().updateActivityPanel(response);
            }
        }
    }
}
