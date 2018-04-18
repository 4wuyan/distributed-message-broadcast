package activitystreamer.client;

import java.io.*;
import java.net.*;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import activitystreamer.util.Settings;

import messages.*;

public class ClientSkeleton extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static ClientSkeleton clientSolution;
	private Socket socket;
	private TextFrame textFrame;
	private PrintWriter out;
	private BufferedReader in;



	public static ClientSkeleton getInstance(){
		if(clientSolution==null){
			clientSolution = new ClientSkeleton();
		}
		return clientSolution;
	}

	private ClientSkeleton(){
		textFrame = new TextFrame();
	}

	public void connect() {
		int remotePort = Settings.getRemotePort();
		String remoteHostname = Settings.getRemoteHostname();
		try {
			socket = new Socket(remoteHostname, remotePort);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			log.fatal("failed to establish the socket connection: "+e);
			System.exit(-1);
		}

		String username = Settings.getUsername();
		String secret = Settings.getSecret();
		Message message;
		if (username.equals("anonymous")) {
			message = new LoginMessage(username);
		} else {
			if (secret != null) {
				message = new LoginMessage(username, secret);
			} else {
				String newSecret = Settings.nextSecret();
				Settings.setSecret(newSecret);
				log.info("Registering "+username+" with secret "+newSecret);
				message = new RegisterMessage(username, newSecret);
			}
		}
		sendMessageToServer(message);
	}

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
			log.info(response);
			processReplyString(response);
		}
		// Make sure we close the socket
		closeSocket();
	}


	@SuppressWarnings("unchecked")
	public void sendActivityObject(JSONObject activityObj){
		String username = Settings.getUsername();
		String secret = Settings.getSecret();
		ActivityMessageMessage message = new ActivityMessageMessage(username, secret, activityObj);
		sendMessageToServer(message);
	}


	public void disconnect(){
		sendMessageToServer(new LogoutMessage());
		closeSocket();
		System.exit(0);
	}

	private void closeSocket() {
		if (!socket.isClosed()) {
			try {
				socket.close();
			} catch (IOException e) {
				log.error("error in closing the socket");
			}
		}
	}

	private void sendMessageToServer(Message message) {
		out.println(message.toString());
	}

	private void updateActivityPanel(String string) {
		ActivityBroadcastMessage reply =
				new Gson().fromJson(string, ActivityBroadcastMessage.class);
		JSONObject activity = reply.getActivity();
		textFrame.setOutputText(activity);
	}

	private void processReplyString(String response) {
		String command;
		boolean shouldExit = false;
		try {
			command = Message.getCommandFromJson(response);
			switch (command) {
				case "ACTIVITY_BROADCAST": updateActivityPanel(response); break;
				case "REGISTER_SUCCESS":
					String username = Settings.getUsername();
					String secret = Settings.getSecret();
					sendMessageToServer(new LoginMessage(username, secret));
					break;
				case "LOGIN_SUCCESS": break;
				case "REDIRECT": redirect(response); break;
				default:
					// For INVALID_MESSAGE, LOGIN_FAILED, REGISTER_FAILED
					// AUTHENTICATION_FAIL, REDIRECT
					// and other commands.
					shouldExit = true; break;
			}
		} catch (NullPointerException e) {
			// Happens when Server disconnects first
			log.debug("the server disconnected");
			shouldExit = true;
		} catch (IllegalStateException|JsonSyntaxException e) {
			log.debug("failed to parse an incoming message in json");
			shouldExit = true;
		}

		if (shouldExit) {
			closeSocket();
			System.exit(1);
		}
	}

	private void redirect(String string) {
		RedirectMessage message = new Gson().fromJson(string, RedirectMessage.class);
		Settings.setRemoteHostname(message.getHostname());
		Settings.setRemotePort(message.getPort());
		closeSocket();
		connect();
	}
}
