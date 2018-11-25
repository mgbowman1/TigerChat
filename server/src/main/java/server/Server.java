package server;

import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

	public static void main(String[] args) {
		ExecutorService es = Executors.newCachedThreadPool();
		try {
			ServerConnection s = new ServerConnection(es);
			es.execute(s);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		synchronized(es) {
			try {
				es.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
