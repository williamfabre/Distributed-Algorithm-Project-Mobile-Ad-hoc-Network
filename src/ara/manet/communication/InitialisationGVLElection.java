package ara.manet.communication;

import ara.manet.algorithm.election.GVLElection;
import ara.manet.detection.NeighborProtocolImpl;
import ara.manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class InitialisationGVLElection implements Control {

	public InitialisationGVLElection(String prefix) {
	}

	@Override
	public boolean execute() {

		int position_pid = Configuration.lookupPid("position");
		// int emitter_pid = Configuration.lookupPid("emit");
		int neighbor_pid = Configuration.lookupPid("neighbor");
		int elect_pid = Configuration.lookupPid("election");
		PositionProtocolImpl position;
		

		for (int i = 0; i < Network.size(); i++) {

			Node node = Network.get(i);
			
			position = (PositionProtocolImpl) node.getProtocol(position_pid);
			NeighborProtocolImpl np = (NeighborProtocolImpl) node.getProtocol(neighbor_pid);
			
			position.initialiseCurrentPosition(node);
			position.processEvent(node, position_pid, "LOOPEVENT");
			
			np.processEvent(node, neighbor_pid, "HEARTEVENT");
		}
			for (int i = 0; i < Network.size(); i++) {

				Node node = Network.get(i);
				GVLElection gvl = (GVLElection) node.getProtocol(elect_pid);
				gvl.initialisation(node);
		}
		return false;
	}

}