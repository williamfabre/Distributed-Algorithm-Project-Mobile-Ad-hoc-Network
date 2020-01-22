package ara.util;

import java.util.Iterator;
import java.util.Vector;

import ara.manet.algorithm.election.GVLElection;
import peersim.core.Node;

public class Knowledge implements Cloneable {

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

			k.knowledge = new Vector<View>();// (Vector<View>) knowledge.clone();
			k.position = new Vector<Long>();// (Vector<Long>) position.clone();
			Iterator<View> iterator = this.knowledge.iterator();
			while (iterator.hasNext()) {
				k.knowledge.add((View) iterator.next().clone());
			}
			for (Long p : this.position) {
				Long tmp = p;
				k.position.add(tmp);
			}

		} catch (CloneNotSupportedException e) {
		}
		return k;
	}

	public String toString() {
		String s = new String();
		int i = 0;
		for (View v : knowledge) {
			s = s + "\n pos[" + i + "] = " + position.elementAt(i) + ",";
			s = s + v.toString();
			i++;
		}
		return s;
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
		knowledge.set(pos, (View) v.clone());
	}

	public int getLastClock(int pos) {

		return knowledge.get(pos).getClock();
	}

	// add peer in host's list neighbors
	public void updateMyViewAdd(Peer p) {

		knowledge.get(0).updateViewAdd((Peer) p.clone());
		knowledge.get(0).setClock(knowledge.get(0).getClock() + 1);
	}

	// remove peer from host's list neighbors
	public void updateMyViewRemove(Peer p) {

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
		if (knowledge.elementAt(pos) == null || knowledge.elementAt(pos).getNeighbors().size() == 0) {
			View v = null;
			// create and insert new view
			if (knowledge.elementAt(pos) == null) {
				v = new View();

				// v.setClock(clock);
			} else {
				v = knowledge.elementAt(pos);
				// v.updateViewAddMult((Vector<Peer>) neighbors.clone());
			}
			v.updateViewAddMult(neighbors);
			knowledge.set(pos, v);

		} else { // view of src has some elts in the knowledge of host

			for (Peer p : neighbors) {

				if (p != null) {
					View v = knowledge.elementAt(pos);
					v.updateViewAddMult((Vector<Peer>) neighbors.clone());
					// v.setClock(clock);
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

					Vector<Peer> myneighbors = knowledge.elementAt(pos).getNeighbors();
					for (Peer p : myneighbors) {

						if (p.getId() == n.getId()) {
							knowledge.elementAt(pos).updateViewRemove(p);
							break;
						}
					}

				}
			}
		}
	}

	public void updateOneClock(Long source, int clock) {
		int pos = position.indexOf(source);
		knowledge.get(pos).setClock(clock);
		//if (source == 67)
			//System.out.println(source + " Clock changed : " + knowledge.get(pos).getClock());
	}

}
