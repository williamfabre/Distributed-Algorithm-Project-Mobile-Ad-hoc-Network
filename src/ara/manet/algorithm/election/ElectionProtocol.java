package ara.manet.algorithm.election;

import peersim.edsim.EDProtocol;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public interface ElectionProtocol extends EDProtocol {

	/*
	 * renvoie l'identité du leader du noeud courant, peut renvoyer -1 si le leader
	 * n'est pas encore connu
	 */
	public long getIDLeader();

	/*
	 * renvoie la valeur associée au noeud, plus cette valeur est elevée plus le
	 * site a des chances d'être élu
	 */
	public int getValue();

}
