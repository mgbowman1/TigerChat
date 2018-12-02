package ttp;

import java.net.SocketException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;

import rdp.DataSocket;

public class Server extends Processor {
	
	// List of packets for users
	protected static LinkedList<ServerSendPackets> sendPackets;
	
	// List of connections
	protected static HashMap<String, DataSocket> connections;
	
	// Current port
	protected static int currentPort = 11001;
	
	// The main thread holder
	ExecutorService es;
	
	// The datasocket for the server
	DataSocket serverSocket;
	
	public Server(ExecutorService es, DataSocket serverSocket) throws SQLException {
		super();
		this.es = es;
		this.serverSocket = serverSocket;
		sendPackets = new LinkedList<>();
	}
	
	public static void addPacket(String user, TTPPacket ttp) {
		sendPackets.add(new ServerSendPackets(user, ttp));
	}
	
	public static void sendAllPackets() {
		for (ServerSendPackets s : sendPackets) {
			if (connections.containsKey(s.user)) connections.get(s.user).addSend(s.packet);
		}
	}

	@Override
	public int reconnect() {
		return -1;
	}

	@Override
	public void receive(TTPPacket packet) {
		HashMap<String, Object> values = packet.getData();
		if (values.containsKey("username")) {
			try {
				String id = super.handleLogin((String) values.get("username"), (String) values.get("password"));
				if (!id.equals("")) {
					try {
						DataSocket ds = new DataSocket(new SocketConnection(Server.currentPort++));
						es.execute(ds);
						this.serverSocket.addSend(new TTPPacket(Server.currentPort - 1));
					} catch (SocketException e) {
						e.printStackTrace();
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public int getPort() {
		return 11000;
	}

	@Override
	public void close() throws SQLException {
		super.close();
	}

}
