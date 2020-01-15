package ara.util;

public class ElectionDynamicMessage extends Message{
	
	long source_election;
	long ieme_election;
	
	public ElectionDynamicMessage(long idsrc, long iddest, long source_election, long ieme_election, int pid) {
		super(idsrc, iddest, pid);
	}

	public long getSource_election() {
		return source_election;
	}
	public long getIeme_election() {
		return ieme_election;
	}
}
