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
	private int desirability; 						// desirabilit� du noeud									(-1 si inconnu)
	private long parent; 							// permet de conna�tre son p�re et remonter dans l'arbre 	(-1 si inconnu)
	private long id_leader;							// id du leader actuel, -1 si aucun leader.					(-1 si inconnu)
	private long desirability_leader;				// desirabilit� du noeud leader								(-1 si inconnu)
	private long potential_leader;					// id du leader potentiel, -1 si aucun leader.				(-1 si inconnu)
	private long desirability_potential_leader;		// d�sirabilit� du leader potentiel, -1 si aucun leader.	(-1 si inconnu)
	
	// new variables for dynamic protocol
	private boolean is_electing;		// Variable qui dit si ce noeud est en train de faire une �l�ction.			(0 si inconnu)
	private boolean ack_2_parent;		// Variable qui dit si ce noeud a envoy� son ack � son p�re.				(false si inconnu)
	private long source_election;		// Noeud d'o� provient l'�lection dans laquelle je suis.					(-1 si inconnu)
	private long ieme_election;			// indique pour ce node la ieme election qu'il lance.						(0 si inconnu)
										// utile pour differencier les elections et choisir parmi elles.			
										// Plus un node lance d'election plus il a de chance d'en lancer.
										// Soit i,j Node� : (i.ieme_election(), i.getID()) > (j.ieme_election(), j.getID())
										// <=> i.ieme_election() > j.ieme_election() ||
										// (i.ieme_election() == j.ieme_election()) &&  (i.getID() >  j.getID())
	
	private long source_ieme_election; // ieme election de la source a laquelle je participe
	
	private long ieme_election_max;	// La plus grande election � laquelle j'ai particip�.						(0 si inconnu)
	
	private int state;								// 0 : leader_known
													// 1 : leader_unknown
													// 2 : leader_isMe
	
