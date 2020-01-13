package ara.util;

public class AckMessage extends Message{

	private long most_valued_node;
	private long most_valued_node_desirability;
	
	public AckMessage(long idsrc, long iddest, int pid, long most_valued_node, long most_valued_node_desirability ) {
		super(idsrc, iddest, pid);

		this.most_valued_node = most_valued_node;
		this.most_valued_node_desirability = most_valued_node_desirability;
	}
	
	
	public long getMostValuedNode () {
		return this.most_valued_node;
	}
	
	public long getMostValuedNodeDesirability () {
		return this.most_valued_node_desirability;
	}
}
