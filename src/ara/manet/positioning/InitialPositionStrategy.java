package ara.manet.positioning;

import peersim.core.Node;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public interface InitialPositionStrategy {

	/*
	 * retourne la position initiale du noeud host, doit toujours renvoyer la même
	 * valeur pour un noeud donné
	 */
	public Position getInitialPosition(Node host);
}