public VKT04Election(String prefix) {
		
		String tmp[] = prefix.split("\\.");
		this.my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		
		this.periode_leader = Configuration.getInt(prefix + "." + PAR_PERIODE_LEADER);
		
		// Creation de liste privees.
		this.neighbors = new ArrayList<Long>(); 		// Liste des voisins
		this.neighbors_ack = new ArrayList<Long>(); 	// liste noeuds qui ont ack
		this.parent = -1;
		this.id_leader = -1;
		this.desirability_leader = -1;
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
			vkt.neighbors_ack = new ArrayList<Long>(); 	// liste noeuds qui ont ack
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
	 * Fonction utilis� par la classe d'initialisation qui est appel�e
	 * en d�but de programme pour tous les noeuds.
	 * Elle a pour but d'initialis� la d�sirability du node avec son ID en param�tre.
	 * 
	 * @param node le node en lui m�me
	 */
	public void initialisation(Node node) {
		this.desirability = node.getIndex();
	}
	
	
	/*****************************Election******************************/	
	
	/**
	 * Partie �lection statique, va lancer une nouvelle �lection
	 * avec la liste statique des neouds.
	 * 
	 * @param host
	 */
	private void VKT04ElectionTrigger(Node host) {

		// R�cup�ration du protocol de communication
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		// Recuperation du protocol de Neighbor
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol((neighbor_pid));
		
		
		// D�but d'une demande d'�l�ction globale, mise � jour du node
		// pour d�buter une �l�ction
		this.state = 1;
		this.parent = -1;
		this.id_leader = -1;
		this.desirability_leader = -1;
		this.desirability_potential_leader = desirability;
		this.potential_leader = host.getID();
		
		this.is_electing = true;					// je suis passe en mode election.
		this.ack_2_parent = false;					// je n'ai pas besoin d'attendre un ack.
		this.source_election = host.getID();		// je suis le createur de cette election.
		this.ieme_election = this.ieme_election + 1;	// Pour calculer la priorite de mon election.
		this.source_ieme_election = this.ieme_election;	// je suis ma propre source
		this.ieme_election_max = Math.max(this.ieme_election_max, this.ieme_election);
		
		ElectionDynamicMessage edm = new ElectionDynamicMessage(host.getID(), ALL, host.getID(),
				this.potential_leader,
				this.desirability_potential_leader,
				this.ieme_election,
				this.my_pid);
		emp.emit(host, edm);
		this.neighbors = new ArrayList<Long>(np.getNeighbors());
		this.neighbors_ack = new ArrayList<Long>(np.getNeighbors());
		// Ajouter de la variance pour ne pas que les noeuds lance tout le temps des �lections
		// exactement en m�me temps.
		EDSimulator.add(periode_leader, leader_event, host, my_pid);
	}
	
	
	/********************************** ELECTION MESSAGE ****************************************/	
	/**
	 * Met � jour les champs n�cessaire � l'�lection avec les valeurs worthy
	 * 
	 * du nouveau parent.
	 * @param host moi m�me
	 * @param edm message d'�l�ction dynamique
	 */
	private void patchAfterElectionMessage(Node host, ElectionDynamicMessage edm) {
		
		// Recuperation du protocol de Neighbor
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol((neighbor_pid));
				
		
		// passe en mode election
		this.is_electing = true;
		
		// patch le parent
		this.parent = edm.getIdSrc();
		this.neighbors_ack = new ArrayList<Long>(np.getNeighbors());
		neighbors_ack.remove(parent);
		
		// patch les variables d'election
		this.source_election = edm.getSource_election();
		this.source_ieme_election = edm.getIeme_election();
		this.ieme_election_max = source_ieme_election;
		
		//patch state
		this.state = 1;
	}
	
	/**
	 * @param host moi m�me
	 * @param edm message d'�l�ction dynamique
	 * @return Vrai si je suis plus worthy
	 */
	private boolean worthierElection(Node host, ElectionDynamicMessage edm) {
		
		return this.source_ieme_election > edm.getIeme_election()
				|| this.source_ieme_election == edm.getIeme_election() 
				&& this.desirability_potential_leader > edm.getMostValuedNodeDesirability();
	}

	
	/**
	 * Le but est de propager le l'�l�ction provenant du message en param�tre
	 * @param host moi m�me
	 * @param edm message d'�l�ction dynamique
	 */
	private void PropagateElection2children(Node host, ElectionDynamicMessage edm) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		for (Long neinei : neighbors_ack) {
			Node dest = Network.get(neinei.intValue());
			if(dest.getID() == parent) { continue; } // Skip l'id du pere
			ElectionDynamicMessage em_propagation = new ElectionDynamicMessage(host.getID(), dest.getID(),
					edm.getMostValuedNode(),
					edm.getMostValuedNodeDesirability(),
					edm.getSource_election(),
					edm.getIeme_election(),
					this.my_pid);
			emp.emit(host, em_propagation);
		}
	}


	/**
	 * Fonction appel�e en cas de r�ception d'un message d'�l�ction dynamique
	 * Elle sert a r�soudre les conflits si une �l�ction est d�j� en cours
	 * ou si j'ai un parent ill�gitime de devenir leader.
	 * 
	 * @param host moi m�me
	 * @param edm message d'�l�ction dynamique
	 */
	private void recvElectionDynamicMsg(Node host, ElectionDynamicMessage event) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		ElectionDynamicMessage edm = (ElectionDynamicMessage)event;
		
		// Ce n'est pas mon propre message et c'est une election qui vaut le coup
		if (host.getID() != event.getIdSrc() && event.getIeme_election() >= ieme_election_max) {
			
			if (!worthierElection(host, event)) {
	
				// Il devient mon pere
				patchAfterElectionMessage(host, edm);
			
				// Propagation aux fils
				 PropagateElection2children(host, edm);
			} else {
				
				// J'ai d�j� un parent legitime, r�ponse immediate de la valeur potentielle
				AckMessage am = new AckMessage(host.getID(), edm.getIdSrc(), 
						this.my_pid,
						this.potential_leader,
						this.desirability_potential_leader,
						this.source_election,
						this.source_ieme_election);
				emp.emit(host, am);
			}
		}
	}

	/********************************** ACK MESSAGE ****************************************/
	/**
	 * Surcharge de la fonction pour les messages de type AckMessge
	 * @param host
	 * @param ack
	 */
	private void patchAfterAckMessage(Node host, AckMessage am) {
		
		//int neighbor_pid = Configuration.lookupPid("neighbor");
		//NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol(neighbor_pid);
		
		// passe en mode election
		//this.is_electing = true;
		
		// patch le parent
		//this.parent = am.getIdSrc();						// Ce noeud est mon p�re
		
		//patch les listes (remise a neutre)
		//neighbors = new ArrayList<Long>(np.getNeighbors());
		//neighbors_ack = new ArrayList<Long>(np.getNeighbors());
		//neighbors_ack.remove(am.getIdSrc());
		
		// patch les variables d'election
		//this.source_election = am.getSource_election(); 	// Je met a jour a source de l'election
		//this.source_ieme_election = am.getIeme_election(); 	// Je met a jour le ieme de la source de l'election
		//this.ieme_election_max = this.source_ieme_election;
		
		// patch des variables leader potentiel 
		this.potential_leader = am.getMostValuedNode();
		this.desirability_potential_leader = am.getMostValuedNodeDesirability();
		
		// patch etat
		//this.state = 1;
	}
	
	
	
	/**
	 * @param host moi m�me
	 * @param edm message d'�l�ction dynamique
	 * @return Vrai si je suis plus worthy
	 */
	private boolean worthierElection(Node host, AckMessage am) {
		
		return source_ieme_election > am.getIeme_election()
				|| source_ieme_election == am.getIeme_election() 
				&& desirability_potential_leader > am.getMostValuedNodeDesirability();
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
			
			// Mise a jour de mon noeud leader si le potentiel leader que j'ai est moins d�sirable.
			if (!worthierElection(host, am)) {
				patchAfterAckMessage(host, am);
			}
			
			// J'ai re�u un ack de ce node c'est bon, remove safe d'un node DANS (il y est) ma liste !
			this.neighbors_ack.remove(am.getIdSrc()); // remove is empty safe.
			
			// Je suis une feuille ou il n'y avait qu'un fils � attendre
			if (this.neighbors_ack.isEmpty() && !ack_2_parent) {
				
				// J'ai repondu a mon pere (tres important sinon le nombre
				// de messages explose
				ack_2_parent = true;
				
				// reponse a mon pere si j'en ai un
				if (this.parent >= 0) {
					
					// Envoie d'un ack � mon p�re, je suis une feuille
					AckMessage am_to_father = new AckMessage(host.getID(), 
							this.parent,
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
	/**
	 * @param host
	 * @param event
	 */
	private void patchLeader(Node host, LeaderMessage lm) {
		
		// passe en mode election
		this.is_electing = false;
		
		// patch le parent
		this.parent = -1;
		
		//patch les listes
		
		// patch les variables d'election
		this.source_election = lm.getSource_election(); 	// Je met a jour a source de l'election
		this.source_ieme_election = lm.getIeme_election(); 	// Je met a jour le ieme de la source de l'election
		this.ieme_election_max = lm.getIeme_election();
		
		// patch des variables leader potentiel
		this.potential_leader = lm.getMostValuedNode();
		this.desirability_potential_leader = lm.getMostValuedNodeDesirability();
		
		// Patch des variables leader
		this.id_leader = lm.getMostValuedNode();
		this.ieme_election_max = lm.getIeme_election();
		this.desirability_leader = lm.getMostValuedNodeDesirability();
		
		// etat
		this.state = lm.getMostValuedNode() == host.getID() ? 2 : 0; // 
	}
	

	/**
	 * @param host
	 * @param event
	 * @return
	 */
	private boolean worthierLeader(Node host, LeaderMessage event){
	
		
		//return this.desirability_potential_leader > event.getSource_election();
		return this.source_ieme_election > event.getIeme_election()
				|| this.source_ieme_election == event.getIeme_election() 
				&& this.desirability_potential_leader > event.getMostValuedNodeDesirability();
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
					this.my_pid, id_leader,
					this.desirability_leader,
					this.source_election,
					this.source_ieme_election);
			emp.emit(host, lm_local);
			
			// mise a jour // TODO au cas ou?
			patchLeader(host, lm_local);
			
		} else {
			// Si il est illegitime alors je dois le modifier propager le nouveau leader
			// me mettre a jour et propager.
			patchLeader(host, event);
			LeaderMessage lm_broadcast = new LeaderMessage(host.getID(), ALL, 
					this.my_pid,
					event.getMostValuedNode(),
					event.getMostValuedNodeDesirability(),
					event.getSource_election(),
					event.getIeme_election());
			emp.emit(host, lm_broadcast);
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
			patchLeader(host, lm);			
			LeaderMessage lm_propagate = new LeaderMessage(host.getID(), ALL, my_pid, id_leader, desirability_leader, -1, -1); // TODO
			emp.emit(host, lm_propagate);
		} else {
			mergeLeader(host, lm);
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
		
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol(neighbor_pid);
		
		neighbors = new ArrayList<Long>(np.getNeighbors());
		
		switch (state) {
		case 0:							// 0 : leader_known
		case 2: 						// 2 : leader_isMe
			// Echange de leader
			// Je rediffuse pour qu'il s'insere avec nous ou pas
			// Deux cas :
			// 1) je suis un nouveau parmi un groupe => beaucoup d'envoie
			// 2) Je suis un groupe et j'accueil un nouveau => peu d'envoie, beaucoup de receptions (tester l'egalite pour ne pas flicker)
			LeaderMessage lm_cible = new LeaderMessage(host.getID(), id_new_neighbor,
					this.my_pid,
					this.id_leader,
					this.desirability_leader,
					this.source_election,
					this.source_ieme_election);
			emp.emit(host, lm_cible);
			break;
		case 1:							// 1 : leader_unknown
			if (is_electing) {
				// phase de diffusion du ElectionMessage [en cours]
				// 1) je suis un nouveau parmi un groupe => beaucoup d'envoie
				// 2) Je suis un groupe et j'accueil un nouveau => peu d'envoie, beaucoup de receptions (tester l'egalite pour ne pas flicker)
				ElectionDynamicMessage em_propagation = new ElectionDynamicMessage(host.getID(), id_new_neighbor,
						this.potential_leader,
						this.desirability_potential_leader,
						this.source_election,
						this.source_ieme_election,
						this.my_pid);
				emp.emit(host, em_propagation);
			}
			//else {
				// Je trigger une election TODO?
				// 1) Tentative d'election dans un nouveau groupe
				// 2) un nouveau arrive il declenche une election, est-ce normal?
				// ca spam le reseau, attend la prochaine election
				//VKT04ElectionTrigger(host);
			//}
			break;
		default:
			break;
		}
	}

	/***********************Lost Neighbor Detected ******************************/
	/**
	 * @param host
	 * @param id_lost_neighbor
	 */
	/*
	private void patchNeighbors(Node host, long id_lost_neighbor) {
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol(neighbor_pid);
		
		//neighbors = np.getNeighbors();
		if (neighbors_ack.contains(id_lost_neighbor)) {
			//neighbors_ack.remove(id_lost_neighbor);
		}
	}
	*/
	

	/**
	 *
	 */
	@Override
	public void lostNeighborDetected(Node host, long id_lost_neighbor) {
		/*
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
	
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol(neighbor_pid);
		
		switch (this.state) {
		case 0:							// 0 : leader_known
			if (this.id_leader == id_lost_neighbor) {
				
				//leader is dead
				VKT04ElectionTrigger(host);
				
			} else {
				// don't care just patch the lists
				//neighbors = np.getNeighbors();
			}

			break;
		case 1:							// 1 : leader_unknown
			if (this.is_electing) {
				// On vient de perdre le mec qu'on voulait elire
				if (id_lost_neighbor == this.potential_leader) {
					VKT04ElectionTrigger(host);
					
					if (id_lost_neighbor == this.parent) {
						//this.parent = -1; // j'ai perdu mon parent je suis la racine de l'election
					}

				// On a [pas] perdu le mec qu'on voulait elire alors
				// il suffit de le nommer leader
				} else {
					
					// je suis la racine de cette election => attente que neighbor_ack fini
					// j'ai suffisamment d'info?
					if (id_lost_neighbor == parent) {
						//LeaderMessage lm = new LeaderMessage(host.getID(), ALL, my_pid, potential_leader, desirability_potential_leader, host.getID(), ieme_election);
						//patchLeader(host, lm);
						//emp.emit(host, lm);
					} else {
						
						// neighbor_ack -1 mais ajoute
					}
				}
			} else {
				// techniquement pas possible
			}
			break;
		case 2: 						// 2 : leader_isMe
			// don't care?
		default:
			break;
		}
		*/
	}

	
	
	/********************************MONITORABLE**********************************/
	/* MONITORABLE : impl�mente l'interface Monitorable pour afficher sur le moniteur 
	 * graphique l'�tat de chaque noeud : on peut diff�rencier dans 
	 * cet algorithme trois �tats : 
	 * 
	 * * leader inconnu,
	 * * leader connu, 
	 * * �tre le leader.
	 */
	
	/* permet d'obtenir le nombre d'�tat applicatif du noeud */
	public int nbState() {
		return 3;
	}

	/* permet d'obtenir l'�tat courant du noeud */
	@Override
	public  int getState(Node host) {
		return state;
	}

	/*
	 * permet d'obtenir une liste de chaine de caract�re, affichable en colonne �
	 * cot� du noeud sur un moniteur graphique
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
		
		// Gestion de la r�ception d'un message de type ElectionMessage
		if (event instanceof ElectionDynamicMessage) {
			recvElectionDynamicMsg(host, (ElectionDynamicMessage) event);
			return;
		}
		
		// Gestion de la r�ception d'un message de type LeaderMessage
		if (event instanceof LeaderMessage) {
			recvLeaderlMsg(host, (LeaderMessage) event);
			return;
		}

		// Gestion de la r�ception d'un message de type AckMessage
		if (event instanceof AckMessage) {
			recvAckMsg(host, (AckMessage) event);
			return;
		}
		
		// Ev�nement p�riodique d'�lections.		
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
