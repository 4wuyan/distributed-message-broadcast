package activitystreamer.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import messages.BadMessageException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.util.Settings;

import messages.Message;

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
		// Temporary test only! Probably need to be changed later.
		sendRawStringToServer(activityObj.toString());
		String response = readRawStringFromServer();
		try {
			Message message = Message.getMessageFromJson(response);
			textFrame.setOutputText(message.toJSONObject());
		} catch (BadMessageException e) {
			e.getMessage();
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
}
