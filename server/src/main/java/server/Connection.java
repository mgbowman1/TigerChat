package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public abstract class Connection extends Thread {
	
	// The socket
	protected DatagramSocket socket;
	
	// The max message size and timeout time
	private int maxMessageSize = 10000;
	private int timeout = 500;
	
	// Running flag to keep the thread going
	private boolean running = true;
	
	// Integer to track how long it has been since receiving a message
	protected int failedReads = 0;
	
	public Connection(int port) throws SocketException {
		socket = new DatagramSocket(port);
		socket.setReceiveBufferSize(maxMessageSize);
		socket.setSendBufferSize(maxMessageSize);
		socket.setSoTimeout(timeout);
	}
	
	public void run() {
		while (running) {
			byte[] buffer = new byte[maxMessageSize];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			try {
				socket.receive(packet);
			} catch (SocketTimeoutException e) {
				failedReads++;
				if (failedReads > 7200) messageTimeout();
				continue;
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
			failedReads = 0;
			UDPPacket udp = new UDPPacket(packet);
			System.out.println("Received message: " + udp.getMessage() + " with error: " + udp.getError() + " from " + udp.getSender() + ":" + udp.getSenderPort());
			try {
				messageHandler(udp);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
		}
	}
	
	protected abstract void messageHandler(UDPPacket udp) throws IOException;
	protected abstract void messageTimeout();
	
	public void shutdown() {
		running = false;
		socket.close();
	}

}
