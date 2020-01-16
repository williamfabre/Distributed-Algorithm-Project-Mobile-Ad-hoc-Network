package ara.manet.algorithm.election;

import java.util.ArrayList;
import java.util.List;

import ara.manet.Monitorable;
import ara.manet.communication.EmitterProtocolImpl;
import ara.manet.detection.NeighborProtocolVKTImpl;
import ara.manet.detection.NeighborhoodListener;
import ara.util.AckMessage;
import ara.util.ElectionDynamicMessage;
import ara.util.LeaderMessage;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
	
public class VKT04Election implements ElectionProtocol, Monitorable, NeighborhoodListener {

	private static final long ALL = -2; // Broadcast == true
	
	private static final String PAR_PERIODE_LEADER = "periode_leader";
	private static final String PAR_TIMER = "timer";
	public static final String leader_event = "LEADEREVENT";
	
	private int my_pid; 							// protocol
	
	private final int periode_leader;				// duree entre deux elections 

	private List<Long> neighbors;					// Liste de voisins.
	private List<Long> neighbors_ack;				// permet de compter le nombre de ack				
	private int desirability; 						// desirabilité du noeud									(-1 si inconnu)
	private long parent; 							// permet de connaître son père et remonter dans l'arbre 	(-1 si inconnu)
	private long id_leader;							// id du leader actuel, -1 si aucun leader.					(-1 si inconnu)
	private long desirability_leader;				// desirabilité du noeud leader								(-1 si inconnu)
	private long potential_leader;					// id du leader potentiel, -1 si aucun leader.				(-1 si inconnu)
	private long desirability_potential_leader;		// désirabilité du leader potentiel, -1 si aucun leader.	(-1 si inconnu)
	
	// new variables for dynamic protocol
	private boolean is_electing;		// Variable qui dit si ce noeud est en train de faire une éléction.			(0 si inconnu)
	private long ack_2_parent;			// Variable qui dit si ce noeud a envoyé son ack à son père.				(0 si inconnu)
	private long source_election;		// Noeud d'où provient l'élection dans laquelle je suis.					(-1 si inconnu)
	private long ieme_election;			// indique pour ce node la ieme election qu'il lance.						(0 si inconnu)
										// utile pour differencier les elections et choisir parmi elles.			
										// Plus un node lance d'election plus il a de chance d'en lancer.
										// Soit i,j Node² : (i.ieme_election(), i.getID()) > (j.ieme_election(), j.getID())
										// <=> i.ieme_election() > j.ieme_election() ||
										// (i.ieme_election() == j.ieme_election()) &&  (i.getID() >  j.getID())
	
	private long source_ieme_election; // ieme election de la source a laquelle je participe
	
	private long ieme_election_max;	// La plus grande election à laquelle j'ai participé.						(0 si inconnu)
	
	private int state;								// 0 : leader_known
													// 1 : leader_unknown
													// 2 : leader_isMe
	
public VKT04Election(String prefix) {
		
		String tmp[] = prefix.split("\\.");
		my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		
		this.periode_leader = Configuration.getInt(prefix + "." + PAR_PERIODE_LEADER);
		
		// Creation de liste privees.
		neighbors = new ArrayList<Long>(); 		// Liste des voisins
		neighbors_ack = new ArrayList<Long>(); 	// liste noeuds qui ont ack
		parent = -1;
		id_leader = -1;
		desirability_leader = -1;
		potential_leader = -1;
		desirability_potential_leader = -1;
		state = 1;
		is_electing = false;
		ack_2_parent = 0;
		source_election = -1;
		source_ieme_election = -1;
		ieme_election = 0;
		ieme_election_max = 0;	

	}
	
	public Object clone() {
		VKT04Election vkt = null;
		try {
			vkt = (VKT04Election) super.clone();
			vkt.neighbors = new ArrayList<Long>(); 		// Liste des voisins
			vkt.neighbors_ack = new ArrayList<Long>(); 	// liste noeuds qui ont ack
			vkt.parent = -1;
			vkt.id_leader = -1;
			vkt.desirability_leader = -1;
			vkt.potential_leader = -1;
			vkt.desirability_potential_leader = -1;
			vkt.state = 1;
			
			vkt.is_electing = false;
			vkt.ack_2_parent = 0;
			vkt.source_election = -1;
			vkt.source_ieme_election = -1;
			vkt.ieme_election = 0;
			vkt.ieme_election_max = 0;	
		} catch (CloneNotSupportedException e) {
		}
		return vkt;
	}
	
	/**
	 * Fonction utilisé par la classe d'initialisation qui est appelée
	 * en début de programme pour tous les noeuds.
	 * Elle a pour but d'initialisé la désirability du node avec son ID en paramètre.
	 * 
	 * @param node le node en lui même
	 */
	public void initialisation(Node node) {
		this.desirability = node.getIndex();
	}
	
	
	/*****************************Election******************************/	
	
