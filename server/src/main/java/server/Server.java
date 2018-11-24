package server;

import java.io.IOException;
import java.net.DatagramPacket;
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
				DatagramPacket packet = r.getNextMessage();
				byte[] arr = new byte[packet.getLength()];
				for (int i = 0; i < arr.length; i++) {
					arr[i] = packet.getData()[i];
				}
				String str = new String(arr);
				Sender s = new Sender(packet.getAddress(), packet.getPort() - 1);
				System.out.println("Received message: " + str + " from " + packet.getAddress().toString() + ":" + packet.getPort());
				s.send(str + " from the server.");
				s.destroy();
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

}
