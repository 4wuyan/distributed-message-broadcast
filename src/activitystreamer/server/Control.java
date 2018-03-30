package activitystreamer.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.Settings;

import messages.*;
import org.json.simple.JSONObject;

public class Control extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static ArrayList<Connection> connections;
	private static ArrayList<Connection> serverConnections;
	private static HashMap<String, String> registeredUsers;
	private static HashMap<String, LockManager> lockManagers;
	private static boolean term=false;
	private static Listener listener;
	
	protected static Control control = null;
	
	public static Control getInstance() {
		if(control==null){
			control=new Control();
		} 
		return control;
	}
	
	public Control() {
		// initialize the connections array
		connections = new ArrayList<>();
		serverConnections = new ArrayList<>();

		registeredUsers = new HashMap<>();
		lockManagers = new HashMap<>();
		// start a listener
		try {
			listener = new Listener();
		} catch (IOException e1) {
			log.fatal("failed to startup a listening thread: "+e1);
			System.exit(-1);
		}
		log.info("using given secret: " + Settings.getSecret());
	}
	
	public void initiateConnection(){
		// make a connection to another server if remote hostname is supplied
		if(Settings.getRemoteHostname()!=null){
			try {
				outgoingConnection(new Socket(Settings.getRemoteHostname(),Settings.getRemotePort()));
			} catch (IOException e) {
				log.error("failed to make connection to "+Settings.getRemoteHostname()+":"+Settings.getRemotePort()+" :"+e);
				System.exit(-1);
			}
		}
	}
	
	/*
	 * Processing incoming messages from the connection.
	 * Return true if the connection should close.
	 */
	public synchronized boolean process(Connection con,String msg){
		String command;
		boolean shouldClose = false;
		try {
			command = Message.getCommandFromJson(msg);
			switch (command) {
				case "LOGIN":
					shouldClose = processLogin(con, msg); break;
				case "ACTIVITY_MESSAGE":
					shouldClose = processActivityMessage(con, msg); break;
				case "ACTIVITY_BROADCAST":
					shouldClose = processActivityBroadcast(con, msg); break;
				case "REGISTER":
					shouldClose = processRegister(con, msg);
					break;
				case "AUTHENTICATE":
					//authenticate(con, msg);
					break;
				case "LOCK_REQUEST":
					shouldClose = processLockRequest(con, msg); break;
				case "LOCK_ALLOWED": break;
				case "LOCK_DENIED": break;
				case "LOGOUT": break;
				case "SERVER_ANNOUNCE": break;
				default:
					// other commands.
					shouldClose = true; break;
			}
		} catch (IllegalStateException|JsonSyntaxException e) {
			log.debug("failed to parse an incoming message in json");
			InvalidMessageMessage reply;
			reply = new InvalidMessageMessage("your message is invalid");
			con.sendMessage(reply);
			shouldClose = true;
		}
		return shouldClose;
	}

	private boolean processLockRequest(Connection connection, String string) {
		if (!connection.isAuthenticated()) {
			connection.sendMessage(new InvalidMessageMessage("you are not authenticated"));
			return true;
		}
		LockRequestMessage message = new Gson().fromJson(string, LockRequestMessage.class);
		String username = message.getUsername();
		String secret = message.getSecret();

		///
		///
		///

		return false;
	}

	private boolean processRegister(Connection connection, String string) {
		RegisterMessage message = new Gson().fromJson(string, RegisterMessage.class);
		String username = message.getUsername();
		String secret = message.getSecret();
		if (connection.isAuthenticated()) {
			String info = "received REGISTER from a client that has already logged in as"+username;
			connection.sendMessage(new InvalidMessageMessage(info));
			return true;
		}
		boolean shouldClose = false;

		String successInfo = "register success for "+username;
		String failedInfo = username+" is already registered with the system";
		RegisterSuccessMessage successMessage = new RegisterSuccessMessage(successInfo);
		RegisterFailedMessage failedMessage = new RegisterFailedMessage(failedInfo);
		if(registeredUsers.containsKey(username)) {
		    connection.sendMessage(failedMessage);
		    shouldClose = true;
		} else {
			LockManager lockManager = new LockManager
				(serverConnections, connection, successMessage, failedMessage);
			lockManagers.put(username, lockManager);

			LockRequestMessage request = new LockRequestMessage(username, secret);
			forwardMessage(request, connection, serverConnections);
		}
		return shouldClose;
	}

	/*
	private void authenticate(Connection connection, String string) {
		AuthenticateMessage message = new Gson().fromJson(string, AuthenticateMessage.class);
		String secret = message.getSecret();
		Message reply;
		if(secret.equals(Settings.getSecret())) {
			reply = new AuthenticateMessage()
		}
	}
	*/

	private boolean processActivityMessage(Connection connection, String string) {
		if (!connection.isAuthenticated()) {
			connection.sendMessage(new InvalidMessageMessage("must send a LOGIN message first"));
			return true;
		}

		ActivityMessageMessage message =
			new Gson().fromJson(string, ActivityMessageMessage.class);

		String username = message.getUsername();
		String secret = message.getSecret();
		boolean isValid = checkUsernameSecret(username, secret);
		Message reply;
		if(isValid) {
			JSONObject activity = message.getActivity();
			activity.put("authenticated_user", username);
			reply = new ActivityBroadcastMessage(activity);
			forwardMessage(reply, connection, connections);
		} else {
			String info = "username and/or secret is incorrect";
			reply = new AuthenticationFailMessage(info);
		}
		connection.sendMessage(reply);

		return !isValid;
	}

	private boolean processActivityBroadcast(Connection connection, String string) {
		if (!connection.isAuthenticated()) {
			connection.sendMessage(new InvalidMessageMessage("you are not authenticated"));
			return true;
		}
        ActivityBroadcastMessage message =
                new Gson().fromJson(string, ActivityBroadcastMessage.class);
        forwardMessage(message, connection, connections);
		return false;
	}

	private boolean processLogin(Connection connection, String string) {
		LoginMessage message = new Gson().fromJson(string, LoginMessage.class);
		String username, secret;
		username = message.getUsername();
		secret = message.getSecret();

		boolean isSuccessful = checkUsernameSecret(username, secret);
		Message reply;
		String replyInfo;
		if(isSuccessful) {
			replyInfo = "logged in as user " + username;
			reply = new LoginSuccessMessage(replyInfo);
			connection.setAuthenticated(true);
			/*

			CHECK REDIRECT!!!!

			 */
		} else {
			if (registeredUsers.containsKey(username))
			    replyInfo = "wrong secret for user " + username;
			else
				replyInfo = "user "+username+" is not registered";
			reply = new LoginFailedMessage(replyInfo);
		}
		connection.sendMessage(reply);
		return !isSuccessful;
	}

	private boolean checkUsernameSecret(String username, String secret) {
		if(username.equals("anonymous")) return true;
		if(! registeredUsers.containsKey(username)) return false;

        String storedSecret = registeredUsers.get(username);
		return storedSecret.equals(secret);
	}

	private void forwardMessage(Message message, Connection from, ArrayList<Connection> group) {
		for(Connection connection: group) {
			if (connection != from) connection.sendMessage(message);
		}
	}
	/*
	 * The connection has been closed by the other party.
	 */
	public synchronized void connectionClosed(Connection con){
		if(!term) connections.remove(con);
	}
	
	/*
	 * A new incoming connection has been established, and a reference is returned to it
	 */
	public synchronized Connection incomingConnection(Socket s) throws IOException{
		log.debug("incoming connection: "+Settings.socketAddress(s));
		Connection c = new Connection(s);
		connections.add(c);
		return c;
	}
	
	/*
	 * A new outgoing connection has been established, and a reference is returned to it
	 */
	public synchronized Connection outgoingConnection(Socket s) throws IOException{
	    if(s.getInetAddress().equals(InetAddress.getByName("localhost"))) {
	        if(s.getPort() == listener.getPortnum()) {
                log.fatal("Must not connect to yourself!");
	            throw new IOException();
			}
		}
		log.debug("outgoing connection: "+Settings.socketAddress(s));
		Connection c = new Connection(s);
		String secret = Settings.getSecret();
		AuthenticateMessage message = new AuthenticateMessage(secret);
		c.sendMessage(message);
		connections.add(c);
		serverConnections.add(c);
		return c;
	}
	
	@Override
	public void run(){
		log.info("using activity interval of "+Settings.getActivityInterval()+" milliseconds");
		while(!term){
			// do something with 5 second intervals in between
			try {
				Thread.sleep(Settings.getActivityInterval());
			} catch (InterruptedException e) {
				log.info("received an interrupt, system is shutting down");
				break;
			}
			if(!term){
				log.debug("doing activity");
				term=doActivity();
			}
			
		}
		log.info("closing "+connections.size()+" connections");
		// clean up
		for(Connection connection : connections){
			connection.closeCon();
		}
		listener.setTerm(true);
	}
	
	public boolean doActivity(){
		return false;
	}
	
	public final void setTerm(boolean t){
		term=t;
	}
	
	public final ArrayList<Connection> getConnections() {
		return connections;
	}
}
