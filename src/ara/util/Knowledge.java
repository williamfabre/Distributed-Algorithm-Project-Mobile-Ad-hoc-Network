package ara.util;

import java.util.Vector;

import peersim.core.Node;

public class Knowledge {

	private Vector<View> knowledge;
	private Vector<Long> position;

	public Knowledge(Node host, View v) {
		int my_pos = 0;
		knowledge = new Vector<View>();
		position = new Vector<Long>();
		
		this.knowledge.add(my_pos, v);
		position.add(host.getID());
		
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
		
		knowledge.add(pos, v);
	}

	public int getLastClock(int pos) {
		return knowledge.get(pos).getClock();
	}

	public void updateMyViewAdd(Peer p, int clock) {
		knowledge.get(0).updateViewAdd(p, clock);
	}

	public void updateMyViewRemove(Peer p) {
		knowledge.get(0).updateViewRemove(p);
	}

	public void updateOneView(Long peerID, Vector<Peer> neighbors) {

		if (!position.contains(peerID)) {
			position.add(peerID);
		}
		
		int pos = position.indexOf(peerID);
		View v = new View(neighbors);
		setView(pos, v);
	}
	
	public void updateOneViewRem(Long peerID, Vector<Peer> neighbors) {

		if (position.contains(peerID)) {
			int pos = position.indexOf(peerID);
			knowledge.remove(pos);
			position.remove(peerID);
		}
	}
	
	public void updateOneClock(Long peerID, int clock) {
		int pos = position.indexOf(peerID);
		knowledge.get(pos).setClock(clock);
	}

}
