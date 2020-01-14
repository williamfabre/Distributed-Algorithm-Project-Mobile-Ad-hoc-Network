package ara.manet.algorithm.election;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ara.manet.Monitorable;
import ara.manet.communication.EmitterProtocolImpl;
import ara.manet.detection.NeighborProtocol;
import ara.manet.detection.NeighborProtocolImpl;
import ara.manet.detection.NeighborhoodListener;
import ara.util.EditMessage;
import ara.util.Knowledge;
import ara.util.KnowledgeMessage;
import ara.util.Peer;
import ara.util.View;
import ara.util.ProbeMessage;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.util.ExtendedRandom;

public class GVLElection implements Monitorable, ElectionProtocol, NeighborhoodListener {

	private static final long ALL = -2;

	private static final String PAR_PERIODE = "periode";
	public static final String leader_event = "LEADEREVENT";

	private final int my_pid;
	private int value;
	private final int periode;
	private Knowledge knowledge;

	public GVLElection(String prefix) {

		String tmp[] = prefix.split("\\.");
		my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.periode = Configuration.getInt(prefix + "." + PAR_PERIODE);
		knowledge = new Knowledge();
	}

	public Object clone() {
		GVLElection p = null;
		try {
			p = (GVLElection) super.clone();
			p.knowledge = new Knowledge();
		} catch (CloneNotSupportedException e) {
		}
		return p;
	}

	public void initialisation(Node node) {

		//ExtendedRandom my_random = new ExtendedRandom(10);
		//this.value = (int) (my_random.nextInt(1000) / (node.getID() + 1));
		this.value = (int) node.getID();

		long id = node.getID();
		Peer p = new Peer(id, value);
		int clock = 0;
		View v = new View(p, clock);

		this.knowledge.setView(0, v);
		this.knowledge.setPosition(node.getID());
		//System.out.println("Node " + node.getID() + " : "+ knowledge.getKnowledge().size());

	}

	@Override
	public void newNeighborDetected(Node host, long id_new_neighbor) {
		
		// get neighbors value
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolImpl np = (NeighborProtocolImpl) host.getProtocol(neighbor_pid);
		int neighbors_value = np.getNeighborValue(id_new_neighbor);

		Peer j = new Peer(id_new_neighbor, neighbors_value);

		int clock = this.knowledge.getLastClock(0) + 1;
		this.knowledge.updateMyViewAdd(j, clock);

		// Broadcast knowledge
		KnowledgeMessage knowlmsg = new KnowledgeMessage(host.getID(), ALL, my_pid, knowledge);
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		emp.emit(host, knowlmsg);
		
		//System.out.println("Node " + host.getID() + " : "+ knowledge.getKnowledge().size());
	}

	@Override
	public void lostNeighborDetected(Node host, long id_lost_neighbor) {

		// get neighbors value
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolImpl np = (NeighborProtocolImpl) host.getProtocol(neighbor_pid);
		int neighbors_value = np.getNeighborValue(id_lost_neighbor);

		// Create edit message
		Peer j = new Peer(id_lost_neighbor, neighbors_value);
		int clock = knowledge.getLastClock(0);
		EditMessage edmsg = new EditMessage(host.getID(), ALL, my_pid);
		edmsg.setAutor(host.getID());
		edmsg.setUpdates(null, j);
		edmsg.setClock(clock, clock + 1);
		this.knowledge.updateMyViewRemove(j);

		// Broadcast edit
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		emp.emit(host, edmsg);
		
		//System.out.println("Node " + host.getID() + " : "+ knowledge.getKnowledge().size());
	}

	@Override
	public long getIDLeader() {

		// max value in all reachable peer from i
		int max_val = 0;
		long idLeader = 0;
		
		View v = knowledge.getView(0);
		
		for (Peer p : v.getNeighbors()) {
		
			if (p.getValue() >= max_val) {
			
				max_val = p.getValue();
				idLeader = p.getId();
			}
		}

		return idLeader;
	}

	@Override
	public int getValue() {
		
		return this.value;
	}

