package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

public abstract class UDPConnection extends Thread {
	
	// The socket and port
	protected DatagramSocket socket;
	protected int port;
	
	// The max message size and timeout time
	private int maxMessageSize = 10000;
	private int timeout = 500;
	
	// Running flag to keep the thread going
	private boolean running = true;
	
	// Integer to track how long it has been since receiving a message and max timeout
	protected int failedReads = 0;
	protected int maxFailedReads = 10;
	
	// The mysql connection
	protected Connection conn;
	
	// The current packet number
	protected long packetNumber;
	
	// The last packet received from a client
	private DatagramPacket lastReceived;
	
	public UDPConnection(int port) throws SocketException, SQLException, UnknownHostException {
		this.port = port;
		lastReceived = new DatagramPacket(new byte[0], 0, InetAddress.getLocalHost(), 0);
		socket = new DatagramSocket(port);
		socket.setReceiveBufferSize(maxMessageSize);
		socket.setSendBufferSize(maxMessageSize);
		socket.setSoTimeout(timeout);
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setUser("root");
		dataSource.setPassword("root");
		dataSource.setServerName("localhost");
		dataSource.setDatabaseName("tiger_chat");
		conn = dataSource.getConnection();
		packetNumber = 0;
	}
	
	public void run() {
		while (running) {
			UDPPacket udp = receive();
			if (udp == null) continue;
			System.out.println("Received message: " + udp.getMessage() + " with error: " + udp.getError() + " from " + udp.getSender() + ":" + udp.getSenderPort());
			try {
				messageHandler(udp);
			} catch (IOException | SQLException e) {
				e.printStackTrace();
				continue;
			}
		}
	}
	
	private boolean datagramsEqual(DatagramPacket a, DatagramPacket b) {
		if (!a.getAddress().equals(b.getAddress()) || a.getPort() != b.getPort() || a.getLength() != b.getLength()) return false;
		byte[] aData = a.getData();
		byte[] bData = b.getData();
		for (int i = 0; i < aData.length; i++) {
			if (aData[i] != bData[i]) return false;
		}
		return true;
	}
	
	protected UDPPacket receive() {
		byte[] buffer = new byte[maxMessageSize];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		try {
			socket.receive(packet);
			UDPPacket p = new UDPPacket(packet);
			System.out.println("Received " + p.getMessage());
			UDPPacket ack = new UDPPacket("Ack " + Long.toString(p.getPacketNumber()), packetNumber++);
			byte[] b = ack.getBytes();
			DatagramPacket packet2 = new DatagramPacket(b, b.length, p.getSender(), p.getSenderPort());
			socket.send(packet2);
			failedReads = 0;
			if (datagramsEqual(lastReceived, packet)) return null;
			else {
				lastReceived = packet;
				return p;
			}
		} catch(SocketTimeoutException e) {
			failedReads++;
			if (failedReads > maxFailedReads) messageTimeout();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected boolean send(DatagramPacket packet) {
		try {
			int attempts = 0;
			while (attempts < maxFailedReads) {
				socket.send(packet);
				try {
					byte[] buffer = new byte[maxMessageSize];
					DatagramPacket p = new DatagramPacket(buffer, buffer.length);
					socket.receive(p);
					UDPPacket pack = new UDPPacket(p);
					if (pack.getMessage().equals("Ack " + Long.toString(packetNumber))) return true;
					else if (pack.getError().equals(PacketError.MISSEDPACKET)) attempts = -1;
					attempts++;
				} catch (SocketTimeoutException e) {
					attempts++;
					continue;
				} catch (IOException e) {
					e.printStackTrace();
					messageTimeout();
					return false;
				}
			}
			if (attempts == maxFailedReads) {
				messageTimeout();
				return false;
			}
			packetNumber++;
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	protected boolean send(UDPPacket udp, InetAddress add, int port) {
		System.out.println("Sending " + udp.getMessage());
		byte[] buffer = udp.getBytes();
		return send(new DatagramPacket(buffer, buffer.length, add, port));
	}
	
	protected boolean send(UDPPacket udp, UDPPacket origin) {
		return send(udp, origin.getSender(), origin.getSenderPort());
	}
	
	protected byte[] combineBytes(byte[] a, byte[] b) {
		byte[] c = new byte[a.length + b.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i];
		}
		for (int i = 0; i < b.length; i++) {
			c[i + a.length] = b[i];
		}
		return c;
	}
	
	protected abstract void messageHandler(UDPPacket udp) throws IOException, SQLException;
	protected abstract void messageTimeout();
	
	public boolean stillRunning() {
		return running;
	}
	
	public boolean hasTime() {
		return failedReads <= maxFailedReads - 2;
	}
	
	public void shutdown() {
		running = false;
		socket.close();
	}

}
