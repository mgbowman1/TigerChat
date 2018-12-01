package ttp;

public enum FlagType {
	MSG(0),
	FIL(1),
	INF(2),
	RQM(3),
	FRG(4),
	CON(5),
	CLS(6),
	CCV(7);
	
	private final int value;
	private FlagType(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return this.value;
	}
}
