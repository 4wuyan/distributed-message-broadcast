package activitystreamer.client;

import java.io.*;
import java.net.Socket;

import com.google.gson.Gson;
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

		textFrame = new TextFrame();
		start();
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
		// To be improved later
		// Still need login logic
		String username = Settings.getUsername();
		String secret = Settings.getSecret();
		LoginMessage loginMessage = new LoginMessage(username, secret);
		sendMessageToServer(loginMessage);
	}

	public void sendMessageToServer(Message message) {
		out.println(message.toString());
	}

	public void updateActivityPanel(String string) {
		ActivityBroadcastMessage reply =
				new Gson().fromJson(string, ActivityBroadcastMessage.class);
		JSONObject activity = reply.getActivity();
		textFrame.setOutputText(activity);
	}
}
