package ttp;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import util.Utility;

public class TTPPacket {

	// The flag header and data
	private FlagType flag;
	private byte[] data;
	
	// Main used delimiter byte
	private final char DELIM = '|';
	private final byte[] DELIMARR = new byte[] {DELIM};
	
	public TTPPacket(byte[] bytes) {
		this.flag = FlagType.values()[(int) bytes[0]];
		this.data = Utility.splitBytes(bytes, 1);
	}
	
	// MSG
	public TTPPacket(String senderID, String conversationID, String timestamp, String message) {
		this.flag = FlagType.MSG;
		try {
			this.data = Utility.mergeBytes(new byte[][] {
				senderID.getBytes("UTF-8"),
				DELIMARR,
				conversationID.getBytes("UTF-8"),
				DELIMARR,
				timestamp.getBytes("UTF-8"),
				DELIMARR,
				message.getBytes("UTF-8")});
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	// INF
	public TTPPacket(String senderID, String conversationID, String timestamp, String fileName, int size) {
		this.flag = FlagType.INF;
		try {
			this.data = Utility.mergeBytes(new byte[][] {
				senderID.getBytes("UTF-8"),
				DELIMARR,
				conversationID.getBytes("UTF-8"),
				DELIMARR,
				timestamp.getBytes("UTF-8"),
				DELIMARR,
				Integer.toString(size).getBytes("UTF-8"),
				DELIMARR,
				fileName.getBytes("UTF-8")});
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	// Missing RQM for client
	
	// RQM
	public TTPPacket(TON o) {
		this.flag = FlagType.RQM;
		this.data = o.getBytes();
	}
	
	// Missing FRG for client
	
	// Missing CON for client
	
	// CON
	public TTPPacket() {
		this.flag = FlagType.CON;
		this.data = new byte[0];
	}
	
	// CON with port
	public TTPPacket(int port) {
		this.flag = FlagType.CON;
		this.data = Utility.intToByte(port);
	}
	
	// Missing CLS for client
	
	// Missing CCV for client
	
	// CCV
	public TTPPacket(String conversationID) {
		this.flag = FlagType.CCV;
		try {
			this.data = conversationID.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	// Convert most any packet into a hashmap
	public HashMap<String, Object> getData() {
		HashMap<String, Object> o = new HashMap<>();
		o.put("flag", this.flag);
		String[] s = new String[0];
		try {
			s = new String(this.data, "UTF-8").split("[|]");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// Add different data depending on the flag
		switch (this.flag) {
		case MSG:
			o.put("senderID", s[0]);
			o.put("conversationID", s[1]);
			o.put("timestamp", s[2]);
			o.put("message", rebuildString(s, 3));
			break;
		case INF:
			o.put("senderID", s[0]);
			o.put("conversationID", s[1]);
			o.put("timestamp", s[2]);
			o.put("size", s[3]);
			o.put("fileName", rebuildString(s, 4));
			break;
		case RQM: // Note this is only for getting client RQM
			o.put("conversationID", s[0]);
			o.put("messageBlockNumber", s[1]);
			break;
		case CON: // Note that FRG was skipped and this is only for getting client CON
			o.put("username", s[0]);
			o.put("password", s[1]);
			break;
		case CCV: // Note that CLS was skipped and this is only for getting client CCV
			for (int i = 0; i < s.length; i++) {
				o.put("username" + Integer.toString(i), s[i]);
			}
			break;
		default:
			System.out.println("Bad attempt to get data");
			break;
		}
		return o;
	}
	
	public byte[] getBytes() {
		return Utility.mergeBytes(new byte[] {(byte) this.flag.getValue()}, this.data);
	}
	
	private String rebuildString(String[] s, int offset) {
		String ret = "";
		for (int i = offset; i < s.length; i++) {
			ret += s[i];
		}
		return ret;
	}
	
}
