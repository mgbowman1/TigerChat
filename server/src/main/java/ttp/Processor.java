package ttp;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.mysql.cj.jdbc.MysqlDataSource;

import util.Utility;

public abstract class Processor {
	protected Connection conn;
	
	// The sql queries
	protected CallableStatement sendMessage;
	protected CallableStatement getConversationUsers;
	protected CallableStatement sendFile;
	protected CallableStatement getMessageBlock;
	protected CallableStatement login;
	protected CallableStatement createConversation;
	protected CallableStatement getUserConversations;
	
	// The username
	protected String userID;
	
	public Processor() throws SQLException {
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setUser("root");
		dataSource.setPassword("root");
		dataSource.setServerName("localhost");
		dataSource.setDatabaseName("tiger_chat");
		conn = dataSource.getConnection();
		sendMessage = conn.prepareCall("INSERT INTO CONVERSATION_HISTORY (conversation_id, conversation_data, type, created_by_user_id) VALUES (?, ?, 'm', ?)");
		getConversationUsers = conn.prepareCall("SELECT user_id_list FROM CONVERSATION WHERE conversation_id=? LIMIT 1");
		sendFile = conn.prepareCall("INSERT INTO CONVERSATION_HISTORY (conversation_id, conversation_data, type, created_by_user_id) VALUES (?, ?, 'f', ?)");
		getMessageBlock = conn.prepareCall("{call request_message(?, ?)}");
		login = conn.prepareCall("{? = call login(?, ?)}");
		login.registerOutParameter(1, Types.VARCHAR);
		createConversation = conn.prepareCall("{call create_conversation(?, ?)}");
		getUserConversations = conn.prepareCall("{call request_user_conversations(?)}");
	}
	
	private InputStream getBlob(byte[] bytes) {
		return new ByteArrayInputStream(bytes);
	}
	
	private String getTimestamp() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}
	
	protected void handleSendMessage(String conversationID, byte[] message) throws SQLException {
		sendMessage.setString(1, conversationID);
		sendMessage.setBlob(2, getBlob(message));
		sendMessage.setString(3, this.userID);
		sendMessage.execute();
		String timestamp = getTimestamp();
		ArrayList<String> ids = handleGetConversationUsers(conversationID);
		for (int i = 0; i < ids.size(); i++) {
			if (!ids.get(i).equals(this.userID)) {
				try {
					TTPPacket ttp = new TTPPacket(this.userID, conversationID, timestamp, new String(message, "UTF-8"));
					Server.addPacket(ids.get(i), ttp);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		Server.sendAllPackets();
	}
	
	protected ArrayList<String> handleGetConversationUsers(String conversationID) throws SQLException {
		ArrayList<String> res = new ArrayList<>();
		getConversationUsers.setString(1, conversationID);
		getConversationUsers.execute();
		ResultSet r = getConversationUsers.getResultSet();
		r.next();
		String[] s = r.getString(1).split(",");
		for (int i = 0; i < s.length; i++) {
			res.add(s[i].trim());
		}
		return res;
	}
	
	protected void handleSendFile(String conversationID, byte[] file) throws SQLException {
		sendFile.setString(1, conversationID);
		sendFile.setBlob(2, getBlob(file));
		sendFile.setString(3, this.userID);
		sendFile.execute();
		String timestamp = getTimestamp();
		ArrayList<String> ids = handleGetConversationUsers(conversationID);
		for (int i = 0; i < ids.size();i ++) {
			if (!ids.get(i).equals(this.userID)) {
				try {
					int size = Utility.byteToInt(file, 0);
					int nameSize = Utility.byteToInt(file, 4);
					String name = new String(Utility.splitBytes(file, 8, 8 + nameSize), "UTF-8");
					TTPPacket ttp = new TTPPacket(this.userID, conversationID, timestamp, name, size);
					Server.addPacket(ids.get(i), ttp);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		Server.sendAllPackets();
	}
	
	public TON handleGetMessageBlock(String conversationID, int blockNumber) throws SQLException {
		getMessageBlock.setString(1, conversationID);
		getMessageBlock.setInt(2, blockNumber);
		getMessageBlock.execute();
		TON t = new TON();
		ResultSet r = getMessageBlock.getResultSet();
		while (!r.isAfterLast()) {
			r.next();
			Blob message = r.getBlob("conversationi_data");
			try {
				t.addEntry(r.getString("created_by_user_id"), conversationID, r.getString("created"), new String(message.getBytes(1L, (int) message.length()), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return t;
	}
	
	public String handleLogin(String username, String password) throws SQLException {
		login.setString(2, username);
		login.setString(3, password);
		login.execute();
		String id = login.getString(1);
		if (id.equals("null")) id = "";
		return id;
	}
	
	public String handleCreateConversation(String userList) throws SQLException {
		createConversation.setString(1, userList);
		createConversation.setString(2, this.userID);
		createConversation.execute();
		ResultSet r = createConversation.getResultSet();
		r.next();
		return r.getString("conv_id");
	}
	
	public ArrayList<String> handleGetUserConversations() throws SQLException {
		getUserConversations.setString(1, this.userID);
		getUserConversations.execute();
		ResultSet r = getUserConversations.getResultSet();
		ArrayList<String> res = new ArrayList<>();
		while (!r.isAfterLast()) {
			r.next();
			res.add(r.getString("conversation_id"));
		}
		return res;
	}
	
	public abstract int reconnect();
	public abstract void receive(TTPPacket packet);
	public abstract int getPort();
	public void close() throws SQLException {
		this.createConversation.close();
		this.getConversationUsers.close();
		this.getMessageBlock.close();
		this.getUserConversations.close();
		this.login.close();
		this.sendFile.close();
		this.sendMessage.close();
		this.conn.close();
	}
}
