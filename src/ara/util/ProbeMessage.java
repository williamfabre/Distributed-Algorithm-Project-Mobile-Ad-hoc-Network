package ara.util;

public class ProbeMessage extends Message{
	
	private int value;
	
	public ProbeMessage(long idsrc, long iddest, int pid, int value) {
		super(idsrc, iddest, pid);
		this.value = value;
	}
	
	public int getValue () {
		return this.value;
	}
	

}