	/**
	 * Partie élection statique, va lancer une nouvelle élection
	 * avec la liste statique des neouds.
	 * 
	 * @param host
	 */
	void VKT04ElectionTrigger(Node host) {

		// Récupération du protocol de communication
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		// Recuperation du protocol de Neighbor
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol((neighbor_pid));
		
		
		// Début d'une demande d'éléction globale, mise à jour du node
		// pour débuter une éléction
		this.state = 1;
		this.parent = -1;
		this.id_leader = -1;
		this.desirability_leader = -1;
		this.desirability_potential_leader = desirability;
		this.potential_leader = host.getID();
		
		is_electing = false;					// je suis passe en mode election.
		ack_2_parent = 1;						// je n'ai pas besoin d'attendre un ack.
		source_election = host.getID();			// je suis le createur de cette election.
		ieme_election++;						// Pour calculer la priorite de mon election.
		source_ieme_election = ieme_election;	// je suis ma propre source
		ieme_election_max = Math.max(ieme_election_max, ieme_election);
		
		ElectionDynamicMessage edm = new ElectionDynamicMessage(host.getID(), ALL, host.getID(), ieme_election, my_pid);
		emp.emit(host, edm);
		neighbors = np.getNeighbors();
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
	private void recvElectionDynamicMsg(Node host, ElectionDynamicMessage event) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		ElectionDynamicMessage edm = (ElectionDynamicMessage)event;
		
		// Si je n'ai pas de parent j'ajoute l'envoyeur comme mon père
		// le ack message attendra que j'ai reçu une réponse de tous
		// mes fils.
		if (this.parent == -1) {
			
			if (edm.getIdSrc() != host.getID()) {
				
				is_electing = true;	// je passe en mode election
				parent = edm.getIdSrc();// Ce noeud est mon père
				neighbors_ack.remove(this.parent); // Je ne dois pas attendre mon père
				source_election = edm.getSource_election(); // Je met a jour a source de l'election
				source_ieme_election = edm.getIeme_election(); // Je met a jour le ieme de la source de l'election
			
				// Propagation aux fils
				for (Long neinei : neighbors_ack) {
					
					Node dest = Network.get(neinei.intValue()); // TODO ??????
					if(dest.getID() == parent) { continue; } // Skip l'id du pere
					ElectionDynamicMessage em_propagation = new ElectionDynamicMessage(host.getID(), dest.getID(), edm.getSource_election(), ieme_election, my_pid);
					emp.emit(host, em_propagation);
				}
			}
			
		// J'ai deja un pere alors il faut checker si cette personne est plus prioritaire.
		} else {
			
			// Checker si cette election n'est pas plus priritaire pour changer mon pere
			if (this.ieme_election > edm.getIeme_election() ||
					this.ieme_election <= edm.getIeme_election() 
					&& this.desirability_leader > edm.getSource_election()) {
				// J'ai déjà un parent, 
				// J'ai déjà un parent, réponse immediate de la valeur potentielle
				AckMessage am = new AckMessage(host.getID(), edm.getIdSrc(), my_pid, potential_leader, desirability_potential_leader);
				emp.emit(host, am);
			} else {
				
				if (edm.getIdSrc() != host.getID()) {
					
					is_electing = true;	// je passe en mode election
					parent = edm.getIdSrc();// Ce noeud est mon père
					neighbors_ack.remove(this.parent); // Je ne dois pas attendre mon père
					source_election = edm.getSource_election(); // Je met a jour a source de l'election
					source_ieme_election = edm.getIeme_election(); // Je met a jour le ieme de la source de l'election
				
					// Propagation aux fils
					for (Long neinei : neighbors_ack) {
						
						Node dest = Network.get(neinei.intValue()); // TODO ??????
						if(dest.getID() == parent) { continue; } // Skip l'id du pere
						ElectionDynamicMessage em_propagation = new ElectionDynamicMessage(host.getID(), dest.getID(), edm.getSource_election(), ieme_election, my_pid);
						emp.emit(host, em_propagation);
					}
				}
			}
		}
		return;
	}

	
	/**
	 * TODO 
	 * 
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
		
		// J'ai reçu un ack de ce node c'est bon, remove safe d'un node DANS (il y est) ma liste !
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
				LeaderMessage lm_broadcast = new LeaderMessage(host.getID(), ALL, my_pid, id_leader, desirability_leader, source_election, ieme_election);
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
		
		switch (state) {
		case 0:							// 0 : leader_known
		case 2: 						// 2 : leader_isMe
			// Je recois un message de leader et je connais deja mon leader
			// Si il est legitime alors
			if (this.source_ieme_election > event.getIeme_election() ||
					this.source_ieme_election <= event.getIeme_election() 
					&& this.desirability_leader > event.getSource_election()) {
				// propager  mon leader
				LeaderMessage lm_local = new LeaderMessage(host.getID(), event.getIdSrc(), 
						my_pid, 
						id_leader,
						desirability_leader, 
						source_election, 
						source_ieme_election);
				
				emp.emit(host, lm_local);
			
			} else {
				
				// Si il est illegitime alors je dois le modifier
					// propager le nouveau leader
				LeaderMessage lm_broadcast = new LeaderMessage(host.getID(), ALL, my_pid, 
						event.getMostValuedNode(), 
						event.getMostValuedNodeDesirability(),
						event.getSource_election(), 
						event.getIeme_election());
				
				emp.emit(host, lm_broadcast);
				
			}
			break;
			
		case 1:							// 1 : leader_unknown
			if (is_electing) {
				if (lm.getMostValuedNode() == host.getID()) {
					state = 2; 		// 2 : leader_isMe
					id_leader = host.getID();
					desirability_leader = desirability;
				} else {
					state = 0;		// 0 : leader_known
					id_leader = lm.getMostValuedNode();
					desirability_leader = lm.getMostValuedNodeDesirability();
				}
				
				LeaderMessage lm_propagate = new LeaderMessage(host.getID(), ALL, my_pid, id_leader, desirability_leader, source_election, source_ieme_election);
				emp.emit(host, lm_propagate);
				
			} else {
				// Je ne suis pas en cours d'election et je recois un message de leader
				// Je dois verifier si je ne peux pas devenir le leader
				// Je peux etre leader ?
				// election!
				// Je ne peux pas, je me soumet.
				// TODO
			}
			break;
		default:
			break;
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

	
	/*****************************NEIGHBORHOOD Listener******************************/
	/**
	 * TODO
	 */
	@Override
	public void newNeighborDetected(Node host, long id_new_neighbor) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol(neighbor_pid);
		