	public EditMessage updateKnowledge(Node host, long j, Knowledge j_knowledge) {

		EditMessage edmsg = new EditMessage(host.getID(), ALL, my_pid);

		for (Long peerId : j_knowledge.getPosition()) {

			View j_view = j_knowledge.getView(j_knowledge.getPosition().indexOf(peerId));

			if (knowledge.getPosition().contains(peerId)) {// known neighbor
				int i_position = knowledge.getPosition().indexOf(peerId);
				// compare
				View i_view = knowledge.getView(knowledge.getPosition().indexOf(peerId));

				if (j_view.getClock() > i_view.getClock()) {

					// Create Edit Message
					edmsg.setAutor(peerId);
					Vector<Peer> added = new Vector();
					Vector<Peer> removed = new Vector();
					for (Peer p : j_view.getNeighbors()) {
						if (!i_view.getNeighbors().contains(p))
							added.add(p);
						else
							removed.add(p);
					}
					edmsg.setUpdates(added, removed);
					edmsg.setClock(i_view.getClock(), j_view.getClock());

					knowledge.setView(i_position, j_view);
				} // end if clock

			} else { // absolutely new neighbor

				// Create Edit message
				edmsg.setAutor(peerId);
				edmsg.setUpdates(j_view.getNeighbors(), null);
				edmsg.setClock(0, j_view.getClock());
				knowledge.setPosition(peerId);
				int i_position = knowledge.getPosition().indexOf(peerId);
				knowledge.setView(i_position, j_view);
			}
		}

		return edmsg;

	}

	private void recvKnowlMsg(Node host, KnowledgeMessage msg) {

		// Create edit message
		EditMessage edmsg = updateKnowledge(host, msg.getIdSrc(), msg.getKnowledge());

		// Broadcast edit
		if (!edmsg.empty()) {
			int emitter_pid = Configuration.lookupPid("emit");
			EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
			emp.emit(host, edmsg);
		}
	}

	private void recvEditMsg(Node host, EditMessage msg) {

		int p = 0;
		boolean kno_updated = false;
		for (Long source : msg.getAutors()) {
			boolean updated = false;
			if (!msg.addedIsEmpty(p)) {
				if (!knowledge.getPosition().contains(source)) {
					if (msg.getOldClock(p) == 0) {
						knowledge.updateOneView(source, msg.getAdded(p));
					}
				} else {// known source
					int pos = knowledge.getPosition().indexOf(source);
					if (msg.getOldClock(p) == knowledge.getLastClock(pos)) {
						knowledge.updateOneView(source, msg.getAdded(p));
						updated = true;
					}

				}
			} // end if

			if (!msg.removedIsEmpty(p)) {
				if (knowledge.getPosition().contains(source)) {
					int pos = knowledge.getPosition().indexOf(source);
					if (msg.getOldClock(p) == knowledge.getLastClock(pos)) {
						knowledge.updateOneViewRem(source, msg.getRemoved(p));
						updated = true;
					}

				}
			} // end if

			if (knowledge.getPosition().contains(source)) {
				if (updated) {
					knowledge.updateOneClock(source, msg.getNewClock(p));
					kno_updated = true;
				}
			}
			p++;
		}

		if (kno_updated) {
			// broadcast msg
			int emitter_pid = Configuration.lookupPid("emit");
			EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
			emp.emit(host, msg);
		}

	}

	@Override
	public void processEvent(Node host, int pid, Object event) {

		if (pid != my_pid) {
			throw new RuntimeException("Receive Event for wrong protocol");
		}

		if (event instanceof KnowledgeMessage) {
			recvKnowlMsg(host, (KnowledgeMessage) event);
			return;
		}

		if (event instanceof EditMessage) {
			recvEditMsg(host, (EditMessage) event);
			return;
		}

		if (event instanceof String) {
			String ev = (String) event;

			if (ev.equals(leader_event)) {
				//System.out.println(host.getIndex() + " : Leader " + getIDLeader());
				EDSimulator.add(periode, leader_event, host, my_pid);
				return;
			}
		}

	}
	@Override
	public int nbState() {
		return 2;
	}
	
	@Override
	public int getState(Node host) {
		if (getIDLeader() == host.getID())
			return 1;
		else
			return 0;
	}

	@Override
	public List<String> infos(Node host) {
		List<String> res = new ArrayList<String>();
		res.add("Node" + host.getID() + " Boss["+ getIDLeader() + "]" + "\n Val(" + getValue() + ")");
		return res;
	}
	
	
}
