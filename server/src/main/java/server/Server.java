package server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

	public static void main(String[] args) {
		try {
			System.out.println(InetAddress.getLocalHost().toString());
			ExecutorService es = Executors.newCachedThreadPool();
			Receiver r = new Receiver(es);
			es.execute(r);
			while (true) {
				synchronized (es) {
					es.wait();
				}
				Packet packet = r.getNextMessage();
				Sender s = new Sender(packet.getSender(), packet.getSenderPort() - 1);
				System.out.println("Received message: " + packet.getMessage() + " with error: " + packet.getError() + " from " + packet.getSender().toString() + ":" + packet.getSenderPort());
				s.send(packet.getMessage() + " from the server.");
				s.destroy();
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

}
