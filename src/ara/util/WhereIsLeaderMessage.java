package ara.util;

public class WhereIsLeaderMessage extends Message{

	private long idsrc;
	private long iddest;
	private int pid;
	private boolean is_leaderfound;
	private long lost_node;
	
	public WhereIsLeaderMessage(
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

	public boolean isIs_leaderfound() {
		return is_leaderfound;
	}

}
