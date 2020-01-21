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
	public static final String leader_event = "LEADEREVENT";
	
	private int my_pid; 							// protocol
	
	private final int periode_leader;				// duree entre deux elections 
	
	private List<Long> neighbors;					// Liste de voisins.
	//private ArrayList<Pair<Long, ArrayList<Long>>> neighbors_ack;	// permet de compter le nombre de ack	 par elections differentes.	
	private List<Long> neighbors_ack;					// Liste de voisins.
	private int desirability; 						// desirabilité du noeud									(-1 si inconnu)
	private long parent; 							// permet de connaître son père et remonter dans l'arbre 	(-1 si inconnu)
	private long id_leader;							// id du leader actuel, -1 si aucun leader.					(-1 si inconnu)
	private long desirability_leader;				// desirabilité du noeud leader								(-1 si inconnu)
	private long potential_leader;					// id du leader potentiel, -1 si aucun leader.				(-1 si inconnu)
	private long desirability_potential_leader;		// désirabilité du leader potentiel, -1 si aucun leader.	(-1 si inconnu)
	
	// new variables for dynamic protocol
	private boolean is_electing;		// Variable qui dit si ce noeud est en train de faire une éléction.			(0 si inconnu)
	private boolean ack_2_parent;		// Variable qui dit si ce noeud a envoyé son ack à son père.				(false si inconnu)
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
	private static int ack_message;
	private static int leader_message;
	private static int election_dynamic_message;
	
	boolean first = false;
	
