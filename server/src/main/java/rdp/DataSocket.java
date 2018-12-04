package rdp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import ttp.FlagType;
import ttp.Processor;
import ttp.Server;
import ttp.TTPPacket;
import util.Utility;

public class DataSocket extends Thread {
	
	// The list of pending datagrams waiting on ACKs
	private LinkedList<PendingDatagram> pendingSentDatagrams;
	
	// The list of datagrams still waiting to be sent
	private Queue<RDPDatagram> datagramSendList;
	
	// The list of sequence numbers which still need to be ACKed
	private Queue<Integer> receivedSequences;
	
	// The list of fragmented packets, yet to be put together
	private HashMap<Integer, FragmentedDatagram> fragmentedData;
	
	// The current send window (number of datagrams to send in the current cycle)
	private int sendWindow;
	
	// The index of the current timeout datagram (the datagram being watched to timeout)
	private int timeoutDatagramIndex;
	
	// The current timeout time in milliseconds
	private double timeoutTime;
	
	// The number of timeouts that have occurred for a send window
	private int numTimeouts;
	
	// The maximum number of allowed pending datagrams
	private final int MAXPENDINGDATAGRAMS = 210;
	
	// The maximum size of a datagram's data
	private final int MAXDATAGRAMDATASIZE = 504 - 16;
	
	// The ssthresh to where the send windows stops growing as fast
	private int ssthresh = MAXPENDINGDATAGRAMS / 2;
	
	// Alpha and beta used in timeout calculations
	private final double ALPHA = .125;
	private final double BETA = .25;
	
	// Estimate and dev used for timeout calculation
	private double estimateRTT = 0;
	private double devRTT = 0;
	
	// The index of the datagram to watch for recalculating current timeout
	private int timeoutWatchIndex;
	
	// The processor used for reading packets
	private final Processor reader;
	
	// The UDP socket for sending and receiving datagrams
	private DatagramSocket socket;
	
	// The current address and port to send to
	private InetAddress sendAddress;
	private int sendPort;
	
	// Whether the thread shoould keep running
	private boolean running = true;
	
	public void setConnection(InetAddress address, int port) {
		this.sendAddress = address;
		this.sendPort = port;
	}
	
	public InetAddress getAddress() {
		return this.sendAddress;
	}
	
	public int getPort() {
		return this.sendPort;
	}
	
	public DataSocket(Processor reader) throws SocketException {
		this.pendingSentDatagrams = new LinkedList<>();
		this.datagramSendList = new LinkedList<>();
		this.receivedSequences = new LinkedList<>();
		this.fragmentedData = new HashMap<>();
		this.sendWindow = 1;
		this.timeoutTime = 1000;
		this.reader = reader;
		this.socket = new DatagramSocket(this.reader.getPort());
		this.socket.setSoTimeout(100);
	}
	
	// The main protocol method
	@Override
	public void run() {
		while (this.running) {
			if (this.pendingSentDatagrams.isEmpty()) {
				try {
					sendProtocol();
					if (this.sendWindow < this.ssthresh) this.sendWindow *= 2;
					else this.sendWindow += 1;
					if (this.sendWindow > this.MAXPENDINGDATAGRAMS) this.sendWindow = this.MAXPENDINGDATAGRAMS;
				} catch (IOException e) {
					timeout();
				}
			} else if (new Date().getTime() - this.pendingSentDatagrams.get(this.timeoutDatagramIndex).timeSent >= this.timeoutTime) timeout();
			while (this.running) {
				try {
					RDPDatagram rdp = receive();
					if (rdp == null) break;
					else {
						if (rdp.getAcknowledgement() > 0) processAck(rdp.getAcknowledgement(), rdp.getSequence());
						if (rdp.getLength() > 0) {
							TTPPacket ttp = rdp.getTTPPacket();
							if (ttp.getFlag() == FlagType.CON) this.receivedSequences.add(-rdp.getSequence());
							else this.receivedSequences.add(rdp.getSequence());
							if (rdp.getHead() > 0) deFragmentPacket(rdp);
							else {
								this.reader.receive(ttp);
								if (this.reader instanceof Server) break;
							}
						}
					}
				} catch (IOException e) {
					timeout();
				}
			}
		}
	}
	
