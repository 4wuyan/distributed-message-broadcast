package activitystreamer.server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.Settings;

import messages.*;

public class Connection extends Thread {
	private static final Logger log = LogManager.getLogger();
	private BufferedReader in;
	private PrintWriter out;
	private boolean open;
	private Socket socket;
	private boolean term=false;
	private String heartbeatMessage = "h";
	private Heartbeat keepAlive;

	Connection(Socket socket) throws IOException{
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
		this.socket = socket;
		open = true;
		keepAlive = new Heartbeat(this);
	}

	public void sendMessage(Message message) {
		sendMessage(message.toString());
	}

	private synchronized void sendMessage(String string) {
		if(open){
			out.println(string);
		}
	}

	public void sendHeartbeat() {
		sendMessage(heartbeatMessage);
	}

	public void closeCon(){
		if(open){
			log.info("closing connection "+Settings.socketAddress(socket));
			try {
				term=true;
				socket.close();
			} catch (IOException e) {
				// already closed?
				log.error("received exception closing the connection "+Settings.socketAddress(socket)+": "+e);
			}
		}
		Control.getInstance().connectionClosed(this);
		open = false;
	}

	public void run(){
		try {
			String data;
			while(!term && (data = in.readLine())!=null){
				if (data.equals(heartbeatMessage)){
					keepAlive.resetTimeout();
				} else {
					log.info(data);
					term = Control.getInstance().process(this, data);
				}
			}
			log.debug("connection closed to "+Settings.socketAddress(socket));
		} catch (IOException e) {
			log.error("connection "+Settings.socketAddress(socket)+" closed with exception: "+e);
		}
		closeCon();
	}

	public boolean isOpen() {
		return open;
	}

	public void startHeartbeat() {
		keepAlive.start();
	}
}
