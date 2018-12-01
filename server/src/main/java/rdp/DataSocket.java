package rdp;

import java.util.LinkedList;
import java.util.Queue;

import ttp.Processor;

public class DataSocket extends Thread {
	
	// The list of pending datagrams waiting on ACKs
	private LinkedList<PendingDatagram> pendingSentDatagrams;
	
	// The list of datagrams still waiting to be sent
	private Queue<RDPDatagram> datagramSendList;
	
	// The current send window (number of datagrams to send in the current cycle)
	private int sendWindow;
	
	// The index of the current timeout datagram (the datagram being watched to timeout)
	private int timeoutDatagramIndex;
	
	// The current timeout time in milliseconds
	private int timeoutTime;
	
	// The maximum number of allowed pending datagrams
	private final int MAXPENDINGDATAGRAMS = 210;
	
	// The ssthresh to where the send windows stops growing as fast
	private final int SSTHRESH = MAXPENDINGDATAGRAMS / 2;
	
	// Alpha and beta used in timeout calculations
	private final double ALPHA = .125;
	private final double BETA = .25;
	
	// The processor used for reading packets
	private final Processor reader;
	
	public DataSocket(Processor reader) {
		this.pendingSentDatagrams = new LinkedList<>();
		this.datagramSendList = new LinkedList<>();
		this.sendWindow = 1;
		this.timeoutTime = 1000;
		this.reader = reader;
	}
	
	// Pending datagrams to keep track of duplicate ACKs and timestamps
	private class PendingDatagram {
		private RDPDatagram datagram;
		private int dupAck;
		private int timeSent;
		
		public PendingDatagram(RDPDatagram datagram, int dupAck, int timeSent) {
			this.datagram = datagram;
			this.dupAck = dupAck;
			this.timeSent = timeSent;
		}
	}

}
