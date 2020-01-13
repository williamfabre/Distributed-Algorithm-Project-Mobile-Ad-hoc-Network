package ara.manet.algorithm.election;

import java.util.ArrayList;
import java.util.List;

import ara.manet.Monitorable;
import ara.manet.communication.EmitterProtocolImpl;
import ara.manet.detection.NeighborProtocol;
import ara.manet.positioning.PositionProtocol;
import ara.util.AckMessage;
import ara.util.ElectionMessage;
import ara.util.LeaderMessage;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.util.ExtendedRandom;

public class VKT04StatiqueElection implements ElectionProtocol, Monitorable, NeighborProtocol{
	
	private static final long ALL = -2; // Broadcast == true
	
	private static final String PAR_PERIODE_LEADER = "periode_leader";
	private static final String PAR_PERIODE_NEIGHBOR = "periode_neighbor";
	private static final String PAR_TIMER = "timer";
	private static final String PAR_SCOPE = "scope";
	public static final String leader_event = "LEADEREVENT";
	
	
	private int my_pid; 							// protocol
	
	private final int periode_leader;				// duree entre deux elections 

	private final int periode_neighbor;				// delay avant declenchement timer 
													// entre deux check de mes voisins

	private final long timer_event;					// Tant qu'il est armee, les noeuds de la liste 
													// des neighbors sont consideres comme voisins
													// apres timer seconde ils disparaissent de la liste.
	
	private int scope;								// visibilité d'un node
	
	private List<Long> neighbors;					// Liste de voisins.
	private List<Integer> values; 					// Valeur nécessaire pour les leader protocol.
	private List<Long> neighbors_ack;				// permet de compter le nombre de ack TODO					
	private int desirability; 						// desirabilité du noeud									(-1 si inconnu)
	private long parent; 							// permet de connaître son père et remonter dans l'arbre 	(-1 si inconnu)
	private long id_leader;							// id du leader actuel, -1 si aucun leader.					(-1 si inconnu)
	private long desirability_leader;				// desirabilité du noeud leader								(-1 si inconnu)
	private long potential_leader;					// id du leader potentiel, -1 si aucun leader.				(-1 si inconnu)
	private long desirability_potential_leader;		// désirabilité du leader potentiel, -1 si aucun leader.	(-1 si inconnu)
	private int state;								// 0 : leader_known
													// 1 : leader_unknown
													// 2 : leader_isMe


	
	public VKT04StatiqueElection(String prefix) {
		
		String tmp[] = prefix.split("\\.");
		my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		
		this.periode_leader = Configuration.getInt(prefix + "." + PAR_PERIODE_LEADER);
		this.periode_neighbor = Configuration.getInt(prefix + "." + PAR_PERIODE_NEIGHBOR);
		this.timer_event = Configuration.getInt(prefix + "." + PAR_TIMER);
		this.scope = Configuration.getInt(prefix + "." + PAR_SCOPE);
		
		// Creation de liste privees.
		neighbors = new ArrayList<Long>(); 	// Liste des voisins
		values = new ArrayList<Integer>(); 	// liste des valeurs
		neighbors_ack = new ArrayList<Long>(); 	// liste noeuds qui ont ack
		parent = -1;
		id_leader = -1;
		desirability_leader = -1;
		potential_leader = -1;
		desirability_potential_leader = -1;
		state = 1;

	}
	
	public Object clone() {
		VKT04StatiqueElection vtk = null;
		try {
			vtk = (VKT04StatiqueElection) super.clone();
			vtk.neighbors = new ArrayList<Long>(); 		// Liste des voisins
			vtk.values = new ArrayList<Integer>(); 		// liste des valeurs
			vtk.neighbors_ack = new ArrayList<Long>(); 	// liste noeuds qui ont ack
			vtk.parent = -1;
			vtk.id_leader = -1;
			vtk.desirability_leader = -1;
			vtk.potential_leader = -1;
			vtk.desirability_potential_leader = -1;
			vtk.state = 1;
		} catch (CloneNotSupportedException e) {
		}
		return vtk;
	}

	
	/**
	 * Fonction utilisé par la classe d'initialisation qui est appelée
	 * en début de programme pour tous les noeuds.
	 * Elle a pour but d'initialisé la désirability du node avec son ID en paramètre.
	 * 
	 * @param node le node en lui même
	 */
	public void initialisation(Node node) {
		ExtendedRandom my_random = new ExtendedRandom(10);
		this.desirability = (int) (my_random.nextInt(1000) / (node.getID() + 1));
		
		// TODO initialiser le leader à même au départ? id_leader ?
	}

	
	/**
	 * Partie détection statique
	 * Détecteur statique de voisins qui va déterminer 
	 * en tout instant qui est dans mon scope atteignable.
	 * 
	 * @param host host
	 */
	private void staticDetection(Node host) {

		int position_pid = Configuration.lookupPid("position");
		PositionProtocol phost = (PositionProtocol) host.getProtocol(position_pid);
		
		for (int i = 0; i < Network.size(); i++) {
			
			Node node = Network.get(i);
			PositionProtocol pnode = (PositionProtocol) node.getProtocol(position_pid);
			double distance = pnode.getCurrentPosition().distance(phost.getCurrentPosition());
			
			
			if (distance <= getScope()) {
				// node dans le scope et pas dans la liste encore 		=> ajouter
				if (!neighbors.contains(node.getID())){
					neighbors.add(node.getID());

				}
			} else {
				// node pas dans le scope et deja dans la liste 		=> supprimer
				if (neighbors.contains(node.getID())) {
						neighbors.remove(node.getID());
				}
			}
			
			// recopie dans la liste des personnes que je dois attendre.
			neighbors_ack.addAll(neighbors);
		}
		// création du réveil pour refaire une vérification de mes voisins
		EDSimulator.add(periode_neighbor, timer_event, host, my_pid);
	}
	
