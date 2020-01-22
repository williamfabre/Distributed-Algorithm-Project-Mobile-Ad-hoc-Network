package ara.util;

import java.util.Iterator;
import java.util.Vector;

public class View implements Cloneable {

	private int clock;
	private Vector<Peer> neighbors;

	public View(/*Peer neighbor, int clock*/) {

		this.neighbors = new Vector<Peer>();
		this.clock = 0;
		//updateViewAdd((Peer)neighbor.clone());
		//setClock(clock);
	};
	/*
	public View(Vector<Peer> neighbors, int clock) {

		this.neighbors = new Vector<Peer>();
		//updateViewAddMult(neighbors);
		//setClock(clock);
	};
	*/
	public Object clone() {
		View v = null;
		try {
			v = (View) super.clone();
			/*
			 * for (Peer p : this.neighbors) { Peer tmp = (Peer)p.clone();
			 * v.neighbors.add(tmp); }
			 */
			v.clock = this.clock;
			v.neighbors = new Vector<Peer>();/*(Vector<Peer>)neighbors.clone();*/
			Iterator<Peer> iterator = this.neighbors.iterator();
			while (iterator.hasNext()) {
				v.neighbors.add((Peer) iterator.next().clone());
			}
			
		} catch (CloneNotSupportedException e) {
		}
		return v;
	}

	public String toString() {

		String s = new String();
		s = " T = [" + clock + "], { ";
		for (Peer p : neighbors) {
			s = s + p.toString();
		}
		s = s + "}";
		return s;
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

	public void updateViewAdd(Peer neighbor) { // copy ?

		for (Peer n : this.neighbors) {

			if (n.getId() == neighbor.getId()) {
				return;
			}
		}
		this.neighbors.add((Peer) neighbor.clone());
	}

	public void updateViewAddMult(Vector<Peer> neighbors_to_add) { // copy ?

		for (Peer n : neighbors_to_add) {
			boolean toadd = true;
			
			for (Peer n1 : this.neighbors) {

				if (n.getId() == n1.getId()) {
					toadd = false;
					break;
				}
			}
			if (toadd)
				this.neighbors.add((Peer) n.clone());
		}
	}

	// Delete a peer from a current view
	public void updateViewRemove(Peer neighbor) {

		for (Peer n : this.neighbors) {
			if (n != null) { // null ?
				if ((n.getId() == neighbor.getId())) {
					this.neighbors.remove(n);
					return;
				}
			}
		}
	}
}
