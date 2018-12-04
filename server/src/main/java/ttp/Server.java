package ttp;

import java.io.UnsupportedEncodingException;
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
	
	public Server(ExecutorService es) throws SQLException {
		super();
		this.es = es;
		sendPackets = new LinkedList<>();
		connections = new HashMap<>();
	}
	
	public void setSocket(DataSocket serverSocket) {
		this.serverSocket = serverSocket;
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
		HashMap<String, String> values = packet.getData();
		if (values.containsKey("username")) {
			try {
				String id = super.handleLogin(values.get("username"), values.get("password"));
				if (!id.equals("")) {
					try {
						SocketConnection sc = new SocketConnection(Server.currentPort++, id);
						DataSocket ds = new DataSocket(sc);
						sc.setServerSocket(ds);
						sc.setConnection(this.serverSocket.getAddress(), this.serverSocket.getPort());
						connections.put(id, ds);
						es.execute(ds);
						this.serverSocket.addSend(new TTPPacket(id, true));
					} catch (SocketException | UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			String id = values.get("file");
			int port = connections.get(id).getReader().getPort();
			this.serverSocket.addSend(new TTPPacket(port));
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
