package ara.manet.communication;

import ara.manet.algorithm.election.VKT04Election;
import ara.manet.detection.NeighborProtocolVKTImpl;
import ara.manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class InitialisationVKT04Election implements Control {

	public  InitialisationVKT04Election(String prefix) {}
	
	/**
	 * Provient du protocol de contrôle et permet de créer un état
	 * initial cohérent pour les noeuds et de lancer l'algorithme
	 * d'élection VKT04Statique.
	 */
	@Override
	public boolean execute() {
		
		 //Réupère les informations nécessaires dans le protocol 
		int position_pid = Configuration.lookupPid("position");
		int neighbor_pid = Configuration.lookupPid("neighbor");
		int elect_pid = Configuration.lookupPid("election");
		
		
		// Traitement pour tous les noeuds
		for(int i = 0; i < Network.size(); i++) {
			
			Node node = Network.get(i);
			
			// Protocol de positionnement
			PositionProtocolImpl position = (PositionProtocolImpl) node.getProtocol(position_pid);
	
			// Protocol de detection de neighbors
			NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) node.getProtocol(neighbor_pid);
			
			
			// récupération du protocol d'élection
			VKT04Election vkt04 = (VKT04Election) node.getProtocol(elect_pid);
			
			
			// initialise la position pour tous les noeuds
			position.initialiseCurrentPosition(node);
	
		

			/* ensemble des processevent nécessaire au fonctionnement */
			
			// Evenement de detection de voisins
			np.processEvent(node, neighbor_pid, "HEARTEVENT");
			
			// Evenement permettant le déplacement des noeuds.
			position.processEvent(node, position_pid, "LOOPEVENT");
			

			// fonction d'initialisation si nécessaire
			vkt04.initialisation(node);
			
			
			// Evenement permettant l'affichage des résultats des élections périodiquement.
			vkt04.processEvent(node, elect_pid, "LEADEREVENT");
		} 
		
		return false;
	}
}
