package ara.util;

public class Peer {

	private long id;
	private int value;

	public Peer(long id_new_neighbor, int value) {
		this.id = id_new_neighbor;
		this.value = value;
	};
	
	public Peer() {};
	
	public void print() {
		System.out.print(" ("+id+") ");
	}
	
	public void setValue(int v) {
		this.value = v;
	}
	
	public void setId (long id) {
		this.id = id;
	}

	public int getValue() {
		return value;
	}
	
	public long getId() {
		return id;
	}
}
