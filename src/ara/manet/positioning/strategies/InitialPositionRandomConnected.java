package ara.manet.positioning.strategies;

import java.util.HashMap;
import java.util.Map;

import ara.manet.communication.Emitter;
import ara.manet.positioning.InitialPositionStrategy;
import ara.manet.positioning.Position;
import ara.manet.positioning.PositionProtocol;
import ara.manet.positioning.PositioningConfiguration;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.ExtendedRandom;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public class InitialPositionRandomConnected implements InitialPositionStrategy {
	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_EMITTERPID = "emitter";
	private static final String PAR_DISTANCEINITMIN = "distance_init_min";
	private static final String PAR_DISTANCEINITMAX = "distance_init_max";

	private final int position_pid;
	private final int emitter_pid;
	private final int distance_init_min;
	private final int distance_init_max;
	private final ExtendedRandom my_random;

	private Map<Long, Position> initial_position = null;

	public InitialPositionRandomConnected(String prefix) {
		position_pid = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
		emitter_pid = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
		distance_init_min = Configuration.getInt(prefix + "." + PAR_DISTANCEINITMIN, 0);
		distance_init_max = Configuration.getInt(prefix + "." + PAR_DISTANCEINITMAX, Integer.MAX_VALUE);
		this.my_random = PositioningConfiguration.getPositioningRandom();
	}

	@Override
	public Position getInitialPosition(Node host) {
		if (initial_position == null) {
			int scope = ((Emitter) host.getProtocol(emitter_pid)).getScope();
			double maxX = ((PositionProtocol) host.getProtocol(position_pid)).getMaxX();
			double maxY = ((PositionProtocol) host.getProtocol(position_pid)).getMaxY();
			initial_position = new HashMap<Long, Position>();
			for (int i = 0; i < Network.size(); i++) {
				Node n = Network.get(i);
				if (initial_position.size() == 0) {
					double min_distance_x = maxX / 3;
					double max_distance_x = maxX;
					double min_distance_y = maxY / 3;
					double max_distance_y = maxY;
					initial_position.put(n.getID(),
							new Position(my_random.nextDouble() * (max_distance_x - min_distance_x) + min_distance_x,
									my_random.nextDouble() * (max_distance_y - min_distance_y) + min_distance_y));
				} else {
					// choisir un voisin
					long id_neigbor = my_random.nextLong(initial_position.size());
					// choisir un angle
					double angle = my_random.nextDouble() * (Math.PI * 2);
					// position du voisin
					Position pos_neigbor = initial_position.get(id_neigbor);
					// choisir une distance aléatoire du voisin entre distance_init_min et
					// distance_init_max
					double min_distance = Math.min(scope, Math.max(distance_init_min, 0));
					double max_distance = Math.min(scope, distance_init_max);
					double distance = CommonState.r.nextDouble() * (max_distance - min_distance) + min_distance; // Math.min(scope,
																													// CommonState.r.nextDouble()*scope+(scope/3));

					Position position = pos_neigbor.getNewPositionWith(distance, angle).bound(0, 0, maxX, maxY);
					initial_position.put(n.getID(), position);

				}
			}

		}
		// System.out.println("Node "+host.getID()+" init_position =
		// "+initial_position.get(host.getID()));
		return initial_position.get(host.getID());
	}
}
