package ara.manet.detection;

import java.util.List;

import peersim.core.Protocol;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public interface NeighborProtocol extends Protocol {
	
	/* désigne tous les voisins accessibles */
	public static final int ALL = -2;
	
	/* Renvoie la liste courante des Id des voisins directs */
	public List<Long> getNeighbors();
}
