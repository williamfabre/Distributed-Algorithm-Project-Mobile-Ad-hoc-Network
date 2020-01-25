package ara.util;

public class ElectionMessageNextGeneration extends Message{
	
	private long source_election;
	private long ieme_election;
	
	public ElectionMessageNextGeneration(long idsrc,
			long iddest,
			int pid,
			long source_election,
			long ieme_election
			) {
		super(idsrc, iddest, pid);
		this.source_election = source_election;
		this.ieme_election = ieme_election;
	}
	
	public long getSource_election() {
		return source_election;
	}
	
	public long getIeme_election() {
		return ieme_election;
	}
}