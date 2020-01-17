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

		// this.knowledge.add(my_pos, v);
		// position.add(host.getID());

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

	public void updateMyViewAdd(Peer p) {
		knowledge.get(0).updateViewAdd(p);
		knowledge.get(0).setClock(knowledge.get(0).getClock() + 1);
		// System.out.println(p.getId() + " updateMyView " +
		// knowledge.get(0).getNeighbors().size());
	}

	public void updateMyViewRemove(Peer p) {
		knowledge.get(0).updateViewRemove(p);
		knowledge.get(0).setClock(knowledge.get(0).getClock() + 1);
	}

	public void updateOneView(Long peerID, Vector<Peer> neighbors, int clock) {

		boolean found = false;
		for (Long p : position) {
			if (p == peerID) {
				found = true;
				break;
			}
		}

		if (!found) {
			position.add(peerID);
		}

		int pos = position.indexOf(peerID);

		if (knowledge.size() <= pos) {
			knowledge.setSize(pos + 1);
			// System.out.println(peerID + " resize : " + knowledge.size() + " pos : " +
			// pos);
		}

		if (knowledge.elementAt(pos) == null || knowledge.elementAt(pos).getNeighbors() == null) {
			// System.out.println(peerID + " to add " + neighbors.size());
			View v = new View(neighbors, clock); // clock ??
			knowledge.set(pos, v);

			// System.out.println(peerID + " added " +
			// knowledge.elementAt(pos).getNeighbors());
		} else {
			// System.out.println("voila : " +
			// knowledge.elementAt(pos).getNeighbors().size());
			for (Peer p : neighbors) {
				// knowledge.elementAt(pos).updateViewAddMult(neighbors);
				if (p != null) {
					View v = new View(neighbors, clock); // clock ??
					knowledge.set(pos, v);
				}
			}
		}
		// System.out.println(peerID + " updateOneView Res " +
		// knowledge.elementAt(pos).getNeighbors());
		// View v = new View(neighbors);
		// setView(pos, v);
	}

	public void updateOneViewRem(Long peerID, Vector<Peer> neighbors) {

		boolean found = false;
		for (Long p : position) {
			if (p == peerID) {
				found = true;
				break;
			}
		}

		if (found) {
			int pos = position.indexOf(peerID);
			for (Peer n : neighbors) {
				if (n != null) {
					// System.out.println("remove " + n.getId());
					knowledge.elementAt(pos).updateViewRemove(n);
				}
			}

			/*
			 * if (knowledge.elementAt(pos).getNeighbors().isEmpty()) {
			 * position.remove(peerID); knowledge.remove(pos); System.out.println("remove "
			 * + peerID); }
			 */
		}
	}

	public void updateOneClock(Long peerID, int clock) {
		int pos = position.indexOf(peerID);
		knowledge.get(pos).setClock(clock);
	}

}
