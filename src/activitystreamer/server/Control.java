package activitystreamer.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.Settings;

import messages.*;
import org.json.simple.JSONObject;

public class Control {
	private static final Logger log = LogManager.getLogger();

	// for servers
	private HashSet<Connection> serverConnections;
	private HashMap<Connection, ServerAnnounceMessage> neighbourInfo;
	private LimitedLinkedList<ActivityBroadcastMessage> activityHistory;

	// for clients
	private HashSet<Connection> clientConnections;
	private OnlineUserManager onlineUserManager;

	private HashMap<String, String> registeredUsers;

	// for partition recovery
	private Connection parent = null;
	private String lastId = "";

	private static Control control = null;

	public static Control getInstance() {
		if(control==null){
			control=new Control();
		}
		return control;
	}

	private Control() {
		// for servers
		serverConnections = new HashSet<>();
		neighbourInfo = new HashMap<>();
		activityHistory = new LimitedLinkedList<>(Settings.getMaxHistory());

		// for clients
		clientConnections = new HashSet<>();
		onlineUserManager = new OnlineUserManager();

		registeredUsers = new HashMap<>();

		// start a listener
		try {
			new Listener().start();
		} catch (IOException e1) {
			log.fatal("failed to startup a listening thread: "+e1);
			System.exit(-1);
		}
		log.info("using given secret: " + Settings.getSecret());
	}

	public synchronized void initiateConnection(){
		// make a connection to another server if remote hostname is supplied
		if(Settings.getRemoteHostname()==null) return;

		try {
			Connection c = outgoingConnection(
				new Socket(Settings.getRemoteHostname(),Settings.getRemotePort())
			);
			AuthenticateMessage message = new AuthenticateMessage(Settings.getSecret());
			c.sendMessage(message);
			c.sendMessage(getAnnouncement());
		} catch (IOException e) {
			log.error("failed to make connection to "+Settings.getRemoteHostname()+":"+Settings.getRemotePort()+" :"+e);
			System.exit(-1);
		}

	}

