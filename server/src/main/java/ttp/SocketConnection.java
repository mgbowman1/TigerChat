package ttp;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.HashMap;

import rdp.DataSocket;

public class SocketConnection extends Processor {
	
	int port;
	private DataSocket serverSocket;

	public SocketConnection(int port, String userID) throws SQLException {
		super();
		this.port = port;
		this.userID = userID;
	}
	
	public void setServerSocket(DataSocket serverSocket) {
		this.serverSocket = serverSocket;
	}
	
	public void setConnection(InetAddress address, int port) {
		this.serverSocket.setConnection(address, port);
	}

	@Override
	public int reconnect() {
		return 0;
	}

	@Override
	public void receive(TTPPacket packet) {
		HashMap<String, String> values = packet.getData();
		FlagType f = FlagType.values()[Integer.parseInt(values.get("flag"))];
		try {
			switch (f) {
			case MSG:
				super.handleSendMessage(values.get("conversationID"), values.get("message").getBytes("UTF-8"));
				break;
			case FIL:
				super.handleSendFile(values.get("conversationID"), Integer.parseInt(values.get("size")), values.get("name"), values.get("file").getBytes("UTF-8"));
				break;
			case RQM:
				this.serverSocket.addSend(new TTPPacket(super.handleGetMessageBlock(values.get("conversationID"), Integer.parseInt(values.get("messageBlockNumber")))));
				break;
			case CLS:
				close();
				break;
			case CCV:
				this.serverSocket.addSend(new TTPPacket(super.handleCreateConversation(values.get("usernames"))));
				break;
			default:
				System.out.println("Bad packet");
				break;
			}
		} catch (UnsupportedEncodingException | SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public void close() throws SQLException {
		super.close();
	}

}
