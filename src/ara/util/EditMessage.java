package ara.util;

import java.util.Vector;

public class EditMessage extends Message {
	
	private Vector<Long> autors = new Vector<Long>();
	private Vector<Vector<Peer>> added = new Vector<Vector<Peer>>();
	private Vector<Vector<Peer>> removed = new Vector<Vector<Peer>>();
	private Vector<Integer> old_clock = new Vector<Integer>();
	private Vector<Integer> new_clock = new Vector<Integer>();

	public EditMessage(long idsrc, long iddest, int pid) {
		super(idsrc, iddest, pid);
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
			added.add(new Vector());
			return;
		}
			added.add(j);
	}
	
	public void setRemoved(Vector<Peer> j) {
		if (j == null) {
			added.add(new Vector());
			return;
		}
		removed.add(j);
	}
	public void setUpdates(Peer k, Peer j) {
		Vector vk = new Vector();
		Vector vj = new Vector();
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
		
		if(added.size()<=pos)
			return false;
		
		return added.elementAt(pos).isEmpty();
	}
	
	public boolean removedIsEmpty(int pos) {
		
		if(removed.size()<=pos)
			return false;
		
		return removed.elementAt(pos).isEmpty();
	}
	
	public boolean empty() {
		return (added.isEmpty() && removed.isEmpty());
	}

}
