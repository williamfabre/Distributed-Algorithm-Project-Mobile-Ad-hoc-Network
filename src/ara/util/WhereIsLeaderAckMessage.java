package ara.util;

public class WhereIsLeaderAckMessage extends Message{
	private long idsrc;
	private long iddest;
	private int pid;
	private boolean leaderfound;
	private long lost_node;
	
	public WhereIsLeaderAckMessage(
			long idsrc,
			long iddest,
			int pid,
			boolean is_leaderfound, 
			long lost_node) {
		super(idsrc, iddest, pid);
		// TODO Auto-generated constructor stub
	}

	public long getLost_node() {
		return lost_node;
	}

	public boolean isLeaderfound() {
		return leaderfound;
	}

}
