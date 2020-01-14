package ara.util;

public class ElectionMessage extends Message{
	
	long source_election;
	
	public ElectionMessage(long idsrc, long iddest, long source_election, int pid) {
		super(idsrc, iddest, pid);
	}

	public long getSource_election() {
		return source_election;
	}
}
