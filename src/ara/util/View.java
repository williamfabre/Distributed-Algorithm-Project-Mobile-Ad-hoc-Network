package ara.util;

import java.util.Vector;

public class View {

	private int clock;
	private Vector<Peer> neighbors = new Vector<Peer>();

	public View(Peer neighbor, int clock) {
		updateViewAdd(neighbor, clock);
	};

	public View(Vector<Peer> neighbors) {
		updateViewAddMult(neighbors);
	};

	public int getClock() {
		return clock;
	}
	
	public void setClock(int clock) {
		this.clock = clock;
	}

	public Vector<Peer> getNeighbors() {
		return neighbors;
	}

	public void updateViewAdd(Peer neighbor, int clock) {

		this.clock = clock;
		if (!this.neighbors.contains(neighbor)) {

			for (Peer n : neighbors) {
				if (n.getId() == neighbor.getId()) {
					n.setValue(neighbor.getValue());
					return;
				}
			}

			this.neighbors.add(neighbor);
		}
	}

	public void updateViewAddMult(Vector<Peer> neighbors_to_add) {

		for (Peer n : neighbors_to_add) {

			if (!this.neighbors.contains(n)) {
				this.neighbors.add(n);
			}
		}
	}

	public void updateViewRemove(Peer neighbor) {

		this.clock++;
		if (this.neighbors.contains(neighbor)) {
			this.neighbors.remove(neighbor);
		}
	}
}
