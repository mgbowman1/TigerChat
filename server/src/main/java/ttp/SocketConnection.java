package ttp;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.HashMap;

public class SocketConnection extends Processor {
	
	int port;

	public SocketConnection(int port) throws SQLException {
		super();
		this.port = port;
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
				super.handleSendFile(values.get("conversationID"), values.get("file").getBytes("UTF-8"));
				break;
			case RQM:
				super.handleGetMessageBlock(values.get("conversationID"), Integer.parseInt(values.get("messageBlockNumber")));
				break;
			case CLS:
				close();
				break;
			case CCV:
				super.handleCreateConversation(values.get("usernames"));
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
