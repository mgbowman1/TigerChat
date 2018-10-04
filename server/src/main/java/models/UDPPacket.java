package models;

import java.nio.charset.Charset;

/* Types:
 * SENDMESSAGE:
 * 	Client sends to server to send a message to another user.
 * 	First packet of this type will contain the user to send to, second will contain the message.
 * 
 * 	Server sends to client to show message from another user.
 * 	First packet of this type will contain the user it was sent from, second will contain the message.
 * IMAGE:
 * 	Client sends to server to send an image to another user.
 * 	First packet contains the user to send to, second contains the number of packets to be sent.
 * 	Each subsequent packet contains the data for the image.
 * 
 * 	Server sends to client to show image from another user.
 * 	First packet of this type will contain the user it was sent from.
 * 	Second will contain the number of packets to be sent.
 * 	Each subsequent packet contains the data for the image.
 * FILE:
 * 	Client sends to server to send a file to another user.
 * 	First packet contains tthe user to send to, second contains the number of packets to be sent.
 * 	Each subsequent packet contains theh data for the file.
 * 
 * 	Server sends to client to show file from another user.
 * 	First packet of this type will contain the user it was sent from.
 * 	Second will contain the number of packets to be sent.
 * 	Each subsequent packet contains the data for thhe file.
 * CONVERSATIONHISTORY:
 * 	Client sends to server to ask for the history of a conversation.
 * 	First packet of this type will contain the conversation to get the history from, second will
 * 		contain which page of the history to get or -1 to get the most recent one.
 * 
 * 	Server sends to client to give history of a conversation.
 * 	First packet of this type will contain the page number, second will contain the number of messages.
 * 	Each subsequent packet will contain a single message.
 * CONVERSATIONS:
 * 	Client sends to server to ask for all the conversations the user is a part of.
 * 	First packet, and only packet, contains no data, just authentication.
 * 
 * 	Server sends to client to give all conversations the user is a part of.
 * 	First packet contains the number of conversations.
 * 	Each subsequent packet will contain information on a single conversation.
 * AUTHENTICATE:
 * 	Client sends to server to attempt to authenticate a login.
 * 	First packet, and only packet, contains the username and password, but no JWT.
 * 
 * 	Server sends to client to show authentication success or failure.
 * 	First packet, and only packet, contains data showing the success or failure of the authentication.
 * 	It also contains a JWT if it was successful or no JWT if it was not.
 * ACK:
 * 	Client or server send to each other to show which packets were received.
 * 	First packet, and only packet, contains a list of each packet number that was received.
 * 	Note that an ACK packet is always sent as a response to a received packet for both client and server.
 */
enum UDPType {
	SENDMESSAGE,
	IMAGE,
	FILE,
	CONVERSATIONHISTORY,
	CONVERSATIONS,
	AUTHENTICATE,
	ACK;
}

public class UDPPacket {
	
	// The number of the packet
	private short number;

	// The type of the packet
	private UDPType type;
	
	// The authentication and data
	private String JWT;
	private String data;
	
	// Longest allowed data size
	private int maxDataSize = 9000;
	
	public UDPPacket(short number, UDPType type, String JWT, String data) {
		this.number = number;
		this.type = type;
		this.JWT = JWT;
		if (data.length() > maxDataSize) throw new RuntimeException();
		this.data = data;
	}
	
	public UDPPacket(byte[] bytes) {
		if (bytes.length < 8) throw new RuntimeException();
		int numberLen = (int) (bytes[0]) * 127 + (int) (bytes[1]);
		int typeLen = (int) (bytes[2]) * 127 + (int) (bytes[3]);
		int JWTLen = (int) (bytes[4]) * 127 + (int) (bytes[5]);
		int dataLen = (int) (bytes[6]) * 127 + (int) (bytes[7]);
		if (dataLen > maxDataSize) throw new RuntimeException();
		if (bytes.length != 8 + numberLen + typeLen + JWTLen + dataLen) throw new RuntimeException();
		short number = 0;
		int type = 0;
		byte[] JWT = new byte[JWTLen];
		byte[] data = new byte[dataLen];
		for (int i = 8; i < numberLen + 8; i++) {
			number += (short) bytes[i];
		}
		for (int i = 8 + numberLen; i < typeLen + numberLen + 8; i++) {
			type += (int) bytes[i];
		}
		for (int i = 8 + numberLen + typeLen; i < JWTLen + typeLen + numberLen + 8; i++) {
			JWT[i - 8 - numberLen - typeLen] = bytes[i];
		}
		for (int i = 8 + numberLen + typeLen + JWTLen; i < bytes.length; i++) {
			data[i - 8 - numberLen - typeLen - JWTLen] = bytes[i];
		}
		this.number = number;
		this.type = UDPType.values()[type];
		this.JWT = new String(JWT, Charset.forName("UTF-8"));
		this.data = new String(data, Charset.forName("UTF-8"));
	}
	
	public byte[] getBytes() {
		byte[] bNumber = new byte[(int) Math.ceil(number / 127.0)];
		for (int i = 0; i < bNumber.length - 1; i++) {
			bNumber[i] = (byte) 127;
		}
		bNumber[bNumber.length - 1] = (byte) (number + 1);
		byte[] bType = new byte[(int) Math.ceil(type.ordinal() / 127.0)];
		for (int i = 0; i < bType.length - 1; i++) {
			bType[i] = (byte) 127;
		}
		bType[bType.length - 1] = (byte) ((type.ordinal() % 127) + 1);
		byte[] bJWT = JWT.getBytes(Charset.forName("UTF-8"));
		byte[] bData = data.getBytes(Charset.forName("UTF-8"));
		byte[] ret = new byte[6 + bType.length + bJWT.length + bData.length];
		ret[0] = (byte) (bNumber.length / 127);
		ret[1] = (byte) (bNumber.length % 127);
		ret[2] = (byte) (bType.length / 127);
		ret[3] = (byte) (bType.length % 127);
		ret[4] = (byte) (bJWT.length / 127);
		ret[5] = (byte) (bJWT.length % 127);
		ret[6] = (byte) (bData.length / 127);
		ret[7] = (byte) (bData.length % 127);
		for (int i = 0; i < bNumber.length; i++) {
			ret[i + 8] = bNumber[i];
		}
		for (int i = 0; i < bType.length; i++) {
			ret[i + bNumber.length + 8] = bType[i];
		}
		for (int i = 0; i < bJWT.length; i++) {
			ret[i + bType.length + bNumber.length + 8] = bJWT[i];
		}
		for (int i = 0; i < bData.length; i++) {
			ret[i + bJWT.length + bType.length + bNumber.length + 8] = bData[i];
		}
		return ret;
	}
	
	public short getNumber() {
		return number;
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