	// Put together fragmented packets
	private void deFragmentPacket(RDPDatagram rdp) {
		if (this.fragmentedData.containsKey(rdp.getHead())) {
			this.fragmentedData.get(rdp.getHead()).addDatagram(rdp);
			if (this.fragmentedData.get(rdp.getHead()).numDatagramsNeeded == 0) this.reader.receive(this.fragmentedData.get(rdp.getHead()).getTTPPacket());
		} else {
			this.fragmentedData.put(rdp.getHead(), new FragmentedDatagram(rdp.getHead(), rdp.getTail()));
			this.fragmentedData.get(rdp.getHead()).addDatagram(rdp);
		}
	}
	
	// When a timeout occurs
	private void timeout() {
		this.sendWindow = 1;
		this.ssthresh /= 2;
		this.numTimeouts++;
		if (this.numTimeouts == 3) {
			int check = this.reader.reconnect();
			if (check == 1) close();
			else if (check == -1) reset();
		}
	}
	
	// Proceses an ACK for the send list
	private void processAck(int ackNumber, int seqNumber) {
		Iterator<PendingDatagram> i = this.pendingSentDatagrams.iterator();
		int index = 0;
		while (i.hasNext()) {
			PendingDatagram pd = i.next();
			if (pd.datagram.getSequence() == ackNumber) {
				i.remove();
				if (pd.datagram.getTTPPacket().getFlag() == FlagType.CON) this.receivedSequences.add(seqNumber);
				if (index == this.timeoutDatagramIndex) this.timeoutDatagramIndex++;
				if (this.timeoutDatagramIndex >= this.pendingSentDatagrams.size()) this.timeoutDatagramIndex = 0;
				if (index == this.timeoutWatchIndex) calculateTimeout(new Date().getTime() - pd.timeSent);
				index = 0;
				i = this.pendingSentDatagrams.iterator();
				while (i.hasNext()) {
					PendingDatagram p = i.next();
					if (p.datagram.getSequence() < ackNumber) {
						p.dupAck++;
						if (p.dupAck == 3) {
							resend(p);
							if (index == this.timeoutDatagramIndex) this.timeoutDatagramIndex++;
							if (this.timeoutDatagramIndex == this.pendingSentDatagrams.size()) this.timeoutDatagramIndex = 0;
							if (index == this.timeoutWatchIndex) this.timeoutWatchIndex++;
						}
						index++;
					} else break;
				}
				break;
			}
			index++;
		}
	}
	
	// Recalculate the current timeout time
	private void calculateTimeout(long sample) {
		this.estimateRTT = (1 - this.ALPHA) * this.estimateRTT + this.ALPHA * sample;
		this.devRTT = (1 - this.BETA) * this.devRTT + this.BETA * Math.abs(sample - this.estimateRTT);
		this.timeoutTime = this.estimateRTT + 4 * this.devRTT;
	}
	
