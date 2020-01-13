package ara.manet.communication;

import ara.manet.algorithm.election.VKT04StatiqueElection;
import ara.manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class InitialisationVKT04StatiqueElection implements Control {

	public  InitialisationVKT04StatiqueElection(String prefix) {}
	
	/**
	 * Provient du protocol de contrôle et permet de créer un état
	 * initial cohérent pour les noeuds et de lancer l'algorithme
	 * d'élection VKT04Statique.
	 */
	@Override
	public boolean execute() {
		
		 //Réupère les informations nécessaires dans le protocol 
		int position_pid = Configuration.lookupPid("position");
		int elect_pid = Configuration.lookupPid("election");
		
		
		// Traitement pour tous les noeuds
		for(int i = 0; i < Network.size(); i++) {
			
			Node node = Network.get(i);
			PositionProtocolImpl position = (PositionProtocolImpl) node.getProtocol(position_pid);
	
			
			// récupération du protocol d'élection
			VKT04StatiqueElection vkt04 = (VKT04StatiqueElection) node.getProtocol(elect_pid);
			
			
			// initialise la position pour tous les noeuds
			position.initialiseCurrentPosition(node);
	
		
			// fonction d'initialisation si nécessaire
			vkt04.initialisation(node);
			
			
			/* ensemble des processevent nécessaire au fonctionnement */
			// Evenement permettant le déplacement des noeuds.
			position.processEvent(node, position_pid, "LOOPEVENT");
			
			// Evenement permettant l'affichage des résultats des élections périodiquement.
			vkt04.processEvent(node, elect_pid, "LEADEREVENT");

		} 
		
		return false;
	}
}
