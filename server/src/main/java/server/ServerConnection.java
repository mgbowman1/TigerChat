package server;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

public class ServerConnection extends UDPConnection {
	
	// The current client port number
	private int portNum = 11001;
	
	// The list of client sockets and usernames
	HashMap<String, ClientConnection> connections = new HashMap<>();
	
	// The thread container
	ExecutorService es;
	
	// The login function call
	CallableStatement login;
	
	public ServerConnection(ExecutorService es) throws SocketException, SQLException, UnknownHostException {
		super(11000);
		this.es = es;
		login = conn.prepareCall("{? = call login(?, ?)}");
		login.registerOutParameter(1, java.sql.Types.INTEGER);
	}

	@Override
	protected void messageHandler(UDPPacket udp) throws SQLException, UnknownHostException  {
		
		// Ensuring they login before connecting
		// Make sure they sent the right format
		String[] creds = udp.getMessage().split(",");
		if (creds.length != 2) {
			badLogin(udp);
			return;
		}
		
		// Make sure it is a valid account
		String username = creds[0];
		String password = creds[1];
		login.setString(2, username);
		login.setString(3, password);
		login.execute();
		if (login.getInt(1) == 0) {
			badLogin(udp);
			return;
		}
		
		// Check if they have a socket and it has enough time left
		if (connections.containsKey(username) && connections.get(username).hasTime()) {
			UDPPacket p = new UDPPacket(Integer.toString(connections.get(username).port), packetNumber);
			send(p, udp);
		} else {
			
			// Setup a new socket for them
			if (portNum == 21001) portNum = 11001;
			ClientConnection c;
			boolean passed = false;
			while (true) {
				try {
					c = new ClientConnection(portNum, username);
					break;
				} catch (SocketException e) {
					portNum++;
					if (portNum == 21001) {
						portNum = 11001;
						if (passed) {
							UDPPacket p = new UDPPacket(PacketError.SOCKETERROR, packetNumber);
							send(p, udp);
							return;
						}
						passed = true;
					}
				}
			}
			es.execute(c);
			connections.put(username, c);
			UDPPacket p = new UDPPacket(Integer.toString(portNum++), packetNumber);
			send(p, udp);
		}
	}
	
	private void badLogin(UDPPacket udp) {
		UDPPacket p = new UDPPacket(PacketError.BADLOGIN, packetNumber);
		send(p, udp);
	}
	
	@Override
	protected void messageTimeout() {
		failedReads = 0;
	}

}
