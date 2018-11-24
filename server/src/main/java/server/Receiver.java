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
	private int port = 11000;
	
	// The queue of messages
	private Queue<DatagramPacket> messages;
	
	// The main thread running service
	ExecutorService main;
	
	// Flag for running
	private boolean running = true;
	
	public Receiver(ExecutorService es) throws SocketException {
		socket = new DatagramSocket(port);
		messages = new LinkedList<>();
		main = es;
	}
	
	public void run() {
		while (running) {
			byte[] buffer = new byte[10000];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			try {
				socket.receive(packet);
				byte[] arr = packet.getData();
				for (int i = 0; i < arr.length; i++) {
					if (arr[i] == 0) {
						packet.setLength(i);
						break;
					}
				}
				messages.add(packet);
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
	
	public DatagramPacket getNextMessage() {
		return messages.poll();
	}
	
	public void shutdown() throws IOException {
		running = false;
		Sender s = new Sender(InetAddress.getLocalHost(), port);
		s.send("");
		s.destroy();
		
	}
}
