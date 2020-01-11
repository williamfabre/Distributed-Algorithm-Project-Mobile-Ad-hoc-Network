package ara.manet.detection;

import java.util.ArrayList;
import java.util.List;

import ara.manet.algorithm.election.GVLElection;
import ara.manet.communication.EmitterProtocolImpl;
import ara.util.ProbeMessage;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class NeighborProtocolImpl implements NeighborProtocol, EDProtocol {

	public static final String heart_event = "HEARTEVENT";
	public static final String timer_event = "TIMEREVENT";

	private static final String PAR_PERIODE = "periode";
	private static final String PAR_TIMER = "timer";
	private static final String PAR_LISTENER = "listener";

	private final int my_pid;

	private final int periode;
	private final long timer;
	private final boolean listener;

	private List<Long> neighbors;
	private List<Integer> values; // for leader protocol

	public NeighborProtocolImpl(String prefix) {

		String tmp[] = prefix.split("\\.");
		my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		
		this.periode = Configuration.getInt(prefix + "." + PAR_PERIODE);
		this.timer = Configuration.getInt(prefix + "." + PAR_TIMER);
		this.listener = Configuration.getBoolean(prefix + "." + PAR_LISTENER);
		
		neighbors = new ArrayList<Long>();
		values = new ArrayList<Integer>(); 

	}

	public int getNeighborValue(long id_new_neighbor) {
		
		return values.get(neighbors.indexOf(id_new_neighbor));
	}

	@Override
	public List<Long> getNeighbors() {
		return this.neighbors;
	}

	public Object clone() {
		
		NeighborProtocolImpl np = null;

		try {
			np = (NeighborProtocolImpl) super.clone();
			np.neighbors = new ArrayList<Long>();
			np.values = new ArrayList<Integer>(); // for leader protocol
		
		} catch (CloneNotSupportedException e) {
			System.err.println(e.toString());
		}

		return np;
	}

	public void recvProbMsg(Node host, ProbeMessage msg) {
		
		if (!neighbors.contains(msg.getIdSrc())) {
		
			long idNeighbor = msg.getIdSrc();
			neighbors.add(idNeighbor);

			EDSimulator.add(timer, timer_event, host, my_pid);

			if (listener) {
				// notify for 
				values.add(msg.getValue()); // value for LP
				/*
				int election_pid = Configuration.lookupPid("election");
				GVLElection gvlEl = (GVLElection) host.getProtocol((election_pid));
				gvlEl.newNeighborDetected(host, idNeighbor);
				*/
				
				int listener_pid = Configuration.lookupPid("election");
				NeighborhoodListener nl = (NeighborhoodListener) host.getProtocol(listener_pid);
				nl.newNeighborDetected(host, idNeighbor);
				
			}
		}
	}

	public void delNeighbor(Node host) {

		long idNeighbor = neighbors.get(0);
		
		if (listener) {
			
			// notify for
			/*
			int election_pid = Configuration.lookupPid("election");
			GVLElection gvlEl = (GVLElection) host.getProtocol((election_pid));
			gvlEl.newNeighborDetected(host, idNeighbor);
			*/
			
			int listener_pid = Configuration.lookupPid("election");
			NeighborhoodListener nl = (NeighborhoodListener) host.getProtocol(listener_pid);
			nl.newNeighborDetected(host, idNeighbor);
			
			values.remove(0);// value for LP
		}
		
		neighbors.remove(0);
	}

	public void heartbeat(Node host) {

		// get value for Leader protocol
		int election_pid = Configuration.lookupPid("election");
		GVLElection gvlEl = (GVLElection) host.getProtocol((election_pid));
		int value = gvlEl.getValue();

		// local broadcast
		ProbeMessage probmsg = new ProbeMessage(host.getID(), ALL, my_pid, value); // value

		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		emp.emit(host, probmsg);

		EDSimulator.add(periode, heart_event, host, my_pid);
	}

	public void processEvent(Node host, int pid, Object event) {
		
		if (pid != my_pid) {
			throw new RuntimeException("Receive Event for wrong protocol");
		}

		if (event instanceof ProbeMessage) {
			recvProbMsg(host, (ProbeMessage) event);
			return;
		}

		if (event instanceof String) {
			String ev = (String) event;
			if (ev.equals(heart_event)) {
				heartbeat(host);
				return;
			}
			if (ev.equals(timer_event)) {
				delNeighbor(host);
				return;
			}
		}
		throw new RuntimeException("Receive unknown Event");
	}

}
