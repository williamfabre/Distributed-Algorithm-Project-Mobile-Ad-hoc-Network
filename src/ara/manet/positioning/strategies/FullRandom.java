package ara.manet.positioning.strategies;

import java.util.HashMap;
import java.util.Map;

import ara.manet.positioning.InitialPositionStrategy;
import ara.manet.positioning.NextDestinationStrategy;
import ara.manet.positioning.Position;
import ara.manet.positioning.PositionProtocol;
import ara.manet.positioning.PositioningConfiguration;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.ExtendedRandom;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public class FullRandom implements InitialPositionStrategy, NextDestinationStrategy {

	private static final String PAR_POSITIONPID = "positionprotocol";
	private final int position_pid;
	private final ExtendedRandom my_random;
	private Map<Long, Position> initial_position = null;

	public FullRandom(String prefix) {
		position_pid = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
		this.my_random = PositioningConfiguration.getPositioningRandom();

	}

	@Override
	public Position getInitialPosition(Node host) {
		if (initial_position == null) {
			initial_position = new HashMap<Long, Position>();
			for (int i = 0; i < Network.size(); i++) {
				Node n = Network.get(i);
				initial_position.put(n.getID(), getNextDestination(n, 0));
			}
		}
		return initial_position.get(host.getID());
	}

	PositionProtocol getPositionProcotol(Node host) {
		return ((PositionProtocol) host.getProtocol(position_pid));
	}

	@Override
	public Position getNextDestination(Node host, int speed) {
		double maxX = getPositionProcotol(host).getMaxX();
		double maxY = getPositionProcotol(host).getMaxY();
		return new Position(my_random.nextDouble() * maxX, my_random.nextDouble() * maxY);
	}

}
