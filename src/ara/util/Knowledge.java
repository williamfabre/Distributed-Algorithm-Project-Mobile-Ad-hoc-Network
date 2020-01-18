package ara.util;

import java.util.Vector;

import ara.manet.algorithm.election.GVLElection;
import peersim.core.Node;

public class Knowledge {

	private Vector<View> knowledge;
	private Vector<Long> position;

	public Knowledge() {

		knowledge = new Vector<View>();
		position = new Vector<Long>();
	}

	public Object clone() {
		Knowledge k = null;
		try {
			k = (Knowledge) super.clone();
			k.knowledge = new Vector<View>();
			k.position = new Vector<Long>();

		} catch (CloneNotSupportedException e) {
		}
		return k;
	}

	public void print() {
		String s = new String();
		int i = 0;
		for (View v : knowledge) {
			System.out.println("pos = " + position.elementAt(i));
			v.print();
			i++;
		}
	}

	public Vector<View> getKnowledge() {
		return knowledge;
	}

	public Vector<Long> getPosition() {
		return position;
	}

	public void setPosition(Long p) {
		position.add(p);
	}

	public View getView(int pos) {

		return knowledge.elementAt(pos);
	}

	public void setView(int pos, View v) {

		if (knowledge.size() <= pos) {
			knowledge.setSize(pos + 1);
		}

		knowledge.set(pos, v);
	}

	public int getLastClock(int pos) {

		return knowledge.get(pos).getClock();
	}

	// add peer in host's list neighbors
	public void updateMyViewAdd(Peer p) {
		knowledge.get(0).updateViewAdd(p);
		knowledge.get(0).setClock(knowledge.get(0).getClock() + 1);
	}

	// remove peer from host's list neighbors
	public void updateMyViewRemove(Peer p) {
		
		//test if src is in knowledge of host
			/*	boolean found = false;
				for (Long peer : position) {
					if (peer == p.getId()) {
						found = true;
						break;
					}
				}

				if (found) {
					for (Peer n : knowledge.get(0).getNeighbors()) {
						if (p.getId() == n.getId())
							knowledge.get(0).updateViewRemove(p);
					}
				}
				*/	
		knowledge.get(0).updateViewRemove(p);
		knowledge.get(0).setClock(knowledge.get(0).getClock() + 1);
		
	}

	public void updateOneView(Long source, Vector<Peer> neighbors, int clock) {

		// test if src is in knowledge of host
		boolean found = false;
		for (Long p : position) {
			if (p == source) {
				found = true;
				break;
			}
		}

		if (!found) {
			position.add(source);
		}

		int pos = position.indexOf(source);

		if (knowledge.size() <= pos) {
			knowledge.setSize(pos + 1);
		}

		// view of src is vide in the knowledge of host
		if (knowledge.elementAt(pos) == null || knowledge.elementAt(pos).getNeighbors() == null) {

			// create and insert new view
			View v = new View(neighbors, clock);
			knowledge.set(pos, v);

		} else { // view of src has some elts in the knowledge of host

			for (Peer p : neighbors) {

				if (p != null) {
					View v = new View(neighbors, clock);
					knowledge.set(pos, v);
				}
			}
		}
	}

	public void updateOneViewRem(Long source, Vector<Peer> neighbors) {

		// test if src is in knowledge of host
		boolean found = false;
		for (Long p : position) {
			if (p == source) {
				found = true;
				break;
			}
		}

		if (found) {
			int pos = position.indexOf(source);

			for (Peer n : neighbors) {
				if (n != null) {
					// Delete a peer from a current view defined by pos
					knowledge.elementAt(pos).updateViewRemove(n);
					/*
					for (Peer p : knowledge.elementAt(pos).getNeighbors()) {
						if (p.getId() == n.getId())
							knowledge.elementAt(pos).updateViewRemove(p);
					}
					*/
				}
			}
		}
	}

	public void updateOneClock(Long source, int clock) {
		int pos = position.indexOf(source);
		knowledge.get(pos).setClock(clock);
	}

}