	/**
	 * Partie élection statique, va lancer une nouvelle élection
	 * avec la liste statique des neouds.
	 * 
	 * @param host
	 */
	void VKT04StaticElectionTrigger(Node host) {

		// Récupération du protocol de communication
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		// Début d'une demande d'éléction globale, mise à jour du node
		// pour débuter une éléction
		this.state = 1;
		this.parent = -1;
		this.id_leader = -1;
		this.desirability_leader = -1;
		this.desirability_potential_leader = desirability;
		this.potential_leader = host.getID();
		ElectionMessage em = new ElectionMessage(host.getID(), ALL, my_pid);
		emp.emit(host, em);
		
		
		// Ajouter de la variance pour ne pas que les noeuds lance tout le temps des élections
		// exactement en même temps.
		EDSimulator.add(periode_leader, leader_event, host, my_pid);
	}
	
	/**
	 * TODO
	 * 
	 * @param host
	 * @param event
	 */
	private void recvElectionMsg(Node host, ElectionMessage event) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		ElectionMessage em = (ElectionMessage)event;
		
		// Si je n'ai pas de parent j'ajoute l'envoyeur comme mon père
		// le ack message attendra que j'ai reçu une réponse de tous
		// mes fils.
		if (this.parent == -1) {
			
			if (em.getIdSrc() != host.getID()) {
				// Ce noeud est mon père
				this.parent = em.getIdSrc();

				// Je ne dois pas attendre mon père
				neighbors_ack.remove(this.parent);
			
				// Propagation aux fils
				for (Long neinei : neighbors) {
				
					Node dest = Network.get(neinei.intValue()); // TODO ??????
					if(dest.getID() == parent) { continue; } // Skip l'id du pere
				
					ElectionMessage em_propagation = new ElectionMessage(host.getID(), dest.getID(), my_pid);
					emp.emit(host, em_propagation);
				}
			}
		} else {
			// Je me choisi par défaut
			this.desirability_potential_leader = desirability;
			this.potential_leader = host.getID();
			
			// J'ai déjà un parent, réponse immediate de ma propre valeur 
			AckMessage am = new AckMessage(host.getID(), em.getIdSrc(), my_pid, host.getID(), desirability);
			emp.emit(host, am);
		}
		return;
	}

	
	/**
	 * @param host
	 * @param event
	 */
	private void recvAckMsg(Node host, AckMessage event) {

		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		AckMessage am = (AckMessage)event;
		
		// Mise a jour de mon noeud leader si le leader que 
		// j'ai est moins désirable.
		if (am.getMostValuedNodeDesirability() > this.desirability_potential_leader) {
			
			this.potential_leader = am.getMostValuedNode();
			this.desirability_potential_leader = am.getMostValuedNodeDesirability();
		}
		
		// J'ai reçu un ack de ce node c'est bon !
		neighbors_ack.remove(am.getIdSrc()); // remove is empty safe.
		
		// Je suis une feuille ou il n'y avait qu'un fils à attendre
		if (neighbors_ack.isEmpty()) {
		
			// Fin de l'élection je suis le noeud de départ TODO??
			// je dois maintenant propager ma valeur. TODO ??
			if (parent == -1) {
				id_leader = potential_leader;
				desirability_leader = desirability_potential_leader;
				
				if (id_leader == host.getID()) {
					state = 2; 		// 2 : leader_isMe
				} else {
					state = 0;		// 0 : leader_known
				}

				// Broadcast du message de leader
				LeaderMessage lm_broadcast = new LeaderMessage(host.getID(), ALL, my_pid, id_leader, desirability_leader);
				emp.emit(host, lm_broadcast);
			} else {
				
				// Envoie d'un ack à mon père, je suis une feuille
				AckMessage am_to_father = new AckMessage(host.getID(), parent, my_pid, potential_leader, desirability_potential_leader);
				emp.emit(host, am_to_father);
			}
		}
		
	}	
	
	/**
	 * TODO 
	 * 
	 * @param host
	 * @param event
	 */
	private void recvLeaderlMsg(Node host, LeaderMessage event) {
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		LeaderMessage lm = (LeaderMessage)event;
		
		if (state == 1) { // 1 : leader_unknown
			LeaderMessage lm_propagete = new LeaderMessage(host.getID(), ALL, my_pid, id_leader, desirability_leader);
			emp.emit(host, lm_propagete);
			
			if (lm.getMostValuedNode() == host.getID()) {
				state = 2; 		// 2 : leader_isMe
				id_leader = host.getID();
				desirability_leader = desirability;
			} else {
				state = 0;		// 0 : leader_known
				id_leader = lm.getMostValuedNode();
				desirability_leader = lm.getMostValuedNodeDesirability();
			}
		}
		
	}


	/********************************ELECTION PROTOCOL**********************************/
	@Override
	public long getIDLeader() {
		return id_leader;
	}

	@Override
	public int getValue() {
		return this.desirability;
	}
	
	/*****************************NEIGHBORHOOD PROTOCOL******************************/
	/* NeighborProtocol. Puisque nous sommes dans un système
	 * statique, nous allons nous passer ici de la couche de détection dynamique de
	 * voisins via heartbeat (codée dans le précédent exercice) en calculant directement
	 * et statiquement les voisins selon leur position par rapport au rayon d'émission.
	 */
	
    /**
     * Retourne la liste des neighbours du noeud courant
     * 
     * @return np liste de noeud
     */
	@Override
	public List<Long> getNeighbors() {
		return neighbors;
	}
	
	 /**
     * Retourne la valeur du noeud dans notre liste.
     * 
     * @return la valeur du noeud id_new_neighbor
     */
	public int getNeighborValue(long id_new_neighbor) {
		
		return values.get(neighbors.indexOf(id_new_neighbor));
	}

	/**
	 * @return le scope du current node.
	 */
	public int getScope() {
		
		return this.scope;
	}
	
	/********************************MONITORABLE**********************************/
	/* MONITORABLE : implémente l'interface Monitorable pour afficher sur le moniteur 
	 * graphique l'é©tat de chaque noeud : on peut différencier dans 
	 * cet algorithme trois états : 
	 * 
	 * * leader inconnu,
	 * * leader connu, 
	 * * être le leader.
	 */
	
	/* permet d'obtenir le nombre d'état applicatif du noeud */
	public int nbState() {
		return 3;
	}

	/* permet d'obtenir l'état courant du noeud */
	@Override
	public  int getState(Node host) {
		return state;
	}

	/*
	 * permet d'obtenir une liste de chaine de caractère, affichable en colonne à 
	 * coté du noeud sur un moniteur graphique
	 */
	public List<String> infos(Node host) {
		List<String> res = new ArrayList<String>();
		res.add("Node" + host.getID() + " Boss["+ getIDLeader() + "]" + "\n Val(" + getValue() + ")");
		return res;
	}
	
	
	
	/********************************ProcessEvent**********************************/	
	@Override
	public void processEvent(Node host, int pid, Object event) {
		
		if (pid != my_pid) {
			throw new RuntimeException("Receive Event for wrong protocol");
		}
		
		// TODO
		if (event instanceof ElectionMessage) {
			recvElectionMsg(host, (ElectionMessage) event);
			return;
		}
		
		// TODO
		if (event instanceof LeaderMessage) {
			recvLeaderlMsg(host, (LeaderMessage) event);
			return;
		}

		// TODO
		if (event instanceof AckMessage) {
			recvAckMsg(host, (AckMessage) event);
			return;
		}
		
		// Je dois verifier la liste de mes voisins a chaque periode de temps
		// nommee timer.
		if (event.equals(timer_event)) {
			staticDetection(host);
			return;
		}
		
		// Evènement périodique d'affichage d'élections.
		if (event instanceof String) {
			String ev = (String) event;

			if (ev.equals(leader_event)) {
				System.out.println(host.getIndex() + " : value " + getValue() +" : Leader " + getIDLeader());
				VKT04StaticElectionTrigger(host);
				return;
			}
		}
		
		throw new RuntimeException("Receive unknown Event");
	}
}