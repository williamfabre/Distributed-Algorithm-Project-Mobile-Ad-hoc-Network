package ara.util;

public class ElectionMessage extends Message{
	
	private int value;
	
	public ElectionMessage(long idsrc, long iddest, int pid) {
		super(idsrc, iddest, pid);
		value = -1;
	}
	
	public int setValue (int v) {
		this.value = v;
		return v;
	}
	
	public int getValue () {
		return this.value;
	}
	
}
