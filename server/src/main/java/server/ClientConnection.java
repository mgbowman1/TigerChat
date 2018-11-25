package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;

public class ClientConnection extends Connection {
	
	public ClientConnection(int port) throws SocketException {
		super(port);
	}

	@Override
	protected void messageHandler(UDPPacket udp) throws IOException {
		UDPPacket p = new UDPPacket(udp.getMessage() + " from the server.", udp.getError());
		byte[] buffer = p.getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, udp.getSender(), udp.getSenderPort());
		socket.send(packet);
	}
	
	@Override
	protected void messageTimeout() {
		this.shutdown();
	}

}
