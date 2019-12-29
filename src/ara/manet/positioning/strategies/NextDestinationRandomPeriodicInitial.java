package ara.manet.positioning.strategies;

import java.util.HashSet;
import java.util.Set;

import ara.manet.positioning.NextDestinationStrategy;
import ara.manet.positioning.Position;
import ara.manet.positioning.PositioningConfiguration;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public class NextDestinationRandomPeriodicInitial implements NextDestinationStrategy {

	private static final String PAR_RANDOM_DEST_PERIOD = "random_dest_period";

	private final int random_dest_period;
	private final FullRandom randompositionning;
	private long nextdate_to_goback;

	private enum State {
		RANDOM, GOBACK_INITIAL
	}

	private State state;
	private Set<Node> nodes_at_initialposition;

	public NextDestinationRandomPeriodicInitial(String prefix) {
		random_dest_period = Configuration.getInt(prefix + "." + PAR_RANDOM_DEST_PERIOD);
		randompositionning = new FullRandom(prefix);
		nextdate_to_goback = random_dest_period;
		state = State.RANDOM;
		nodes_at_initialposition = new HashSet<>();
	}

	@Override
	public Position getNextDestination(Node host, int speed) {
		Position res = null;
		switch (state) {
		case RANDOM:
			res = randompositionning.getNextDestination(host, speed);
			if (CommonState.getTime() >= nextdate_to_goback) {
				state = State.GOBACK_INITIAL;
				nodes_at_initialposition = new HashSet<>();
			}

			break;
		case GOBACK_INITIAL:
			Position initial = PositioningConfiguration.getInitialPositionStrategy().getInitialPosition(host);
			res = initial;
			if (randompositionning.getPositionProcotol(host).getCurrentPosition().equals(initial)
					&& !nodes_at_initialposition.contains(host))
				nodes_at_initialposition.add(host);
			if (nodes_at_initialposition.size() == Network.size()) {
				state = State.RANDOM;
				nextdate_to_goback = CommonState.getTime() + random_dest_period;
			}
			break;
		}
		return res;
	}

}
