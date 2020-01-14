package ara.util;

public class Reply extends Message{

	private int value;
	
	public Reply(long idsrc, long iddest, int pid) {
		super(idsrc, iddest, pid);
		this.value = value;
	}

	public int getValue () {
		return this.value;
	}
	
}