public VKT04Election(String prefix) {

		String tmp[] = prefix.split("\\.");
		this.my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		
		this.periode_leader = Configuration.getInt(prefix + "." + PAR_PERIODE_LEADER,-1);
		
		// Creation de liste privees.
		this.neighbors = new ArrayList<Long>(); 		// Liste des voisins
		this.neighbors_ack = new ArrayList<Long>(); 	// liste noeuds qui ont ack
		this.nb_ack = 0;
		
		this.parent = -1;
		
		this.id_leader = -1;
		this.desirability_leader = -1;
		this.leader_direction = -1;
		
		this.potential_leader = -1;
		this.desirability_potential_leader = -1;
		
		this.state = 1;
		
		this.is_electing = false;
		
		this.ack_2_parent = false;
		
		
		this.source_election = -1;
		this.source_ieme_election = -1;
		this.ieme_election = 0;
		this.ieme_election_max = 0;	

}
	
	public Object clone() {
		VKT04Election vkt = null;
		try {
			vkt = (VKT04Election) super.clone();
			
			vkt.neighbors = new ArrayList<Long>(); 		// Liste des voisins
			//vkt.neighbors_ack = new ArrayList<Pair<Long,ArrayList<Long>>>(); 	// liste noeuds qui ont ack
			vkt.neighbors_ack = new ArrayList<Long>(); 
			vkt.parent = -1;
			
			vkt.id_leader = -1;
			vkt.desirability_leader = -1;
			
			vkt.potential_leader = -1;
			vkt.desirability_potential_leader = -1;
			
			vkt.state = 1;
			
			vkt.is_electing = false;
			
			vkt.ack_2_parent = false;
			
			
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
		this.desirability = (int) node.getID();
	}
	
	
	/*****************************Election******************************/	
	
	/*
	private void remove_ack_list(Node host, long id_election) {
		
		for (Pair<Long, ArrayList<Long>> p : neighbors_ack) {
			if (p.getId() == id_election);
				neighbors_ack.remove
		}
	}
	*/
	/**
	 * Partie élection statique, va lancer une nouvelle élection
	 * avec la liste statique des neouds.
	 * 
	 * @param host
	 */
	private void VKT04ElectionTrigger(Node host) {

		// Récupération du protocol de communication
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		// Recuperation du protocol de Neighbor
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol(neighbor_pid);

		// List initialisation
		neighbors = new ArrayList<Long>(np.getNeighbors());
		neighbors_ack = new ArrayList<Long>(np.getNeighbors());

		// etat
		this.state = 1;
		
		// lien de parente
		this.parent = -1;
		
		// leader
		this.id_leader = -1;
		this.desirability_leader = -1;
		
		// valeur
		this.desirability_potential_leader = this.desirability; // ma valeur par defaut
		this.potential_leader = host.getID(); 					// moi meme par defaut
		
		// passer en moode election
		this.is_electing = true;
		
		// j'ai pas repondu a mon pere (evite de repasser dans la liste ack_neigh quand elle est vide)
		this.ack_2_parent = false;
		
		// source de l'electioon
		this.source_election = host.getID();		// je suis le createur de cette election.
		this.ieme_election = this.ieme_election + 1;	// Pour calculer la priorite de mon election.
		this.source_ieme_election = this.ieme_election;	// je suis ma propre source
		
		// ma plus grande election
		this.ieme_election_max = Math.max(this.ieme_election_max, this.ieme_election);
		
		ElectionDynamicMessage edm = new ElectionDynamicMessage(host.getID(), ALL,
				this.my_pid,
				this.potential_leader,
				this.desirability_potential_leader,
				this.source_election,
				this.ieme_election);
		
		emp.emit(host, edm);
		
		// Ajouter de la variance pour ne pas que les noeuds lance tout le temps des élections
		// exactement en même temps.
		EDSimulator.add(periode_leader, leader_event, host, my_pid);
	}
	
	
	/********************************** ELECTION MESSAGE ****************************************/	
	/**
	 * Met à jour les champs nécessaire à l'élection avec les valeurs worthy
	 * 
	 * du nouveau parent.
	 * @param host moi même
	 * @param edm message d'éléction dynamique
	 */
	private void patchAfterElectionMessage(Node host, ElectionDynamicMessage edm) {
		
		// Recuperation du protocol de Neighbor
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol((neighbor_pid));

		// patch le parent *new father*
		this.parent = edm.getIdSrc();
		this.ack_2_parent = false;

		// List initialisation
		this.neighbors = new ArrayList<Long>(np.getNeighbors());
		this.neighbors_ack = new ArrayList<Long>(np.getNeighbors());
		
		// passe en mode election
		this.is_electing = true;

		// valeur potentielles
		this.potential_leader = edm.getMostValuedNode();
		this.desirability_potential_leader = edm.getMostValuedNodeDesirability();
		
		// patch les variables d'election
		this.source_election = edm.getSource_election();
		this.source_ieme_election = edm.getIeme_election();
		
		// ma plus grande election
		this.ieme_election_max = Math.max(this.ieme_election_max, source_ieme_election);
		
		//patch state
		this.state = 1;
	}
	
	/**
	 * @param host moi même
	 * @param edm message d'éléction dynamique
	 * @return Vrai si je suis plus worthy
	 */
	private boolean worthierElection(Node host, ElectionDynamicMessage edm) {
		
		return this.ieme_election_max > edm.getIeme_election()
				|| this.ieme_election_max == edm.getIeme_election()
				&& this.desirability_potential_leader > edm.getMostValuedNodeDesirability();
	}

	
	/**
	 * Le but est de propager le l'éléction provenant du message en paramètre
	 * @param host moi même
	 * @param edm message d'éléction dynamique
	 */
	private void PropagateElection2children(Node host, ElectionDynamicMessage edm) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
    
		// J'ai des fils // > 1 ou != 1?
		if (neighbors_ack.size() > 1) {
			for (Long neinei : neighbors_ack) {
				
				Node dest = Network.get(neinei.intValue());
				
				//System.err.println(host.getID() + " " + parent);
				if(dest.getID() == parent) {continue; } // Skip l'id du pere
	
				ElectionDynamicMessage em_propagation = new ElectionDynamicMessage(host.getID(), dest.getID(),
						this.my_pid,
						edm.getMostValuedNode(),
						edm.getMostValuedNodeDesirability(),
						edm.getSource_election(),
						edm.getIeme_election());
				emp.emit(host, em_propagation);
			}
		} else {
			// je n'ai pas de fils je dois repondre a mon pere.
			AckMessage am = new AckMessage(host.getID(), edm.getIdSrc(),
					this.my_pid,
					this.potential_leader,
					edm.getMostValuedNodeDesirability(),
					edm.getSource_election(),
					edm.getIeme_election());
			emp.emit(host, am);
		}
	}

	private boolean same_election(Node host, ElectionDynamicMessage edm) {
		
		return this.source_election == edm.getSource_election()
				&& this.source_ieme_election == edm.getIeme_election()
				&& this.desirability_potential_leader == edm.getMostValuedNodeDesirability();
	}

	/**
	 * Fonction appelée en cas de réception d'un message d'éléction dynamique
	 * Elle sert a résoudre les conflits si une éléction est déjà en cours
	 * ou si j'ai un parent illégitime de devenir leader.
	 * 
	 * @param host moi même
	 * @param edm message d'éléction dynamique
	 */
	private void recvElectionDynamicMsg(Node host, ElectionDynamicMessage event) {

		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));

		// Ce n'est pas mon propre message et c'est une election qui vaut le coup
		if (host.getID() != event.getIdSrc()){
			
			// Je ne connais pas mon leader
			if (this.state == 1) {
				
				// Je suis en cours d'election
				if (this.is_electing) {
					
					// Ce n'est pas la meme election
					if (!same_election(host, event)) {
						
						// son election vaut plus que la mienne
						if (!worthierElection(host, event)) {
							
							//redemarre sur son election.
							patchAfterElectionMessage(host, event);
							PropagateElection2children(host, event);

						} else {
							// son election est moins bonne que la mienne
							// ignorer
						}
					} else {
						// c'est la meme election, ne rien faire?
						AckMessage am = new AckMessage(host.getID(), event.getIdSrc(),
								this.my_pid,
								this.potential_leader,
								this.desirability_potential_leader,
								this.source_election,
								this.source_ieme_election);
						emp.emit(host, am);
					}
				} else {
					// Je ne suis pas en cours d'election
					// + pas de leader => bug
				}
			} else {
				// demande d'election alors que j'ai un leader?
				// TODO
			}
	}

	/********************************** ACK MESSAGE ****************************************/
	/**
	 * Surcharge de la fonction pour les messages de type AckMessge
	 * @param host
	 * @param ack
	 */
	
	
	private boolean same_election(Node host, AckMessage am) {
		
		return this.source_election == am.getSource_election()
				&& this.source_ieme_election == am.getIeme_election()
				&& this.desirability_potential_leader == am.getMostValuedNodeDesirability();
	}
	
	private void patchAfterAckMessage(Node host, AckMessage am) {

		// mise a jour apres reception d'un ack
		this.potential_leader = am.getMostValuedNode();
		this.desirability_potential_leader = am.getMostValuedNodeDesirability();
	}
	
	
	/**
	 * @param host moi même
	 * @param edm message d'éléction dynamique
	 * @return Vrai si je suis plus worthy
	 */
	private boolean worthierElection(Node host, AckMessage am) {
		
		// l'egalite pour ne pas changer de pere tout le temps, superieur pour trouver le meilleur.
		return this.ieme_election_max > am.getIeme_election()
				|| this.ieme_election_max == am.getIeme_election()
				&& this.desirability_potential_leader > am.getMostValuedNodeDesirability();
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
		
		if (host.getID() != event.getIdSrc()) {
			
			if (same_election(host, event)) {
				

				
				//trick la feuille envoi un message special avec sa valeur dans le mostvaluednode
				// je n'ai pas trouve de maniere propre de la faire... faire une variable dans la
				// class ackMessage /!\
				if (event.getMostValuedNode() > this.desirability_potential_leader) {
					this.desirability_potential_leader = event.getMostValuedNode();
					this.potential_leader = event.getMostValuedNode();
				}
				this.neighbors_ack.remove(am.getIdSrc()); // remove is empty safe.
				
				if (host.getID() == 297 ) {
					System.err.println("same_elec i'm :" + host.getID()
					+ " parent " + parent
					+ " PL " + potential_leader
					+ " source " + source_election
					+ " ackmessagevalue " + event.getMostValuedNode()
					+ " from " + event.getIdSrc()
					+ " size " + neighbors_ack);
				}
				
			} else {
				
				// J'ai une moins bonne valeur
				if (!worthierElection(host, am)) {
					// sa valeur est mieux
					patchAfterAckMessage(host, am);

				} else {
					// ma valeur est nieux on ne fait rien
				}
			}
			

			if (this.neighbors_ack.size() == 1 // Je suis une feuille ou il n'y avait qu'un pere à attendre
					&& this.potential_leader != host.getID() // il essaye de s'auto elire (voir 300 noeud)
					|| this.neighbors_ack.isEmpty() // le pere est une feuille
					&& !ack_2_parent) {
        
				// J'ai repondu a mon pere (tres important sinon le nombre
				// de messages explose
				ack_2_parent = true;
				
				// reponse a mon pere si j'en ai un (0 inclu)
				if (this.parent >= 0) {
					//System.err.println("p>=0 i'm :" + host.getID() + " parent " + parent + " PL " + potential_leader + " source " + source_election);
					// Envoie d'un ack à mon père, je suis une feuille
					AckMessage am_to_father = new AckMessage(host.getID(), parent,
							this.my_pid,
							this.potential_leader,
							this.desirability_potential_leader,
							this.source_election,
							this.source_ieme_election);
					emp.emit(host, am_to_father);
					
				} else {
					// on sort de l'election
					this.is_electing = false;

					// Je propose l'election de mon potentiel
					this.id_leader = this.potential_leader;
					this.desirability_leader = this.desirability_potential_leader;
					this.state = (id_leader == host.getID())? 2 : 0;			// auto election?
	
					// Broadcast du message de leader
					LeaderMessage lm_broadcast = new LeaderMessage(host.getID(), ALL,
							this.my_pid,
							this.id_leader,
							this.desirability_leader,
							this.source_election,
							this.ieme_election);
					emp.emit(host, lm_broadcast);
				}
			}
		}
	}



	/********************************** LEADER MESSAGE ****************************************/
	
	private boolean same_election(Node host, LeaderMessage lm) {
		
		return this.source_election == lm.getSource_election()
				&& this.source_ieme_election == lm.getIeme_election()
				&& this.desirability_potential_leader == lm.getMostValuedNodeDesirability();
	}
	
	/**
	 * @param host
	 * @param event
	 */
	private void patchLeader(Node host, LeaderMessage lm) {
	
		// patch des variables leader potentiel
		this.potential_leader = lm.getMostValuedNode();
		this.desirability_potential_leader = lm.getMostValuedNodeDesirability();
		
		// Patch des variables leader
		this.id_leader = lm.getMostValuedNode();
		this.ieme_election_max = lm.getIeme_election();
		this.desirability_leader = lm.getMostValuedNodeDesirability();
		
		// etat
		this.state = lm.getMostValuedNode() == host.getID() ? 2 : 0;
	}
	

	/**
	 * @param host
	 * @param event
	 * @return
	 */
	private boolean worthierLeader(Node host, LeaderMessage event){

			return this.desirability_potential_leader >= event.getMostValuedNodeDesirability();		
	}

	
	/**
	 * Le but est de propager le l'éléction provenant du message en paramètre
	 * @param host moi même
	 * @param edm message d'éléction dynamique
	 */
	private void PropagateLeader(Node host, LeaderMessage lm) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		// Recuperation du protocol de Neighbor
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol(neighbor_pid);
		

		for (Long neinei : np.getNeighbors()) {
			
			Node dest = Network.get(neinei.intValue());
			
			if (dest.getID() == lm.getIdSrc()) { continue; }
			
			LeaderMessage em_propagation = new LeaderMessage(host.getID(), dest.getID(),
					this.my_pid,
					lm.getMostValuedNode(),
					lm.getMostValuedNodeDesirability(),
					lm.getSource_election(),
					lm.getIeme_election());
			
			emp.emit(host, em_propagation);
		}
	}
	
	/**
	 * @param host
	 * @param event
	 */
	private void mergeLeader(Node host, LeaderMessage event) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		

		if (worthierLeader(host, event)) {
			
			// propager  mon leader
			LeaderMessage lm_local = new LeaderMessage(host.getID(), event.getIdSrc(), 
					this.my_pid,
					this.id_leader,
					this.desirability_leader,
					this.source_election,
					this.source_ieme_election);
			emp.emit(host, lm_local);	
		}
		else {
			PropagateLeader(host, event);
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
		
		if (event.getIdSrc() != host.getID()
				&& id_leader != event.getMostValuedNode()) { // deja patch
			
			 // 1 : leader_unknown
			if (this.state == 1) {
				patchLeader(host, lm);
				PropagateLeader(host, lm);
				
			} else {
				//mergeLeader(host, lm);
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

	
	/*****************************NEIGHBORHOOD Listener******************************/

	/***********************New Neighbor Detected ******************************/
	
	/**
	 *
	 */
	@Override
	public void newNeighborDetected(Node host, long id_new_neighbor) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));

		if (state == 0 || state == 2) {
			// Echange de leader
			LeaderMessage lm_cible = new LeaderMessage(host.getID(), id_new_neighbor,
					this.my_pid,
					this.id_leader,
					this.desirability_leader,
					this.source_election,
					this.source_ieme_election);
			emp.emit(host, lm_cible);
		} else {
			// Propagation d'election
			ElectionDynamicMessage em_propagation = new ElectionDynamicMessage(host.getID(), id_new_neighbor,
					this.my_pid,
					this.potential_leader,
					this.desirability_potential_leader,
					this.source_election,
					this.source_ieme_election);
			emp.emit(host, em_propagation);
		}
	}

	/***********************Lost Neighbor Detected ******************************/
	
	private void recvLostLeaderDirectionMsg(Node host, LostLeaderDirectionMessage event) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));

		// Recuperation du protocol de Neighbor
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol((neighbor_pid));	
		
		
		if (event.getIdSrc() != host.getID()) {
				
				if (this.state == 1) { // 1 : leader_unknown
					// discard?
					
				} else {
					if (this.leader_direction == event.getLeader_direction()
							&& !np.getNeighbors().contains(this.leader_direction)) {
							// je l'ai perdu aussi?
					}
				}
			}
		}
	
	
	/**
	 *
	 */
	@Override
	public void lostNeighborDetected(Node host, long id_lost_neighbor) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));

		// Recuperation du protocol de Neighbor
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol(neighbor_pid);
		
		//System.err.println(np.getNeighbors());
		
		
		if (this.state == 2 || this.state == 0) {
			if (this.leader_direction == id_lost_neighbor) {
				
			}
		}
		
		
		
		/*
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));

		// Recuperation du protocol de Neighbor
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol((neighbor_pid));

		// leader connu
		if (this.state == 2 || this.state == 0) {
			if (this.leader_direction == id_lost_neighbor) {
				System.err.println("reparation...");
				LeaderMessage lm = new LeaderMessage(host.getID(), ALL, 
						this.my_pid,
						id_lost_neighbor,
						-2,
						parent,
						-1); // TODO
			if (
					//this.leader_direction == id_lost_neighbor ||
					id_lost_neighbor == this.id_leader) {
				
				
				if (host.getID() == 8 || host.getID() == 1) {
					System.err.println("HOSTID : " + host.getID() +":"+id_lost_neighbor 
					+ " my_source : " + this.source_election
					+ " my_PL : " + this.desirability_potential_leader
					+ " my_ieme : " + this.ieme_election_max
					//+ " my_lead_dir : " + this.leader_direction
					+ " id_leader : " + this.id_leader
							);
				}
				ElectionDynamicMessage edm = new ElectionDynamicMessage(host.getID(), ALL,
						this.my_pid,
						this.potential_leader,
						this.desirability_potential_leader,
						host.getID(),
						this.ieme_election);
				
				// le dernier est parti je suis seul
				//if (np.getNeighbors().size() <= 2) {
					patchAfterElectionMessage(host, edm);
				//}
				emp.emit(host, edm);
				}
			}	
		}
		*/
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
			election_dynamic_message++;
			recvElectionDynamicMsg(host, (ElectionDynamicMessage) event);
			return;
		}
		
		// Gestion de la réception d'un message de type LeaderMessage
		if (event instanceof LeaderMessage) {
			leader_message++;
			recvLeaderlMsg(host, (LeaderMessage) event);
			return;
		}

		// Gestion de la réception d'un message de type AckMessage
		if (event instanceof AckMessage) {
			ack_message++;
			recvAckMsg(host, (AckMessage) event);
			return;
		}
		
		// Evènement périodique d'élections.		
		if (event instanceof String) {
			String ev = (String) event;

			if (ev.equals(leader_event)) {
				
				if (first) {
					VKT04ElectionTrigger(host);
				} else {
					first = true;
					EDSimulator.add(periode_leader, leader_event, host, my_pid);
				}
				/*System.err.println("EDM : " + election_dynamic_message + "\n"
						+ "LM : " + leader_message + "\n"
						+ "AM : " + ack_message + "\n");
						*/
				return;
			}
		}
		throw new RuntimeException("Receive unknown Event");
	}

}
