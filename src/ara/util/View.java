package ara.util;

import java.util.Vector;

public class View {

	private int clock;
	private Vector<Peer> neighbors;

	public View(Peer neighbor, int clock) {
		neighbors = new Vector<Peer>();
		updateViewAdd(neighbor);
		setClock(clock);
	};

	public View(Vector<Peer> neighbors, int clock) {
		this.neighbors = new Vector<Peer>();
		updateViewAddMult(neighbors);
		setClock(clock);
	};
	
	public Object clone() {
		View v = null;
		try {
			v = (View) super.clone();
			neighbors = new Vector<Peer>();
			
		} catch (CloneNotSupportedException e) {
		}
		return v;
	}
	
	public void print() {
		System.out.println();
		System.out.print(" [" + clock + "] ");
		for (Peer p : neighbors) {
			p.print();
		}
		System.out.println();
	}

	public int getClock() {
		return clock;
	}

	public void setClock(int clock) {
		this.clock = clock;
	}

	public Vector<Peer> getNeighbors() {
		return neighbors;
	}

	public void updateViewAdd(Peer neighbor) {	//copy ?

		for (Peer n : this.neighbors) {
			
			if (n.getId() == neighbor.getId()) {
				return;
			}
		}
		this.neighbors.add(neighbor);
	}

	private void updateViewAddMult(Vector<Peer> neighbors_to_add) {	//copy ?
		
		for (Peer n : neighbors_to_add) {
			
			for (Peer n1 : this.neighbors) {

				if (n.getId() == n1.getId()) {
					break;
				}
			}
			this.neighbors.add(n);
		}
	}

	//Delete a peer from a current view
	public void updateViewRemove(Peer neighbor) {
		for (Peer n : this.neighbors) {
			if (n != null) {	//null ?
				if ((n.getId() == neighbor.getId())) {
					this.neighbors.remove(n);
					return;
				}
			}
		}
	}
}
