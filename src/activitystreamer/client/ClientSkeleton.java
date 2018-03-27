package activitystreamer.client;

import java.io.*;
import java.net.Socket;
import java.util.Set;

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
	private Receiver receiver;
	private TextFrame textFrame;
	private PrintWriter out;
	

	
	public static ClientSkeleton getInstance(){
		if(clientSolution==null){
			clientSolution = new ClientSkeleton();
		}
		return clientSolution;
	}
	
	public ClientSkeleton(){
		textFrame = new TextFrame();
		start();
	}

	public void connect() {
		int remotePort = Settings.getRemotePort();
		String remoteHostname = Settings.getRemoteHostname();
		try {
			socket = new Socket(remoteHostname, remotePort);
			out = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			log.fatal("failed to establish the socket connection: "+e);
			System.exit(-1);
		}
		receiver = new Receiver(socket);

		String username = Settings.getUsername();
		String secret = Settings.getSecret();
		Message message;
		if (username.equals("anonymous")) {
			message = new LoginMessage();
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
	

	@SuppressWarnings("unchecked")
	public void sendActivityObject(JSONObject activityObj){
		String username = Settings.getUsername();
		String secret = Settings.getSecret();
		ActivityMessage message = new ActivityMessage(username, secret, activityObj);
		sendMessageToServer(message);
	}
	
	
	public void disconnect(){
		sendMessageToServer(new LogoutMessage());
		closeSocket();
		System.exit(0);
	}

	public void closeSocket() {
	    if (!socket.isClosed()) {
			try {
				socket.close();
			} catch (IOException e) {
				log.error("error in closing the socket");
				e.printStackTrace();
			}
		}
	}
	
	
	public void run(){
	    connect();
	}

	public void sendMessageToServer(Message message) {
		out.println(message.toString());
	}

	private void updateActivityPanel(String string) {
		ActivityBroadcastMessage reply =
				new Gson().fromJson(string, ActivityBroadcastMessage.class);
		JSONObject activity = reply.getActivity();
		textFrame.setOutputText(activity);
	}

	public void process(String response) {
		String command;
		boolean shouldExit = false;
		try {
			command = Message.getCommandFromJson(response);
			log.info(response);
			if (command.equals("ACTIVITY_BROADCAST")) {
				updateActivityPanel(response);
			} else if (command.equals("REGISTER_SUCCESS")) {
				String username = Settings.getUsername();
				String secret = Settings.getSecret();
				sendMessageToServer(new LoginMessage(username, secret));
			} else if (command.equals("INVALID_MESSAGE")) {
				shouldExit = true;
			} else if (command.equals("LOGIN_FAIL")) {
				shouldExit = true;
			} else if (command.equals("REGISTER_FAILED")) {
				shouldExit = true;
			} else if (command.equals("AUTHENTICATION_FAIL")) {
				shouldExit = true;
			} else if (command.equals("REDIRECT")) {
				redirect(response);
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

	// TO BE TESTED!!!!!!!!
	public void redirect(String string) {
		RedirectMessage message = new Gson().fromJson(string, RedirectMessage.class);
		Settings.setRemoteHostname(message.getHostname());
		Settings.setRemotePort(message.getPort());
		closeSocket();
		connect();
	}
}
