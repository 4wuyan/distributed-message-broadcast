package activitystreamer.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.util.Settings;

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
		sendRawStringToServer(activityObj.toString());

		// Temporary test only! Probably need to be changed later.
		String response = readRawStringFromServer();
		JSONParser parser = new JSONParser();
		JSONObject json = null;
		try {
			json = (JSONObject) parser.parse(response);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		textFrame.setOutputText(json);
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

	public void sendRawStringToServer(String string) {
		out.println(string);
	}

	public String readRawStringFromServer() {
		try {
			return in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
}
