package activitystreamer.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import messages.BadMessageException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import activitystreamer.util.Settings;

import messages.*;

public class ClientSkeleton extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static ClientSkeleton clientSolution;
	private TextFrame textFrame;
	private BufferedReader in;
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
	
	
	
	
	
	
	@SuppressWarnings("unchecked")
	public void sendActivityObject(JSONObject activityObj){
		String username = Settings.getUsername();
		String secret = Settings.getSecret();
		ActivityMessage message = new ActivityMessage(username, secret, activityObj);
		sendRawStringToServer(message.toString());

		// Temporary test only! Probably need to be changed later.
		String response = readRawStringFromServer();
		System.out.println(response);
		String command = Message.getCommandFromJson(response);
		if (command.equals("ACTIVITY_BROADCAST")) {
			ActivityBroadcastMessage reply =
					new Gson().fromJson(response, ActivityBroadcastMessage.class);
			JSONObject activity = reply.getActivity();
			textFrame.setOutputText(activity);
		}
	}
	
	
	public void disconnect(){
		
	}
	
	
	public void run(){
		int remotePort = Settings.getRemotePort();
		String remoteHostname = Settings.getRemoteHostname();
		try {
			Socket socket = new Socket(remoteHostname, remotePort);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
		}
		catch (IOException e) {
			System.out.println(e.getMessage());
		}


		String username = Settings.getUsername();
		String secret = Settings.getSecret();
		LoginMessage loginMessage = new LoginMessage(username, secret);
		sendMessageToServer(loginMessage);

	}

	private void sendRawStringToServer(String string) {
		out.println(string);
	}

	private String readRawStringFromServer() {
		try {
			return in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	public void sendMessageToServer(Message message) {
		sendRawStringToServer(message.toString());
	}
}
