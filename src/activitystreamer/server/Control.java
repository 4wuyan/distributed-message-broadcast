package activitystreamer.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.Settings;

import messages.*;
import org.json.simple.JSONObject;

public class Control extends Thread {
	private static final Logger log = LogManager.getLogger();
	private HashSet<Connection> connections;
	private HashSet<Connection> serverConnections;
	private HashSet<Connection> clientConnections;
	private HashMap<String, String> registeredUsers;
	private HashMap<String, PendingRegistration> registrations;
	private boolean term=false;
	private Listener listener;
	private String serverId;
	private HashMap<String, RedirectMessage> redirects;
	private HashSet<String> knownServerIDs;

	private static Control control = null;

	public static Control getInstance() {
		if(control==null){
			control=new Control();
		}
		return control;
	}

	private Control() {
		// initialize the connections array
		connections = new HashSet<>();
		serverConnections = new HashSet<>();
		clientConnections = new HashSet<>();
		serverId = Settings.nextSecret();
		redirects = new HashMap<>();
		knownServerIDs = new HashSet<>();

		registeredUsers = new HashMap<>();
		registrations = new HashMap<>();
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
		if(Settings.getRemoteHostname()==null) return;

		try {
			Connection c = outgoingConnection(
				new Socket(Settings.getRemoteHostname(),Settings.getRemotePort())
			);
			AuthenticateMessage message = new AuthenticateMessage(Settings.getSecret());
			c.sendMessage(message);
		} catch (IOException e) {
			log.error("failed to make connection to "+Settings.getRemoteHostname()+":"+Settings.getRemotePort()+" :"+e);
			System.exit(-1);
		}

	}

