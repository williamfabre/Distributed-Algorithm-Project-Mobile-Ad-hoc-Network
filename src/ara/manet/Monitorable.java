package ara.manet;

import java.util.ArrayList;
import java.util.List;

import peersim.core.Node;
import peersim.core.Protocol;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public interface Monitorable extends Protocol {

	/* permet d'obtenir le nombre d'état applicatif du noeud */
	public default int nbState() {
		return 1;
	}

	/* permet d'obtenir l'état courant du noeud */
	public default int getState(Node host) {
		return 0;
	}

	/*
	 * permet d'obtenir une liste de chaine de caractère, affichable en colonne à
	 * coté du noeud sur un moniteur graphique
	 */
	public default List<String> infos(Node host) {
		List<String> res = new ArrayList<String>();
		res.add("Node" + host.getID());
		return res;
	}
}
