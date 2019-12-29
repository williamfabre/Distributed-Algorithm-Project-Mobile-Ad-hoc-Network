package ara.manet.positioning;

import peersim.config.Configuration;
import peersim.util.ExtendedRandom;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public final class PositioningConfiguration {

	private PositioningConfiguration() {
	}

	private static final String PAR_INITIAL_POSITION_STRATEGY = "initial_position_strategy";
	private static final String PAR_NEXT_DESTINATION_STRATEGY = "next_destination_strategy";
	private static final String PAR_SEED_POSITIONING = "seed_positionning";

	private static InitialPositionStrategy initial_position_strategy = null;
	private static NextDestinationStrategy next_destination_strategy = null;
	private static ExtendedRandom positionningrandom = null;

	public static InitialPositionStrategy getInitialPositionStrategy() {
		if (initial_position_strategy == null) {
			initial_position_strategy = (InitialPositionStrategy) Configuration
					.getInstance(PAR_INITIAL_POSITION_STRATEGY);
		}
		return initial_position_strategy;
	}

	public static NextDestinationStrategy getNextDestinationStrategy() {
		if (next_destination_strategy == null) {
			next_destination_strategy = (NextDestinationStrategy) Configuration
					.getInstance(PAR_NEXT_DESTINATION_STRATEGY);
		}
		return next_destination_strategy;
	}

	public static ExtendedRandom getPositioningRandom() {
		if (positionningrandom == null) {
			positionningrandom = new ExtendedRandom(Configuration.getLong(PAR_SEED_POSITIONING, 1));
		}
		return positionningrandom;
	}
}