	/*
	 * Processing incoming messages from the connection.
	 * Return true if the connection should close.
	 */
	public synchronized boolean process(Connection con,String msg){
		String command;
		boolean shouldClose;
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
					shouldClose = processRegister(con, msg); break;
				case "AUTHENTICATE":
					shouldClose = processAuthenticate(con, msg); break;
				case "LOCK_REQUEST":
					shouldClose = processLockRequest(con, msg); break;
				case "LOCK_ALLOWED":
					shouldClose = processLockAllowed(con, msg); break;
				case "LOCK_DENIED":
					shouldClose = processLockDenied(con, msg); break;
				case "LOGOUT":
					shouldClose = true; break;
				case "SERVER_ANNOUNCE":
					shouldClose = processServerAnnounce(con, msg); break;
				default:
					// other commands.
					shouldClose = true; break;
			}
		} catch (NullPointerException|IllegalStateException|JsonSyntaxException e) {
			log.debug("failed to parse an incoming message in json");
			InvalidMessageMessage reply;
			reply = new InvalidMessageMessage("your message is invalid");
			con.sendMessage(reply);
			shouldClose = true;
		}
		return shouldClose;
	}

	private boolean processServerAnnounce(Connection connection, String string) {
		if (!serverConnections.contains(connection)) {
			connection.sendMessage(new InvalidMessageMessage("you are not authenticated"));
			return true;
		}

		ServerAnnounceMessage message = new Gson().fromJson(string, ServerAnnounceMessage.class);
		int load = message.getLoad();
		String id = message.getId();

		knownServerIDs.add(id);

		if (clientConnections.size() > load) {
			// When a new client connects later, it will be at least 2 clients less.
			String hostname = message.getHostname();
			int port = message.getPort();
			RedirectMessage redirectMessage = new RedirectMessage(hostname, port);
			redirects.put(id, redirectMessage);
		} else {
			redirects.remove(id);
		}

		forwardMessage(message, connection, serverConnections);
		return false;
	}

	private boolean processLockDenied(Connection connection, String string) {
		if (!serverConnections.contains(connection)) {
			connection.sendMessage(new InvalidMessageMessage("you are not authenticated"));
			return true;
		}

		LockDeniedMessage message = new Gson().fromJson(string, LockDeniedMessage.class);
		String username= message.getUsername();
		String secret = message.getSecret();

		if(registeredUsers.containsKey(username)){
			if(registeredUsers.get(username).equals(secret)) {
				registeredUsers.remove(username);
			}
		}
		if(registrations.containsKey(username)) {
			registrations.get(username).sendFailMessage();
			registrations.remove(username);
		}

		forwardMessage(message, connection, serverConnections);
		return false;
	}

	private boolean processLockAllowed(Connection connection, String string) {
		if (!serverConnections.contains(connection)) {
			connection.sendMessage(new InvalidMessageMessage("you are not authenticated"));
			return true;
		}

		LockAllowedMessage message = new Gson().fromJson(string, LockAllowedMessage.class);
		String username = message.getUsername();
		if (registrations.containsKey(username)) {
			PendingRegistration registration = registrations.get(username);
			registration.acceptApproval(message);
			if (registration.allApproved()) {
				registration.sendSuccessMessage();
				registrations.remove(username);
			}
		}
		forwardMessage(message, connection, serverConnections);
		return false;
	}

	private boolean processLockRequest(Connection connection, String string) {
		if (!serverConnections.contains(connection)) {
			connection.sendMessage(new InvalidMessageMessage("you are not authenticated"));
			return true;
		}
		LockRequestMessage message = new Gson().fromJson(string, LockRequestMessage.class);
		String username = message.getUsername();
		String secret = message.getSecret();

		if (registeredUsers.containsKey(username)) {
			connection.sendMessage(new LockDeniedMessage(username, secret));
		} else {
			registeredUsers.put(username, secret);
			connection.sendMessage(new LockAllowedMessage(username, secret));
			forwardMessage(message, connection, serverConnections);
		}
		return false;
	}

	private boolean processRegister(Connection connection, String string) {
		RegisterMessage message = new Gson().fromJson(string, RegisterMessage.class);
		String username = message.getUsername();
		String secret = message.getSecret();
		if (clientConnections.contains(connection)) {
			String info = "received REGISTER from a client that has already logged in as "+username;
			connection.sendMessage(new InvalidMessageMessage(info));
			return true;
		}
		if (secret == null) {
			String info = "the message must contain non-null key secret";
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
			registeredUsers.put(username, secret);
			if (serverConnections.isEmpty()) {
				// happens when there's only one server
				connection.sendMessage(successMessage);
			} else {
				PendingRegistration pendingRegistration = new PendingRegistration(
						secret, connection, knownServerIDs.size(), successMessage, failedMessage);
				registrations.put(username, pendingRegistration);

				LockRequestMessage request = new LockRequestMessage(username, secret);
				forwardMessage(request, null, serverConnections);
			}
		}
		return shouldClose;
	}

	private boolean processAuthenticate(Connection connection, String string) {
		if (serverConnections.contains(connection)) {
			connection.sendMessage(new InvalidMessageMessage("already authenticated"));
			return true;
		}

		AuthenticateMessage message = new Gson().fromJson(string, AuthenticateMessage.class);
		String secret = message.getSecret();
		boolean shouldClose = false;
		if(secret.equals(Settings.getSecret())) {
			serverConnections.add(connection);
		}
		else {
			connection.sendMessage(new AuthenticationFailMessage("secret incorrect"));
			shouldClose = true;
		}
		return shouldClose;
	}

	@SuppressWarnings("unchecked")
	private boolean processActivityMessage(Connection connection, String string) {
		if (!clientConnections.contains(connection)) {
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
			forwardMessage(reply, connection, clientConnections);
			forwardMessage(reply, null, serverConnections);
		} else {
			String info = "username and/or secret is incorrect";
			reply = new AuthenticationFailMessage(info);
		}
		connection.sendMessage(reply);

		return !isValid;
	}

	private boolean processActivityBroadcast(Connection connection, String string) {
		if (!serverConnections.contains(connection)) {
			connection.sendMessage(new InvalidMessageMessage("you are not authenticated"));
			return true;
		}
		ActivityBroadcastMessage message =
				new Gson().fromJson(string, ActivityBroadcastMessage.class);
		forwardMessage(message, connection, serverConnections);
		forwardMessage(message, null, clientConnections);
		return false;
	}

	private boolean processLogin(Connection connection, String string) {
		LoginMessage message = new Gson().fromJson(string, LoginMessage.class);
		String username, secret;
		username = message.getUsername();
		secret = message.getSecret();

		boolean isSuccessful = checkUsernameSecret(username, secret);
		boolean shouldClose;
		String replyInfo;
		if(isSuccessful) {
			replyInfo = "logged in as user " + username;
			connection.sendMessage(new LoginSuccessMessage(replyInfo));
			clientConnections.add(connection);

			// check redirect
			if (!redirects.isEmpty()) {
				connection.sendMessage(redirects.values().iterator().next());
				shouldClose = true;
			} else {
				shouldClose = false;
			}
		} else {
			if (registeredUsers.containsKey(username))
				replyInfo = "wrong secret for user " + username;
			else
				replyInfo = "user "+username+" is not registered";
			connection.sendMessage(new LoginFailedMessage(replyInfo));
			shouldClose = true;
		}
		return shouldClose;
	}

	private boolean checkUsernameSecret(String username, String secret) {
		if(username.equals("anonymous")) return true;
		if(! registeredUsers.containsKey(username)) return false;

		String storedSecret = registeredUsers.get(username);
		return storedSecret.equals(secret);
	}

	private void forwardMessage (Message message, Connection from, HashSet<Connection> group) {
		for(Connection connection: group) {
			if (connection != from) connection.sendMessage(message);
		}
	}

	/*
	 * The connection has been closed by the other party.
	 */
	public synchronized void connectionClosed(Connection con){
		if(!term){
			connections.remove(con);
			serverConnections.remove(con);
			clientConnections.remove(con);
		}
	}

	public synchronized void incomingConnection(Socket s) throws IOException{
		log.debug("incoming connection: "+Settings.socketAddress(s));
		Connection c = new Connection(s);
		connections.add(c);
	}

	/*
	 * A new outgoing connection has been established, and a reference is returned to it
	 */
	private synchronized Connection outgoingConnection(Socket s) throws IOException{
		if(s.getInetAddress().equals(InetAddress.getByName("localhost"))) {
			if(s.getPort() == Settings.getLocalPort()) {
				log.fatal("Must not connect to yourself!");
				throw new IOException();
			}
		}
		log.debug("outgoing connection: "+Settings.socketAddress(s));
		Connection c = new Connection(s);
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

	private boolean doActivity(){
		ServerAnnounceMessage message;
		int load = clientConnections.size();
		String hostname = Settings.getLocalHostname();
		int port = Settings.getLocalPort();
		message = new ServerAnnounceMessage(serverId, load, hostname, port);

		forwardMessage(message, null, serverConnections);
		return false;
	}

	public final void setTerm(boolean t){
		term=t;
	}
}
