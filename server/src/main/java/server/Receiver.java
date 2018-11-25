package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

public class Receiver extends Thread {
	
	// The socket
	private DatagramSocket socket;
	
	// The port number
	private int port = 12000;
	
	// The queue of messages
	private Queue<Packet> messages;
	
	// The main thread running service
	private ExecutorService main;
	
	// Flag for running
	private boolean running = true;
	
	// The maximum message size
	private int maxMessageSize = 10000;
	
	public Receiver(ExecutorService es) throws SocketException {
		if (System.getProperty("user.dir").contains("current_server")) this.port = 11000;
		socket = new DatagramSocket(port);
		socket.setReceiveBufferSize(maxMessageSize);
		messages = new LinkedList<>();
		main = es;
	}
	
	public void run() {
		while (running) {
			byte[] buffer = new byte[maxMessageSize];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			try {
				socket.receive(packet);
				Packet p = new Packet(packet);
				messages.add(p);
				synchronized (main) {
					main.notify();
				}
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
		socket.close();
	}
	
	public Packet getNextMessage() {
		return messages.poll();
	}
	
	public void shutdown() throws IOException {
		running = false;
		Sender s = new Sender(InetAddress.getLocalHost(), port);
		s.send("");
		s.destroy();
		
	}
}
