package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Connection extends Thread{
	
	// The socket and buffer
	private DatagramSocket socket;
	private byte[] buffer = new byte[10000];
	
	public Connection() throws SocketException {
		socket = new DatagramSocket(11000);
	}
	
	@Override
	public void run() {
		while (true) {
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			try {
				socket.receive(packet);
				System.out.println(new String(packet.getData(), 0, packet.getData().length));
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
		socket.close();
	}

}
