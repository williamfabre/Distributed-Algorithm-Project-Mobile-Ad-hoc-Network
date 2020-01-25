package ara.util;

public class AckMessageNextGeneration extends Message{

	private long potential_leader;
	private long potential_leader_desirability;
	private long source_election;
	private long ieme_election;
	
	public AckMessageNextGeneration(long idsrc,long iddest,
			int pid,
			long potential_leader,
			long potential_leader_desirability,
			long source_election,
			long ieme_election) {
		super(idsrc, iddest, pid);

		this.potential_leader = potential_leader;
		this.potential_leader_desirability = potential_leader_desirability;
		this.source_election = source_election;
		this.ieme_election = ieme_election;
	}
	
	public long getMostValuedNode () {
		return this.potential_leader;
	}
	
	public long getMostValuedNodeDesirability () {
		return this.potential_leader_desirability;
	}

	public long getSource_election() {
		return source_election;
	}

	public long getIeme_election() {
		return ieme_election;
	}


}