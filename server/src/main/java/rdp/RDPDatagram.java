package rdp;

import ttp.TTPPacket;
import util.Utility;

public class RDPDatagram {
	
	// List of headers and data field
	private int sequence;
	private int acknowledgement;
	private int head;
	private int tail;
	private byte[] data;
	
	// Datagram sequence number tracker
	private static int currentSequenceNumber = 1;
	
	public RDPDatagram(byte[] bytes) {
		this.sequence = Utility.byteToInt(bytes, 0);
		this.acknowledgement = Utility.byteToInt(bytes, 4);
		this.head = Utility.byteToInt(bytes, 8);
		this.tail = Utility.byteToInt(bytes, 12);
		this.data = Utility.splitBytes(bytes, 16);
	}
	
	public RDPDatagram(int acknowledgement, int head, int tail, TTPPacket packet) {
		this.sequence = getNextSequenceNumber();
		this.acknowledgement = acknowledgement;
		this.head = head;
		this.tail = tail;
		if (packet == null) this.data = new byte[0];
		else this.data = packet.getBytes();
	}
	
	public RDPDatagram(int acknowledgement, int fragDistance, TTPPacket packet) {
		this.sequence = getNextSequenceNumber();
		this.acknowledgement = acknowledgement;
		this.head = this.sequence;
		this.tail = this.head + fragDistance;
		this.data = packet.getBytes();
	}
	
	public int getSequence() {
		return sequence;
	}

	public int getAcknowledgement() {
		return acknowledgement;
	}

	public int getHead() {
		return head;
	}

	public int getTail() {
		return tail;
	}

	public byte[] getData() {
		return data;
	}
	
	public byte[] getBytes() {
		return Utility.mergeBytes(new byte[][] {
			Utility.intToByte(this.sequence),
			Utility.intToByte(this.acknowledgement),
			Utility.intToByte(this.head),
			Utility.intToByte(this.tail),
			this.data});
	}
	
	public TTPPacket getTTPPacket() {
		return new TTPPacket(this.data);
	}
	
	private static int getNextSequenceNumber() {
		if (currentSequenceNumber == Integer.MAX_VALUE) {
			currentSequenceNumber = 1;
			return Integer.MAX_VALUE;
		}
		return currentSequenceNumber++;
	}
	
	public static void resetSequenceIfLarger(int size) {
		if (currentSequenceNumber + size > Integer.MAX_VALUE) currentSequenceNumber = 1;
	}

}
