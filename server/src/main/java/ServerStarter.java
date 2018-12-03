import java.net.SocketException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rdp.DataSocket;
import ttp.Server;

public class ServerStarter {

	public static void main(String[] args) {
		ExecutorService es = Executors.newCachedThreadPool();
		try {
			Server s = new Server(es);
			DataSocket ds = new DataSocket(s);
			s.setSocket(ds);
			es.execute(ds);
			synchronized (es) {
				es.wait();
			}
		} catch (SQLException | SocketException | InterruptedException e) {
			e.printStackTrace();
		}
	}

}
