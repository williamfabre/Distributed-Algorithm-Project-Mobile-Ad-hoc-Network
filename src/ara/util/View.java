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
		// System.out.println("New view to add : " + neighbors.size());
		updateViewAddMult(neighbors);
		setClock(clock);
		// System.out.println("Added: " + this.neighbors.size());

	};

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

	public void updateViewAdd(Peer neighbor) {

		for (Peer n : neighbors) {
			if (n.getId() == neighbor.getId()) {
				return;
			}
		}
		//System.out.println(neighbor.getId() + " added");
		this.neighbors.add(neighbor);
	}

	public void updateViewAddMult(Vector<Peer> neighbors_to_add) {
		//System.out.println("empty : " + neighbors_to_add.size());
		for (Peer n : neighbors_to_add) {
			// if (this.neighbors.isEmpty())

			for (Peer n1 : this.neighbors) {

				if (n.getId() == n1.getId()) {
					break;
				}
			}
			this.neighbors.add(n);
		}
	}

	public void updateViewRemove(Peer neighbor) {
		// System.out.println(this.neighbors.isEmpty() + " removed");
		for (Peer n : this.neighbors) {
			if (n != null) {
				if ((n.getId() == neighbor.getId())) {
					this.neighbors.remove(n);
					 //System.out.println(neighbor.getId() + " removed");
					return;
				}
			}
		}
	}
}
