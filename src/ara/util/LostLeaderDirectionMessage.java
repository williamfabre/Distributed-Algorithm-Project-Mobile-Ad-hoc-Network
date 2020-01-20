package ara.util;

public class LostLeaderDirectionMessage extends Message{

	private long most_valued_node;
	private long most_valued_node_desirability;
	private long source_election;
	private long ieme_election;
	private long parent;
	private long leader_direction;
	
	public LostLeaderDirectionMessage(
			long idsrc, 
			long iddest, 
			int pid,
			long most_valued_node,
			long most_valued_node_desirability,
			long source_election,
			long ieme_election,
			long parent,
			long leader_direction
			){
		
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
	
	public long getParent() {
		return parent;
	}
	
	public long getLeader_direction() {
		return leader_direction;
	}

}
