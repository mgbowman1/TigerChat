package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Server {

	public static void main(String[] args) {
		try {
			new Connection().start();
			DatagramSocket socket = new DatagramSocket();
			byte[] buf = "tester".getBytes();
			DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("localhost"), 11000);
			socket.send(packet);
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
