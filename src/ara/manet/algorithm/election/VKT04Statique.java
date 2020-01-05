package ara.manet.algorithm.election;

import java.util.ArrayList;
import java.util.List;

import ara.manet.Monitorable;
import ara.manet.detection.NeighborProtocol;
import peersim.config.Configuration;
import peersim.core.Node;

public class VKT04Statique implements ElectionProtocol, Monitorable, NeighborProtocol{
	
	private int value; // desirability
	private int my_pid; // protocol
	
	
	
	
	
	public VKT04Statique(String prefix) {
		String tmp[] = prefix.split("\\.");
		my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
	}
	
	public Object clone() {
		VKT04Statique p = null;
		try {
			p = (VKT04Statique) super.clone();
		} catch (CloneNotSupportedException e) {
		}
		return p;
	}
	
	@Override
	public void processEvent(Node node, int pid, Object event) {
		// TODO Auto-generated method stub
		
	}

	/********************************ELECTION PROTOCOL**********************************/
	@Override
	public long getIDLeader() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getValue() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/*****************************NEIGHBORHOOD PROTOCOL******************************/
	/* NeighborProtocol. Puisque nous sommes dans un système
	 * statique, nous allons nous passer ici de la couche de détection dynamique de
	 * voisins via heartbeat (codée dans le précédent exercice) en calculant directement
	 * et statiquement les voisins selon leur position par rapport au rayon d’émission.
	 */
	
	@Override
	public List<Long> getNeighbors() {
		// TODO Auto-generated method stub
		return null;
	}


	/********************************MONITORABLE**********************************/
	/* MONITORABLE : implémente l’interface Monitorable pour afficher sur le moniteur 
	 * graphique l’état de chaque nœud : on peut différencier dans 
	 * cet algorithme trois états (leader inconnu, leader connu, être le leader).
	 */
	
	/* permet d'obtenir le nombre d'état applicatif du noeud */
	public int nbState() {
		// TODO
		return 1;
	}

	
	/* permet d'obtenir l'état courant du noeud */
	@Override
	public  int getState(Node host) {
		// TODO
		return 0;
	}

	/*
	 * permet d'obtenir une liste de chaine de caractère, affichable en colonne à
	 * coté du noeud sur un moniteur graphique
	 */
	public List<String> infos(Node host) {
		// TODO
		List<String> res = new ArrayList<String>();
		res.add("Node" + host.getID());
		return res;
	}
}
