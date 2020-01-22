package ara.util;

import java.util.Vector;

public class EditMessage extends Message {
	
	private Vector<Long> autors;
	private Vector<Vector<Peer>> added;
	private Vector<Vector<Peer>> removed;
	private Vector<Integer> old_clock;
	private Vector<Integer> new_clock;

	public EditMessage(long idsrc, long iddest, int pid) {
		super(idsrc, iddest, pid);
		autors = new Vector<Long>();
		added = new Vector<Vector<Peer>>();
		removed = new Vector<Vector<Peer>>();
		old_clock = new Vector<Integer>();
		new_clock = new Vector<Integer>();
	}
	
	public String toString() {
		String s = new String();
		int i = 0;
	
		for (Long a : autors) {
			s = s + "(" + a + ")";
			
			if (!added.isEmpty())
				s = s + " Added = {"+ added.elementAt(i) + "}";
			if (!removed.isEmpty())
				s = s + " Removed = {" + removed.elementAt(i) + "}";
			
			s = s + " t(i) = " + old_clock.elementAt(i) + ", t(i+1) = " + new_clock.elementAt(i) + ";" + "\n";
		}
		return s;
	}
	
	public void setClock(int old_clock, int new_clock) {
		this.old_clock.add(old_clock);
		this.new_clock.add(new_clock);
	}
	
	public int getOldClock(int pos) {
		return old_clock.elementAt(pos);
	}
	
	public int getNewClock(int pos) {
		return new_clock.elementAt(pos);
	}
	
	public void setAutor(Long a) {
		autors.add(a);
	}
	
	public Vector<Long> getAutors() {
		return autors;
	}
	public void setAdded(Vector<Peer> j) {
		if (j == null) {
			added.add(new Vector<Peer>());
			return;
		}
			added.add(j);
	}
	
	public void setRemoved(Vector<Peer> j) {
		if (j == null) {
			added.add(new  Vector<Peer>());
			return;
		}
		removed.add(j);
	}
	public void setUpdates(Peer k, Peer j) {
		Vector vk = new Vector<Peer>();
		Vector vj = new Vector<Peer>();
		vk.add(k);
		vj.add(j);
		setAdded(vk);
		setRemoved(vj);
	}
	public void setUpdates(Vector<Peer> k, Vector<Peer> j) {
		setAdded(k);
		setRemoved(j);
	}
	
	public Vector<Peer> getAdded(int pos) {
		return added.elementAt(pos);
	}
	
	public  Vector<Peer> getRemoved(int pos) {
		return removed.elementAt(pos);
	}
	
	public boolean addedIsEmpty(int pos) {
			return added.size() <= pos || added.elementAt(pos).size() == 0;
	}
	
	public boolean removedIsEmpty(int pos) {
			return removed.size() <= pos || removed.elementAt(pos).size() == 0;
	}
	
	public boolean empty() {
		boolean empty = true;
		for (Vector<Peer> vp : added) {
			if (vp.size() != 0)
				return false;
		}
		for (Vector<Peer> vp : removed) {
			if (vp.size() != 0)
				return false;
		}
		return empty;
	}

}
