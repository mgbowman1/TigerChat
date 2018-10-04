package models;

import java.nio.charset.Charset;

enum UDPType {
	// Messages the server receives
	SENDMESSAGE,
	GETHISTORY,
	GETCONVERSATIONS,
	AUTHENTICATE,
	CLIENTACK,
	// Messages the client receives
	BADPACKET,
	CONVERSATIONHISTORY,
	CONVERSATIONS,
	AUTHENTICATION,
	SERVERACK;
}

public class UDPPacket {

	// The type of the packet
	private UDPType type;
	
	// The authentication and data
	private String JWT;
	private String data;
	
	public UDPPacket(UDPType type, String JWT, String data) {
		this.type = type;
		this.JWT = JWT;
		this.data = data;
	}
	
	public UDPPacket(byte[] bytes) {
		if (bytes.length < 6) throw new NumberFormatException();
		int typeLen = (int) (bytes[0]) * 127 + (int) (bytes[1]);
		int JWTLen = (int) (bytes[2]) * 127 + (int) (bytes[3]);
		int dataLen = (int) (bytes[4]) * 127 + (int) (bytes[5]);
		if (bytes.length != 6 + typeLen + JWTLen + dataLen) throw new NumberFormatException();
		int type = 0;
		byte[] JWT = new byte[JWTLen];
		byte[] data = new byte[dataLen];
		for (int i = 6; i < typeLen + 6; i++) {
			type += (int) bytes[i];
		}
		for (int i = 6 + typeLen; i < JWTLen + typeLen + 6; i++) {
			JWT[i - 6 - typeLen] = bytes[i];
		}
		for (int i = 6 + typeLen + JWTLen; i < bytes.length; i++) {
			data[i - 6 - typeLen - JWTLen] = bytes[i];
		}
		this.type = UDPType.values()[type];
		this.JWT = new String(JWT, Charset.forName("UTF-8"));
		this.data = new String(data, Charset.forName("UTF-8"));
	}
	
	public byte[] getBytes() {
		byte[] bType = new byte[(int) Math.ceil(type.ordinal() / 127.0)];
		for (int i = 0; i < bType.length - 1; i++) {
			bType[i] = (byte) 127;
		}
		bType[bType.length - 1] = (byte) ((type.ordinal() % 127) + 1);
		byte[] bJWT = JWT.getBytes(Charset.forName("UTF-8"));
		byte[] bData = data.getBytes(Charset.forName("UTF-8"));
		byte[] ret = new byte[6 + bType.length + bJWT.length + bData.length];
		ret[0] = (byte) (bType.length / 127);
		ret[1] = (byte) (bType.length % 127);
		ret[2] = (byte) (bJWT.length / 127);
		ret[3] = (byte) (bJWT.length % 127);
		ret[4] = (byte) (bData.length / 127);
		ret[5] = (byte) (bData.length % 127);
		for (int i = 0; i < bType.length; i++) {
			ret[i + 6] = bType[i];
		}
		for (int i = 0; i < bJWT.length; i++) {
			ret[i + bType.length + 6] = bJWT[i];
		}
		for (int i = 0; i < bData.length; i++) {
			ret[i + bJWT.length + bType.length + 6] = bData[i];
		}
		return ret;
	}

	public UDPType getType() {
		return type;
	}

	public String getJWT() {
		return JWT;
	}

	public String getData() {
		return data;
	}
	
}
