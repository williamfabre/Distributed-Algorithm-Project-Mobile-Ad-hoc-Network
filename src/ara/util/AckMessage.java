package ara.util;

public class AckMessage extends Message{

	private long most_valued_node;
	private long most_valued_node_desirability;
	private long source_election;
	private long ieme_election;
	
	public AckMessage(long idsrc,
			long iddest,
			int pid,
			long most_valued_node,
			long most_valued_node_desirability,
			long source_election,
			long ieme_election) {
		super(idsrc, iddest, pid);

		this.most_valued_node = most_valued_node;
		this.most_valued_node_desirability = most_valued_node_desirability;
		this.source_election = source_election;
		this.ieme_election = ieme_election;
	}
	
	public long getMostValuedNode () {
		return this.most_valued_node;
	}
	
	public long getMostValuedNodeDesirability () {
		return this.most_valued_node_desirability;
	}

	public long getSource_election() {
		return source_election;
	}

	public long getIeme_election() {
		return ieme_election;
	}


}