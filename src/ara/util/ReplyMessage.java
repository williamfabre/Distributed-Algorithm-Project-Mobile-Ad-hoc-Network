package ara.util;

public class ReplyMessage extends Message{
	
	private int value;
	
	public ReplyMessage(long idsrc, long iddest, int pid, int value) {
		super(idsrc, iddest, pid);
		this.value = value;
	}
	
	public int getValue () {
		return this.value;
	}
	

}
