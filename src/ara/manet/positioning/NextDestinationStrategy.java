package ara.manet.positioning;

import peersim.core.Node;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public interface NextDestinationStrategy {

	/*
	 * retourne une nouvelle de destination du neoud host en fonction de la vitesse
	 * speed
	 */
	public Position getNextDestination(Node host, int speed);
}
