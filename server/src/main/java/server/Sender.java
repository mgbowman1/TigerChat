package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Sender {
	
	// The socket and destination info
	private DatagramSocket socket;
	private InetAddress target;
	private int targetPort;
	
	public Sender(InetAddress target, int targetPort) throws SocketException {
		socket = new DatagramSocket();
		this.target = target;
		this.targetPort = targetPort;
	}
	
	public void send(byte[] message) throws IOException {
		DatagramPacket packet = new DatagramPacket(message, message.length, target, targetPort);
		socket.send(packet);
	}
	
	public void send(String message) throws IOException {
		this.send(new Packet(message).getBytes());
	}
	
	public void destroy() {
		socket.close();
	}
}
