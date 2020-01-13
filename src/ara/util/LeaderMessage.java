package ara.util;

public class LeaderMessage extends Message{
	
	private int value;
	
	public LeaderMessage(long idsrc, long iddest, int pid) {
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
