package activitystreamer.client;

import activitystreamer.Client;
import com.google.gson.JsonSyntaxException;
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
            } catch (SocketException e) {
                // Happens when Client disconnects first
                log.info("socket closed");
                break;
            } catch (IOException e) {
                log.error("error in reading from the socket");
                break;
            }
            try {
                command = Message.getCommandFromJson(response);
                log.info(response);
                if (command.equals("ACTIVITY_BROADCAST")) {
                    ClientSkeleton.getInstance().updateActivityPanel(response);
                }
            } catch (NullPointerException e) {
                // Happens when Server disconnects first
                log.debug("the server disconnected");
                break;
            } catch (IllegalStateException|JsonSyntaxException e) {
                log.debug("failed to parse an incoming message in json");
                break;
            }
        }
        // Make sure we close the socket
        ClientSkeleton.getInstance().closeSocket();
    }
}
