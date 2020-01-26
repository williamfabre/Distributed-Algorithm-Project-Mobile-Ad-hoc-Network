package ara.manet.communication;

import ara.manet.algorithm.election.VKT04Election;
import ara.manet.detection.NeighborProtocolImpl;
import ara.manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class InitialisationVKT04Election implements Control {

	public  InitialisationVKT04Election(String prefix) {}
	
	/**
	 * Provient du protocol de contr�le et permet de cr�er un �tat
	 * initial coh�rent pour les noeuds et de lancer l'algorithme
	 * d'�lection VKT04Statique.
	 */
	@Override
	public boolean execute() {
		
		 //R�up�re les informations n�cessaires dans le protocol 
		int position_pid = Configuration.lookupPid("position");
		int neighbor_pid = Configuration.lookupPid("neighbor");
		int elect_pid = Configuration.lookupPid("election");
		
		
		// Traitement pour tous les noeuds
		for(int i = 0; i < Network.size(); i++) {
			
			Node node = Network.get(i);
			
			// Protocol de positionnement
			PositionProtocolImpl position = (PositionProtocolImpl) node.getProtocol(position_pid);
	
			// Protocol de detection de neighbors
			NeighborProtocolImpl np = (NeighborProtocolImpl) node.getProtocol(neighbor_pid);
			
			
			// r�cup�ration du protocol d'�lection
			//VKT04Election vkt04 = (VKT04Election) node.getProtocol(elect_pid);
			VKT04Election vkt04 = (VKT04Election) node.getProtocol(elect_pid);
			
			
			// initialise la position pour tous les noeuds
			position.initialiseCurrentPosition(node);
	
			// Evenement permettant le d�placement des noeuds.
			position.processEvent(node, position_pid, "LOOPEVENT");
						

			/* ensemble des processevent n�cessaire au fonctionnement */
			
			// Evenement de detection de voisins
			np.processEvent(node, neighbor_pid, "HEARTEVENT");
			
			

			// fonction d'initialisation si n�cessaire
			vkt04.initialisation(node);
			
			
			// Evenement permettant l'affichage des r�sultats des �lections p�riodiquement.
			vkt04.processEvent(node, elect_pid, "LEADEREVENT");
		} 
		
		return false;
	}
}
