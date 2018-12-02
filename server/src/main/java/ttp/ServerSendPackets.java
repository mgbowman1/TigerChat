package ttp;

public class ServerSendPackets {
	String user;
	TTPPacket packet;
	
	public ServerSendPackets(String user, TTPPacket packet) {
		this.user = user;
		this.packet = packet;
	}
}
