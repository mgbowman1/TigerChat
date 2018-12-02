package ttp;

import java.sql.SQLException;

public class SocketConnection extends Processor {
	
	int port;

	public SocketConnection(int port) throws SQLException {
		super();
		this.port = port;
	}

	@Override
	public int reconnect() {
		return 0;
	}

	@Override
	public void receive(TTPPacket packet) {
		
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public void close() {
		
	}

}
