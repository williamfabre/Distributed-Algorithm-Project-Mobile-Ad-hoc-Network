package ara.manet.positioning.strategies;

import java.util.Map;
import java.util.Set;

import ara.manet.communication.Emitter;
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
public class NextDestinationConnectedOneMove implements NextDestinationStrategy {

	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_EMITTERPID = "emitter";
	private static final String PAR_DISTANCEMIN = "distance_min";
	private static final String PAR_DISTANCEMAX = "distance_max";

	private final int position_pid;
	private final int emitter_pid;
	private final int distance_min;
	private final int distance_max;

	private Map<Integer, Set<Node>> initial_connected_component = null;// id_component -> ensemble d'id de node

	private Node currentMoving;
	private final ExtendedRandom my_random;

	public NextDestinationConnectedOneMove(String prefix) {
		position_pid = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
		emitter_pid = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
		distance_min = Configuration.getInt(prefix + "." + PAR_DISTANCEMIN, 0);
		distance_max = Configuration.getInt(prefix + "." + PAR_DISTANCEMAX, Integer.MAX_VALUE);
		this.my_random = PositioningConfiguration.getPositioningRandom();
	}

	@Override
	public Position getNextDestination(Node host, int speed) {
		final int scope = ((Emitter) host.getProtocol(emitter_pid)).getScope();
		PositionProtocol pos_proto_host = ((PositionProtocol) host.getProtocol(position_pid));

		if (initial_connected_component == null) {
			initial_connected_component = PositionProtocol
					.getConnectedComponents(PositionProtocol.getPositions(position_pid), scope);
		}

		if (currentMoving != null) {
			PositionProtocol pos_proto_cur_moving = (PositionProtocol) currentMoving.getProtocol(position_pid);
			if (pos_proto_cur_moving.isMoving()) {// est-il toujours en mouvement
				return pos_proto_host.getCurrentPosition(); // host n'est pas autorisé à bouger.
			} else {
				currentMoving = null;
			}
		}
		// host peut bouger
		// choisir un voisin
		long id_neigbor = my_random.nextLong(Network.size());
		// choisir un angle
		double angle = my_random.nextDouble() * (Math.PI * 2);
		// position du voisin
		Position pos_neigbor = ((PositionProtocol) Network.get((int) id_neigbor).getProtocol(position_pid))
				.getCurrentPosition();
		// choisir une distance aléatoire du voisin entre distance_init_min et
		// distance_init_max
		double min_distance = Math.min(scope, Math.max(distance_min, 0.0));
		double max_distance = Math.min(distance_max, scope);
		double distance = my_random.nextDouble() * (max_distance - min_distance) + min_distance; // Math.min(scope,
																									// my_random.nextDouble()*scope+(scope/3));
		Position new_position = pos_neigbor.getNewPositionWith(distance, angle).bound(0, 0, pos_proto_host.getMaxX(),
				pos_proto_host.getMaxY());
		Map<Long, Position> positions = PositionProtocol.getPositions(position_pid);
		positions.put(host.getID(), new_position);
		Map<?, ?> m = PositionProtocol.getConnectedComponents(positions, scope);
		if (m.size() > 1) {
			return pos_proto_host.getCurrentPosition();// le mouvement de host entraine un split du reseau
		}
		currentMoving = host;
		return new_position;
	}

}
