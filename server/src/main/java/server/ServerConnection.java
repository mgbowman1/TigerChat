package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;

public class ServerConnection extends Connection {
	
	// The current client port number
	private int portNum = 11001;
	
	// The thread container
	ExecutorService es;
	
	public ServerConnection(ExecutorService es) throws SocketException {
		super(11000);
		this.es = es;
	}

	@Override
	protected void messageHandler(UDPPacket udp) throws IOException {
		if (portNum == 12000) portNum = 11001;
		System.out.println("Creating new connection on port " + portNum);
		ClientConnection c = new ClientConnection(portNum);
		es.execute(c);
		UDPPacket p = new UDPPacket(Integer.toString(portNum++), 0);
		byte[] buffer = p.getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, udp.getSender(), udp.getSenderPort());
		socket.send(packet);
	}
	
	@Override
	protected void messageTimeout() {
		failedReads = 0;
	}

}