	// Receive the next datagram or return null if there isn't one to receive
	private RDPDatagram receive() throws IOException {
		byte[] buffer = new byte[512];
		DatagramPacket d = new DatagramPacket(buffer, buffer.length);
		try {
			this.socket.receive(d);
			this.sendAddress = d.getAddress();
			this.sendPort = d.getPort();
			RDPDatagram r = new RDPDatagram(d.getData());
			System.out.println(this.reader.getPort());
			System.out.println(r);
			return r;
		} catch (SocketTimeoutException e) {
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	// Send the current send window
	private void sendProtocol() throws IOException {
		Date d = new Date();
		int numSent = 0;
		this.timeoutDatagramIndex = 0;
		this.timeoutWatchIndex = 0;
		while (!this.datagramSendList.isEmpty() && numSent < this.sendWindow) {
			RDPDatagram r = this.datagramSendList.poll();
			send(r);
			this.numTimeouts = 0;
			this.pendingSentDatagrams.add(new PendingDatagram(r, 0, d.getTime()));
			numSent++;
		}
		while (numSent < this.sendWindow && !this.receivedSequences.isEmpty()) {
			int seq = this.receivedSequences.poll();
			int realSeq = Math.abs(seq);
			RDPDatagram r = new RDPDatagram(realSeq, 0, 0, null);
			send(r);
			if (seq < 0) this.pendingSentDatagrams.add(new PendingDatagram(r, 0, d.getTime()));
			numSent++;
		}
	}
	
	// Send a datagram
	private void send(RDPDatagram rdp) throws IOException {
		byte[] bytes = rdp.getBytes();
		System.out.println("sending");
		System.out.println(rdp);
		System.out.println(this.sendPort + " " + this.sendAddress);
		this.socket.send(new DatagramPacket(bytes, bytes.length, this.sendAddress, this.sendPort));
	}
	
	// Queue up a datagram to be sent
	public void addSend(TTPPacket ttp) {
		byte[] bytes = ttp.getBytes();
		if (bytes.length > this.MAXDATAGRAMDATASIZE) {
			byte[][] data = Utility.splitBytesData(bytes, this.MAXDATAGRAMDATASIZE - 1);
			RDPDatagram.resetSequenceIfLarger(data.length);
			for (int i = 0; i < data.length; i++) {
				if (i > 0) data[i] = Utility.mergeBytes(new byte[] {bytes[0]}, data[i]);
				TTPPacket t = new TTPPacket(data[i]);
				Integer ack = this.receivedSequences.poll();
				if (ack == null) ack = 0;
				else ack = Math.abs(ack.intValue());
				RDPDatagram r = new RDPDatagram(ack, data.length, t);
				this.datagramSendList.add(r);
			}
		} else {
			Integer ack = this.receivedSequences.poll();
			if (ack == null) ack = 0;
			this.datagramSendList.add(new RDPDatagram(ack, 0, 0, ttp));
		}
	}
	
	// Resend a datagram
	private void resend(PendingDatagram pd) {
		pd.dupAck = 0;
		this.sendWindow /= 2;
		pd.timeSent = new Date().getTime();
		while (this.running) {
			try {
				send(pd.datagram);
				break;
			} catch (IOException e) {
				timeout();
				try {
					this.wait(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	// Reset all the sending and receiving
	public void reset() {
		this.datagramSendList.clear();
		this.fragmentedData.clear();
		this.numTimeouts = 0;
		this.pendingSentDatagrams.clear();
		this.receivedSequences.clear();
		this.ssthresh = 210;
		this.timeoutTime = 1000;
	}
	
	// Close the connection
	public void close() {
		this.socket.close();
		try {
			this.reader.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		this.running = false;
	}
	
	public Processor getReader() {
		return this.reader;
	}
	
	// Pending datagrams to keep track of duplicate ACKs and timestamps
	private class PendingDatagram {
		private RDPDatagram datagram;
		private int dupAck;
		private long timeSent;
		
		public PendingDatagram(RDPDatagram datagram, int dupAck, long timeSent) {
			this.datagram = datagram;
			this.dupAck = dupAck;
			this.timeSent = timeSent;
		}
	}
	
	// Framented packets to build them fully
	private class FragmentedDatagram {
		private int numDatagramsNeeded;
		private byte[] data;
		
		public FragmentedDatagram(int start, int end) {
			this.numDatagramsNeeded = end - start + 1;
			this.data = new byte[this.numDatagramsNeeded * MAXDATAGRAMDATASIZE];
		}
		
		public void addDatagram(RDPDatagram rdp) {
			this.numDatagramsNeeded--;
			byte[] bytes = rdp.getData();
			if (rdp.getSequence() == rdp.getTail() && bytes.length < MAXDATAGRAMDATASIZE) {
				byte[] temp = new byte[this.data.length - (MAXDATAGRAMDATASIZE - bytes.length)];
				for (int i = 0; i < temp.length; i++) {
					temp[i] = this.data[i];
				}
				this.data = temp;
			}
			for (int i = 0; i < bytes.length; i++) {
				this.data[i + (rdp.getSequence() - rdp.getHead()) * MAXDATAGRAMDATASIZE] = bytes[i];
			}
		}
		
		public TTPPacket getTTPPacket() {
			
			return new TTPPacket(this.data);
		}
	}

}