		neighbors = np.getNeighbors();
		
		switch (state) {
		case 0:							// 0 : leader_known
		case 2: 						// 2 : leader_isMe
			// Echange de leader
			// Je rediffuse pour qu'il s'insere avec nous ou pas
			LeaderMessage lm_cible = new LeaderMessage(host.getID(), id_new_neighbor, my_pid, id_leader, desirability_leader, source_election, source_ieme_election);
			emp.emit(host, lm_cible);
			break;
		case 1:							// 1 : leader_unknown
			if (is_electing) {
				// phase de diffusion du ElectionMessage [en cours]
				ElectionDynamicMessage em_propagation = new ElectionDynamicMessage(host.getID(), id_new_neighbor, source_election, source_ieme_election, my_pid);
				emp.emit(host, em_propagation);

			} else {
				// Je trigger une election TODO?
				 VKT04ElectionTrigger(host);
			}
			break;
		default:
			break;
		}
	}
	
	/**
	 * TODO
	 */
	@Override
	public void lostNeighborDetected(Node host, long id_lost_neighbor) {
		
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol(neighbor_pid);
		neighbors = np.getNeighbors();
		
		switch (state) {
		case 0:							// 0 : leader_known
			if (is_electing) {
				
			} else {
				if (id_lost_neighbor == id_leader) {
					VKT04ElectionTrigger(host);
				}
			}
			break;
		case 1:							// 1 : leader_unknown
			if (is_electing) {
				
			} else {
				
			}
			break;
		case 2: 						// 2 : leader_isMe
			if (is_electing) {
				
			} else {
				
			}
		default:
			break;
		}
		
		// TODO
		neighbors_ack.remove(id_lost_neighbor); // remove is empty safe.
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
		res.add("Node" + host.getID() + " Boss["+ getIDLeader() + "]");
		res.add(" PBoss["+ potential_leader + "]" + "\n Val(" + getValue() + ")");
		return res;
	}
	
	
	/********************************ProcessEvent**********************************/	
	@Override
	public void processEvent(Node host, int pid, Object event) {
		
		if (pid != my_pid) {
			throw new RuntimeException("Receive Event for wrong protocol");
		}
		
		// Gestion de la réception d'un message de type ElectionMessage
		if (event instanceof ElectionDynamicMessage) {
			recvElectionDynamicMsg(host, (ElectionDynamicMessage) event);
			return;
		}
		
		// Gestion de la réception d'un message de type LeaderMessage
		if (event instanceof LeaderMessage) {
			recvLeaderlMsg(host, (LeaderMessage) event);
			return;
		}

		// Gestion de la réception d'un message de type AckMessage
		if (event instanceof AckMessage) {
			recvAckMsg(host, (AckMessage) event);
			return;
		}
		
		// Evènement périodique d'élections.		
		if (event instanceof String) {
			String ev = (String) event;

			if (ev.equals(leader_event)) {
				VKT04ElectionTrigger(host);
				return;
			}
		}
		
		throw new RuntimeException("Receive unknown Event");
	}

}
