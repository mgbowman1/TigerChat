package server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientConnection extends UDPConnection {
	
	// The username of the user
	String username;
	
	// The mysql function calls
	CallableStatement sendMessage;
	CallableStatement newConversation;
	CallableStatement newerMessages;
	CallableStatement allMessages;
	CallableStatement myConversations;
	
	public ClientConnection(int port, String username) throws SocketException, SQLException, UnknownHostException {
		super(port);
		this.username = username;
		sendMessage = conn.prepareCall("{call add_message(?, '" + username + "', ?, ?)}");
		newConversation = conn.prepareCall("{call create_conversation(?, ?)}");
		newConversation.registerOutParameter(2, java.sql.Types.INTEGER);
		newerMessages = conn.prepareCall("{call get_new_messages(?, ?)}");
		allMessages = conn.prepareCall("SELECT * FROM messages WHERE conversation_id=?");
		myConversations = conn.prepareCall("SELECT conversation_id FROM conversations WHERE username='" + username + "'");
	}

	@Override
	protected void messageHandler(UDPPacket udp) throws IOException {
		String type = udp.getMessage().split(" ")[0];
		
		// Send the packet to the appropriate handler
		try {
			if (type.equals("send")) sendHandler(udp);
			else if (type.equals("makeConversation")) makeConversationHandler(udp);
			else if (type.equals("recentMessages")) recentMessagesHandler(udp);
			else if (type.equals("allMessages")) allMessagesHandler(udp);
			else if (type.equals("myConversations")) myConversationsHandler(udp);
			else if (type.equals("refresh")) failedReads = 0;
			else if (type.equals("close")) messageTimeout();
			else badPacketHandler(udp);
		} catch (SQLException e) {
			UDPPacket p = new UDPPacket(PacketError.DATABASEERROR, packetNumber);
			send(p, udp);
		}
	}
	
	private void sendHandler(UDPPacket udp) throws SQLException {
		String[] args = udp.getMessage().split(" ");
		
		// Error checks on message
		if (args.length < 4) {
			badPacketHandler(udp);
			return;
		} else if (!args[2].equals("t") && !args[2].equals("f") && !args[2].equals("i")) {
			badPacketHandler(udp);
			return;
		}
		int id;
		try {
			id = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			badPacketHandler(udp);
			return;
		}
		
		// Put together the actual message if there were any spaces in it
		for (int i = 4; i < args.length; i++) {
			args[3] += " " + args[i];
		}
		
		// Send the message to the database and let the client know
		sendMessage.setInt(1, id);
		sendMessage.setBlob(2, new ByteArrayInputStream(args[3].getBytes()));
		sendMessage.setString(3, args[2]);
		sendMessage.execute();
		UDPPacket p = new UDPPacket("Success " + Long.toString(udp.getPacketNumber()), packetNumber);
		send(p, udp);
	}
	
	private void makeConversationHandler(UDPPacket udp) throws SQLException {
		String[] args = udp.getMessage().split(" ");
		
		// Error checks on message
		if (args.length < 2) {
			badPacketHandler(udp);
			return;
		}
		
		// Put together the actual users if there were any spaces in them
		for (int i = 2; i < args.length; i++) {
			args[1] += " " + args[i];
		}
		
		// Create the conversation and let the client know
		newConversation.setString(1, this.username + "," + args[1]);
		newConversation.execute();
		int conv = newConversation.getInt(2);
		String mes = "Success ";
		if (conv == -1) mes = "Failure ";
		UDPPacket p = new UDPPacket(mes + Long.toString(udp.getPacketNumber()), packetNumber);
		send(p, udp);
	}
	
	private void recentMessagesHandler(UDPPacket udp) throws SQLException, IOException {
		String[] args = udp.getMessage().split(" ");
		
		// Error checks on message
		if (args.length != 4) {
			badPacketHandler(udp);
			return;
		}
		int id;
		try {
			id = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			badPacketHandler(udp);
			return;
		}
		String[] date = args[2].split("[-]");
		if (date.length != 3) {
			badPacketHandler(udp);
			return;
		}
		try {
			Integer.parseInt(date[0]);
			Integer.parseInt(date[1]);
			Integer.parseInt(date[2]);
		} catch (NumberFormatException e) {
			badPacketHandler(udp);
			return;
		}
		String[] time = args[3].split("[:]");
		if (time.length != 3) {
			badPacketHandler(udp);
			return;
		}
		try {
			Integer.parseInt(time[0]);
			Integer.parseInt(time[1]);
			Double.parseDouble(time[2]);
		} catch (NumberFormatException e) {
			badPacketHandler(udp);
			return;
		}
		String timestamp = args[2] + " " +  args[3];
		
		// Get the messages and send them to the client
		newerMessages.setInt(1, id);
		newerMessages.setString(2, timestamp);
		newerMessages.execute();
		sendMessageList(newerMessages.getResultSet(), udp);
	}
	
	private void allMessagesHandler(UDPPacket udp) throws SQLException, IOException {
		String[] args = udp.getMessage().split(" ");
		
		// Error checks on message
		if (args.length != 2) {
			badPacketHandler(udp);
			return;
		}
		int id;
		try {
			id = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			badPacketHandler(udp);
			return;
		}
		
		// Get the messages and send them to the client
		allMessages.setInt(1, id);
		allMessages.execute();
		sendMessageList(allMessages.getResultSet(), udp);
	}
	
	private void myConversationsHandler(UDPPacket udp) throws SQLException {
		myConversations.execute();
		ResultSet r = myConversations.getResultSet();
		while (r.next()) {
			UDPPacket p = new UDPPacket(Integer.toString(r.getInt(1)), packetNumber);
			send(p, udp);
		}
		UDPPacket p = new UDPPacket("End", packetNumber);
		send(p, udp);
	}
	
	private void sendMessageList(ResultSet r, UDPPacket udp) throws SQLException, IOException {
		while (r.next()) {
			UDPPacket p = new UDPPacket(Integer.toString(r.getInt(1)) + " " + r.getString(3) + " " + r.getString(4), packetNumber);
			send(p, udp);
			byte[] arr = new byte[0];
			InputStream b = r.getBlob(2).getBinaryStream();
			while (b.available() > 0) {
				byte[] newArr = new byte[b.available()];
				int total = b.read(newArr);
				if (total < newArr.length) {
					byte[] temp = new byte[total];
					for (int i = 0; i < total; i++) {
						temp[i] = newArr[i];
					}
					newArr = temp;
				}
				arr = combineBytes(arr, newArr);
			}
			p = new UDPPacket(new String(arr, "UTF-8"), packetNumber);
			send(p, udp);
		}
		UDPPacket p = new UDPPacket("End", packetNumber);
		send(p, udp);
	}
	
	private void badPacketHandler(UDPPacket udp) {
		UDPPacket p = new UDPPacket(Long.toString(udp.getPacketNumber()), PacketError.MALFORMEDPACKET, packetNumber);
		send(p, udp);
	}
	
	@Override
	protected void messageTimeout() {
		try {
			sendMessage.close();
			newConversation.close();
			newerMessages.close();
			allMessages.close();
			myConversations.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		this.shutdown();
	}

}
