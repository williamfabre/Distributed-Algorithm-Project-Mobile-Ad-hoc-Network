package ara.manet.positioning.strategies;

import ara.manet.communication.Emitter;
import ara.manet.positioning.InitialPositionStrategy;
import ara.manet.positioning.Position;
import ara.manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public class InitialPositionConnectedRing implements InitialPositionStrategy {
	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_EMITTERPID = "emitter";

	private final int position_pid;
	private final int emitter_pid;

	public InitialPositionConnectedRing(String prefix) {
		position_pid = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
		emitter_pid = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
	}

	@Override
	public Position getInitialPosition(Node host) {
		int scope = ((Emitter) host.getProtocol(emitter_pid)).getScope();
		double maxX = ((PositionProtocol) host.getProtocol(position_pid)).getMaxX();
		double maxY = ((PositionProtocol) host.getProtocol(position_pid)).getMaxY();

		Position centre = new Position(maxX / 2.0, maxY / 2.0);
		int rayon = Math.min(Network.size() * (scope / 8), (int) (maxY / 2.5));
		if (host.getID() % 2 == 0) {
			rayon -= scope / 2;
		}
		double delta_angle = 2 * Math.PI / (double) Network.size();
		return centre.getNewPositionWith(rayon, delta_angle * host.getID());
	}

}
