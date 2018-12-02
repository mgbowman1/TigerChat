package ttp;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedList;

import util.Utility;

public class TON {
	
	LinkedList<Entry> entries;
	
	byte[] COMMA = new byte[] {','};
	
	public TON() {
		entries = new LinkedList<>();
	}
	
	public TON(byte[] bytes) {
		entries = new LinkedList<>();
		try {
			String s = new String(bytes, "UTF-8");
			if (s.charAt(0) == '[') {
				int index = 1;
				while (index < s.length()) {
					if (s.charAt(index) == '{') {
						String senderID = "";
						String conversationID = "";
						String timestamp = "";
						String message = "";
						index++;
						while (s.charAt(index) != ',') senderID += s.charAt(index++);
						index++;
						while (s.charAt(index) != ',') conversationID += s.charAt(index++);
						index++;
						while (s.charAt(index) != ',') timestamp += s.charAt(index++);
						index += 2;
						while (s.charAt(index) != '"') {
							message += s.charAt(index++);
							if (s.charAt(index) == '"' && s.charAt(index - 1) == '\\') message += s.charAt(index++);
						}
						entries.add(new Entry(senderID, conversationID, timestamp, message));
						entries.getLast().unEscape();
						index += 3;
					} else break;
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public void addEntry(String senderID, String conversationID, String timestamp, String message) {
		this.entries.add(new Entry(senderID, conversationID, timestamp, message));
	}
	
	public byte[] getBytes() {
		byte[] bytes = new byte[] {'['};
		Iterator<Entry> i = this.entries.iterator();
		while (i.hasNext()) {
			bytes = Utility.mergeBytes(new byte[][] {bytes, new byte[] {'{'}, i.next().getBytes(), new byte[] {'}', ','}});
		}
		bytes[bytes.length - 1] = ']';
		return bytes;
	}

	private class Entry {
		private String senderID;
		private String conversationID;
		private String timestamp;
		private String message;
		
		public Entry(String senderID, String conversationID, String timestamp, String message) {
			this.senderID = senderID;
			this.conversationID = conversationID;
			this.timestamp = timestamp;
			this.message = message;
		}
		
		public byte[] getBytes() {
			try {
				return Utility.mergeBytes(new byte[][] {
					senderID.getBytes("UTF-8"),
					COMMA,
					conversationID.getBytes("UTF-8"),
					COMMA,
					timestamp.getBytes("UTF-8"),
					COMMA,
					escape(message).getBytes("UTF-8")
				});
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return new byte[0];
			}
		}
		
		private String escape(String s) {
			String ret = "";
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (c == '\\' || c == '"') {
					ret += "\\" + c;
				} else ret += c;
			}
			return ret;
		}
		
		public void unEscape() {
			String ret = "";
			for (int i = 0; i < this.message.length(); i++) {
				char c = this.message.charAt(i);
				if (c == '\\') {
					ret += this.message.charAt(i + 1);
					i++;
				} else ret += c;
			}
			this.message = ret;
		}
	}
	
}
