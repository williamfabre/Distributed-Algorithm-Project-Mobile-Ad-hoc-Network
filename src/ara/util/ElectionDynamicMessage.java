package ara.util;

public class ElectionDynamicMessage extends Message{
	
	private long most_valued_node;
	private long most_valued_node_desirability;
	long source_election;
	long ieme_election;
	
	public ElectionDynamicMessage(long idsrc, long iddest, long most_valued_node, long most_valued_node_desirability, long source_election, long ieme_election, int pid) {
		super(idsrc, iddest, pid);
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
