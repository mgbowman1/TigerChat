package server;

public enum PacketError {
	NOERROR(0),
	BADLOGIN(1),
	MALFORMEDPACKET(2),
	DATABASEERROR(3),
	SOCKETERROR(4),
	MISSEDPACKET(5);
	
	private final int value;
	private PacketError(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
}
