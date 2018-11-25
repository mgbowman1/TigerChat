package server;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class Packet {
	
	// The sending IP and port
	private InetAddress sender;
	private int senderPort;
	
	// The actual message being sent
	private String message;
	
	// Error flag
	/*
	 * 1: Invalid login
	 */
	private int error;
	
	public Packet(String message) {
		this.message = message;
		this.error = 0;
	}
	
	public Packet(int error) {
		this.message = "";
		this.error = error;
	}
	
	public Packet(DatagramPacket p) {
		int index = 4;
		byte[] data = p.getData();
		while (data[index] != 0) {
			index++;
		}
		index -= 4;
		byte[] buffer = new byte[index];
		for (int i = 0; i < index; i++) {
			buffer[i] = data[i + 4];
		}
		this.message = new String(buffer);
		this.error = data[3] + (data[2] << 8) + (data[1] << 16) + (data[0] << 24);
		this.sender = p.getAddress();
		this.senderPort = p.getPort();
	}
	
	public byte[] getBytes() {
		byte[] mes = message.getBytes();
		byte[] finalArr = new byte[4 + mes.length];
		finalArr[0] = (byte) (error >>> 24);
		finalArr[1] = (byte) (error >>> 16);
		finalArr[2] = (byte) (error >>> 8);
		finalArr[3] = (byte) error;
		for (int i = 4; i < finalArr.length; i++) {
			finalArr[i] = mes[i - 4];
		}
		return finalArr;
	}
	
	public String getMessage() {
		return message;
	}
	
	public int getError() {
		return error;
	}
	
	public InetAddress getSender() {
		return sender;
	}
	
	public int getSenderPort() {
		return senderPort;
	}

}
