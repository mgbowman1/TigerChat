package rdp;

import ttp.TTPPacket;
import util.Utility;

public class RDPDatagram {
	
	// List of headers and data field
	private int sequence;
	private int acknowledgement;
	private int head;
	private int tail;
	private int length;
	private byte[] data;
	
	// Datagram sequence number tracker
	private static int currentSequenceNumber = 1;
	
	public RDPDatagram(byte[] bytes) {
		this.sequence = Utility.byteToInt(bytes, 0);
		this.acknowledgement = Utility.byteToInt(bytes, 4);
		this.head = Utility.byteToInt(bytes, 8);
		this.tail = Utility.byteToInt(bytes, 12);
		this.length = Utility.byteToInt(bytes, 16);
		this.data = Utility.splitBytes(bytes, 20, 20 + this.length);
	}
	
	public RDPDatagram(int acknowledgement, int head, int tail, TTPPacket packet) {
		this.sequence = getNextSequenceNumber();
		this.acknowledgement = acknowledgement;
		this.head = head;
		this.tail = tail;
		if (packet == null) this.data = new byte[0];
		else this.data = packet.getBytes();
		this.length = this.data.length;
	}
	
	public RDPDatagram(int acknowledgement, int fragDistance, TTPPacket packet) {
		this.sequence = getNextSequenceNumber();
		this.acknowledgement = acknowledgement;
		this.head = this.sequence;
		this.tail = this.head + fragDistance;
		this.data = packet.getBytes();
		this.length = this.data.length;
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
	
	public int getLength() {
		return this.length;
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
			Utility.intToByte(this.length),
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RDPDatagram [sequence=");
		builder.append(sequence);
		builder.append(", acknowledgement=");
		builder.append(acknowledgement);
		builder.append(", head=");
		builder.append(head);
		builder.append(", tail=");
		builder.append(tail);
		builder.append(", length=");
		builder.append(length);
		if (length > 0) {
			builder.append(", data=");
			builder.append(getTTPPacket().toString());
		}
		return builder.toString();
	}

}
