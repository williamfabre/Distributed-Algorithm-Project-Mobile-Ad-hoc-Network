package ara.util;

import java.util.Vector;

public class Peer implements Cloneable{

	private long id;
	private int value;

	public Peer(long id_new_neighbor, int value) {
		this.id = id_new_neighbor;
		this.value = value;
	};
	
	public Peer() {};
	
	public Object clone() {
		Peer p = null;
		try {
			p = (Peer) super.clone();
			p.id = id;
			p.value = value;
		} catch (CloneNotSupportedException e) {
		}
		return p;
	}
	
	public String toString() {
		return id + " ";
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
