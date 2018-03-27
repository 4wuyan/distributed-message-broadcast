package activitystreamer.client;

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
            String response;
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
            ClientSkeleton.getInstance().process(response);
        }
        // Make sure we close the socket
        ClientSkeleton.getInstance().closeSocket();
    }

}
