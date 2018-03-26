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
            String response = "";
            try {
                response = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            log.info(response);
            String command = Message.getCommandFromJson(response);
            if (command.equals("ACTIVITY_BROADCAST")) {
                ClientSkeleton.getInstance().updateActivityPanel(response);
            }
        }
    }
}