	/*
	 * Processing incoming messages from the connection.
	 * Return true if the connection should close.
	 */
	public synchronized boolean process(Connection con, String msg){
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
				case "LOGOUT":
					shouldClose = true; break;
				case "SERVER_ANNOUNCE":
					shouldClose = processServerAnnounce(con, msg); break;
				case "SYNC_USER":
					shouldClose = processSyncUser(con, msg); break;
				case "NEW_USER":
					shouldClose = processNewUser(con, msg); break;
				case "USER_CONFLICT":
					shouldClose = processUserConflict(con, msg); break;
				case "ACTIVITY_RETRIEVE":
					shouldClose = processActivityRetrieve(con, msg); break;
				case "BUNDLE":
					shouldClose = processBundleMessage(con, msg); break;
				default:
					// other commands.
					shouldClose = true; break;
			}
		} catch (NullPointerException|IllegalStateException|JsonSyntaxException e) {
		 	/* Exception examples:
		 	{} -> NullPinterException
		 	xx -> IllegalStateException
		 	{x -> JsonSyntaxException
		 	 */
			log.debug("failed to parse an incoming message in json");
			InvalidMessageMessage reply;
			reply = new InvalidMessageMessage("your message is invalid");
			con.sendMessage(reply);
			shouldClose = true;
		}
		return shouldClose;
	}

	private boolean processActivityRetrieve(Connection connection, String string) {
		if (!serverConnections.contains(connection)) {
			connection.sendMessage(new InvalidMessageMessage("you are not authenticated"));
			return true;
		}

		ActivityRetrieveMessage message = new Gson().fromJson(string, ActivityRetrieveMessage.class);
		String lastMessageId = message.getAfter();

		LinkedList<ActivityBroadcastMessage> activities = getActivityBroadcastAfter(lastMessageId);
		BundleMessage response = new BundleMessage(activities);

		connection.sendMessage(response);
		return false;
	}

	private boolean processBundleMessage(Connection connection, String string) {
		boolean shouldClose= false;

		BundleMessage message = new Gson().fromJson(string, BundleMessage.class);
		LinkedList<String> messageStrings = message.getMessages();
		for (String messageString : messageStrings) {
			shouldClose = process(connection, messageString);
			if (shouldClose) {
				break;
			}
		}

		return shouldClose;
	}

	private boolean processUserConflict(Connection connection, String string) {
		if (!serverConnections.contains(connection)) {
			connection.sendMessage(new InvalidMessageMessage("you are not authenticated"));
			return true;
		}

		UserConflictMessage message = new Gson().fromJson(string, UserConflictMessage.class);
		String username = message.getUsername();
		if (registeredUsers.containsKey(username)) {
			registeredUsers.remove(username);
			onlineUserManager.logout(username);
		}
		forwardMessage(message, connection, serverConnections);

		return false;
	}

	private boolean processNewUser(Connection connection, String string) {
		if (!serverConnections.contains(connection)) {
			connection.sendMessage(new InvalidMessageMessage("you are not authenticated"));
			return true;
		}

		NewUserMessage message = new Gson().fromJson(string, NewUserMessage.class);
		String username = message.getUsername();
		String secret = message.getSecret();

		if (registeredUsers.containsKey(username)) {
			if (! registeredUsers.get(username).equals(secret)) {
				registeredUsers.remove(username);
				onlineUserManager.logout(username);
				forwardMessage(new UserConflictMessage(username), null, serverConnections);
			} else {
				forwardMessage(message, connection, serverConnections);
			}
		} else {
			registeredUsers.put(username, secret);
			forwardMessage(message, connection, serverConnections);
		}

		return false;
	}

	private boolean processSyncUser(Connection connection, String string) {
		if (!serverConnections.contains(connection)) {
			connection.sendMessage(new InvalidMessageMessage("you are not authenticated"));
			return true;
		}

		SyncUserMessage message = new Gson().fromJson(string, SyncUserMessage.class);
		HashMap<String, String> users = message.getUsers();

		for (Map.Entry<String, String> e : users.entrySet()) {
			String username = e.getKey();
			String secret = e.getValue();
			if (registeredUsers.containsKey(username)) {
				if (!registeredUsers.get(username).equals(secret)) {
					registeredUsers.remove(username);
					onlineUserManager.logout(username);
				}
			}
			else {
				registeredUsers.put(username, secret);
			}
		}

		forwardMessage(message, connection, serverConnections);
		return false;
	}

	private boolean processServerAnnounce(Connection connection, String string) {
		if (!serverConnections.contains(connection)) {
			connection.sendMessage(new InvalidMessageMessage("you are not authenticated"));
			return true;
		}

		ServerAnnounceMessage message = new Gson().fromJson(string, ServerAnnounceMessage.class);
		neighbourInfo.put(connection, message);
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

		boolean shouldClose;
		if(registeredUsers.containsKey(username)) {
			String info = username+" is already registered with the system";
			RegisterFailedMessage fail = new RegisterFailedMessage(info);
			connection.sendMessage(fail);
			shouldClose = true;
		} else {
			registeredUsers.put(username, secret);
			String info = "register success for "+username;
			RegisterSuccessMessage success = new RegisterSuccessMessage(info);
			connection.sendMessage(success);
			shouldClose = false;

			forwardMessage(new NewUserMessage(username, secret), null, serverConnections);
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
			if (! registeredUsers.isEmpty()) {
				HashMap<String,String> copy = new HashMap<>(registeredUsers);
				connection.sendMessage(new SyncUserMessage(copy));
			}
			connection.sendMessage(getAnnouncement());
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
		if(isValid) {
			JSONObject activity = message.getActivity();
			activity.put("authenticated_user", username);
			String id = Settings.nextSecret();
			ActivityBroadcastMessage broadcast = new ActivityBroadcastMessage(activity, id);
			activityHistory.add(broadcast);
			forwardMessage(broadcast, null, clientConnections);
			forwardMessage(broadcast, null, serverConnections);
		} else {
			String info = "username and/or secret is incorrect";
			connection.sendMessage(new AuthenticationFailMessage(info));
		}

		return !isValid;
	}

	private boolean processActivityBroadcast(Connection connection, String string) {
		if (!serverConnections.contains(connection)) {
			connection.sendMessage(new InvalidMessageMessage("you are not authenticated"));
			return true;
		}
		ActivityBroadcastMessage message =
				new Gson().fromJson(string, ActivityBroadcastMessage.class);
		activityHistory.add(message);
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

			// check redirect
			RedirectMessage redirect = getRedirect();
			if (redirect != null) {
				connection.sendMessage(redirect);
				shouldClose = true;
			} else {
				clientConnections.add(connection);
				onlineUserManager.login(username, connection);
				forwardMessage(getAnnouncement(), null, serverConnections);
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

	private RedirectMessage getRedirect() {
	 	for (ServerAnnounceMessage announcement: neighbourInfo.values()) {
	 		int load = announcement.getLoad();
	 		if (clientConnections.size() > load) {
	 			String hostname = announcement.getHostname();
	 			int port = announcement.getPort();
	 			return new RedirectMessage(hostname, port);
			}
		}
		return null;
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

	public synchronized void connectionClosed(Connection con){
		// For clients
		if(clientConnections.contains(con)) {
			clientConnections.remove(con);
			forwardMessage(getAnnouncement(), null, serverConnections);
		}
		onlineUserManager.remove(con);

		// For servers
		serverConnections.remove(con);
		neighbourInfo.remove(con);

		// For partition recovery
		if (con == parent) {
			parent = null;
			ActivityBroadcastMessage last = activityHistory.peekLast();
			if (last != null) {
			    lastId = last.getId();
			}
		    new Reconnect().start();
		}
	}

	public synchronized void incomingConnection(Socket s) throws IOException{
		log.debug("incoming connection: "+Settings.socketAddress(s));
		new Connection(s).start();
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
		c.start();
		parent = c;
		serverConnections.add(c);
		return c;
	}

	private ServerAnnounceMessage getAnnouncement(){
		int load = clientConnections.size();
		String hostname = Settings.getLocalHostname();
		int port = Settings.getLocalPort();
		return new ServerAnnounceMessage(load, hostname, port);
	}

	private LinkedList<ActivityBroadcastMessage> getActivityBroadcastAfter(String id) {
	 	LinkedList<ActivityBroadcastMessage> answer = new LinkedList<>();
		Iterator<ActivityBroadcastMessage> iterator = activityHistory.descendingIterator();
		while (iterator.hasNext()) {
			ActivityBroadcastMessage message = iterator.next();
			if (message.getId().equals(id)) {
				break;
			} else {
				answer.addFirst(message);
			}
		}
		return answer;
 	}

 	public synchronized void connectionRecover(Socket socket) {
		Connection connection;
		try {
			connection = outgoingConnection(socket);
		} catch (IOException e) {
			log.debug("can't make recovery connection");
			new Reconnect().start();
			return;
		}

		LinkedList<Message> messages = new LinkedList<>();
		messages.add(new AuthenticateMessage(Settings.getSecret()));
		messages.add(new ActivityRetrieveMessage(lastId));
		messages.add(new SyncUserMessage(new HashMap<>(registeredUsers)));
		messages.addAll(getActivityBroadcastAfter(lastId));

		BundleMessage bundleMessage = new BundleMessage(messages);
		connection.sendMessage(bundleMessage);
	}
}
