package server;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class UDPPacket {
	
	// The sending IP and port
	private InetAddress sender;
	private int senderPort;
	
	// The packet number
	private long packetNumber;
	
	// The actual message being sent
	private String message;
	
	// Error flag
	/*
	 * 1: Invalid login
	 */
	private int error;
	
	public UDPPacket(String message, long packetNumber) {
		this.message = message;
		this.error = 0;
		this.packetNumber = packetNumber;
	}
	
	public UDPPacket(PacketError error, long packetNumber) {
		this.message = "";
		this.error = error.getValue();
		this.packetNumber = packetNumber;
	}
	
	public UDPPacket(String message, PacketError error, long packetNumber) {
		this.message = message;
		this.error = error.getValue();
		this.packetNumber = packetNumber;
	}
	
	public UDPPacket(DatagramPacket p) {
		byte[] data = p.getData();
		int messageSize = (data[0] << 8) + data[1];
		byte[] buffer = new byte[messageSize];
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = data[i + 14];
		}
		byte[] err = new byte[4];
		byte[] num = new byte[8];
		for (int i = 0; i < err.length; i++) {
			err[i] = data[i + 2];
		}
		for (int i = 0; i < num.length; i++) {
			num[i] = data[i + err.length + 2];
		}
		this.message = new String(buffer);
		this.error = getInt(err);
		this.packetNumber = getLong(num);
		this.sender = p.getAddress();
		this.senderPort = p.getPort();
	}
	
	private byte[] getBytes(int i) {
		byte[] b = new byte[4];
		b[0] = (byte) (i >>> 24);
		b[1] = (byte) (i >>> 16);
		b[2] = (byte) (i >>> 8);
		b[3] = (byte) i;
		return b;
	}
	
	private byte[] getBytes(long i) {
		byte[] b = new byte[8];
		b[0] = (byte) (i >>> 56);
		b[1] = (byte) (i >>> 48);
		b[2] = (byte) (i >>> 40);
		b[3] = (byte) (i >>> 32);
		b[4] = (byte) (i >>> 24);
		b[5] = (byte) (i >>> 16);
		b[6] = (byte) (i >>> 8);
		b[7] = (byte) i;
		return b;
	}
	
	private int getInt(byte[] b) {
		int total = 0;
		for (int i = 0; i < b.length; i++) {
			total += (b[i] << (8 * (b.length - i - 1)));
		}
		return total;
	}
	
	private long getLong(byte[] b) {
		long total = 0;
		for (int i = 0; i < b.length; i++) {
			total += (b[i] << (8 * (b.length - i - 1)));
		}
		return total;
	}
	
	private byte[] combineBytes(byte[] a, byte[] b) {
		byte[] c = new byte[a.length + b.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i];
		}
		for (int i = 0; i < b.length; i++) {
			c[i + a.length] = b[i];
		}
		return c;
	}
	
	public byte[] getBytes() {
		return combineBytes(combineBytes(combineBytes(new byte[] {(byte) (message.length() >>> 8), (byte) message.length()}, getBytes(error)), getBytes(packetNumber)), message.getBytes());
	}
	
	public long getPacketNumber() {
		return packetNumber;
	}
	
	public String getMessage() {
		return message;
	}
	
	public PacketError getError() {
		return PacketError.values()[error];
	}
	
	public InetAddress getSender() {
		return sender;
	}
	
	public int getSenderPort() {
		return senderPort;
	}

}
