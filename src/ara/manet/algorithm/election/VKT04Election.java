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

	private List<Long> neighbors;
	private List<Long> neighbors_ack;				// permet de compter le nombre de ack				
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
	private void VKT04ElectionTrigger(Node host) {

		// Récupération du protocol de communication
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		// Début d'une demande d'éléction globale, mise à jour du node
		// pour débuter une éléction
		
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
		
		// Il faut rearmer la liste des neighbors?
		this.neighbors_ack = new ArrayList<Long>(np.getNeighbors());
		
		// patch le parent
		this.parent = edm.getIdSrc();
		this.ack_2_parent = false;
		
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
		
		// propagation a tous les noeuds du neighbor sauf le pere.
		for (Long neinei : neighbors) {
			
			Node dest = Network.get(neinei.intValue());
			if(dest.getID() == parent) { continue; } // Skip l'id du pere
			
			ElectionDynamicMessage em_propagation = new ElectionDynamicMessage(host.getID(), dest.getID(),
					this.my_pid,
					edm.getMostValuedNode(),
					edm.getMostValuedNodeDesirability(),
					edm.getSource_election(),
					edm.getIeme_election());
			
			emp.emit(host, em_propagation);
		}
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

		if (this.parent == -1) {
			// Ce n'est pas mon propre message et c'est une election qui vaut le coup
			if (host.getID() != event.getIdSrc()) {
				
				// Cette election que j'ai recu est meilleur que la mienne dis donc!
				if (!worthierElection(host, event)) {
	
					// Il devient mon pere, reconfiguration pour reorienter l'arbre
					patchAfterElectionMessage(host, event);
					
					// Propagation aux fils
					PropagateElection2children(host, event);
				} else {
					// J'ai déjà un parent legitime, réponse immediate de la valeur potentielle
					AckMessage am = new AckMessage(host.getID(), event.getIdSrc(), 
							this.my_pid,
							this.potential_leader,
							this.desirability_potential_leader,
							this.source_election,
							this.source_ieme_election);
					emp.emit(host, am);
				}
			}
		} else {
			AckMessage am = new AckMessage(host.getID(), event.getIdSrc(), 
					this.my_pid,
					this.potential_leader,
					this.desirability_potential_leader,
					this.source_election,
					this.source_ieme_election);
			emp.emit(host, am);
		}
	}

	/********************************** ACK MESSAGE ****************************************/
	/**
	 * Surcharge de la fonction pour les messages de type AckMessge
	 * @param host
	 * @param ack
	 */
	private void patchAfterAckMessage(Node host, AckMessage am) {

		// valeur recuperer si elle est meilleur! ET C'EST TOUT! 
		this.potential_leader = am.getMostValuedNode();
		this.desirability_potential_leader = am.getMostValuedNodeDesirability();
		
		// Fusionne avec nous, on t'adopte
		/*
		if (this.parent == -1 && am.getMostValuedNode() != potential_leader) {
			this.parent = am.getIdSrc();
			
			// patch les variables d'election
			this.source_election = am.getSource_election();
			this.source_ieme_election = am.getIeme_election();
			
			// ma plus grande election
			this.ieme_election_max = Math.max(this.ieme_election_max, source_ieme_election);
		}*/
	}
	
	
	
	/**
	 * @param host moi même
	 * @param edm message d'éléction dynamique
	 * @return Vrai si je suis plus worthy
	 */
	private boolean worthierElection(Node host, AckMessage am) {
		
		return this.desirability_potential_leader > am.getMostValuedNodeDesirability();
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
			
			// Mise a jour de mon noeud leader si le potentiel leader que j'ai est moins désirable.
			if (!worthierElection(host, am)) {
				patchAfterAckMessage(host, am);
			}
			
			// J'ai reçu un ack de ce node c'est bon, remove safe d'un node DANS (il y est) ma liste !
			this.neighbors_ack.remove(am.getIdSrc()); // remove is empty safe.
			
			// Je suis une feuille ou il n'y avait qu'un fils à attendre
			if (host.getID() == 13 || host.getID() == 29) {
				System.err.println("Je suis : " + host.getID()
				+ " mon pere est : " + this.parent
				+ " leader est :" + this.potential_leader
				+ " empty : " + this.neighbors_ack.isEmpty()
				+ " ack2p : " + this.ack_2_parent);
				
				System.err.println(neighbors_ack);
			}
			
			if (this.neighbors_ack.isEmpty() && !this.ack_2_parent) {
				
				// J'ai repondu a mon pere (tres important sinon le nombre de messages explose)
				this.ack_2_parent = true;
				if (host.getID() == 13 || host.getID() == 29) {
					System.err.println("Je suis PASSE : " + host.getID()
					+ " mon pere est : " + this.parent
					+ " leader est :" + this.potential_leader);
				}
				/*
				System.err.println("Je suis : " + host.getID()
				+ " mon pere est : " + this.parent
				+ " leader est :" + this.potential_leader);

				// on sort de l'election, je vais annoncer le leader ou repondre a mon pere
				 */
				this.is_electing = false;

				if (this.parent != -1) {
					
					// Envoie d'un ack à mon père, je suis une feuille
					AckMessage am_to_father = new AckMessage(host.getID(), this.parent,
							this.my_pid,
							this.potential_leader,
							this.desirability_potential_leader,
							this.source_election,
							this.source_ieme_election);
					emp.emit(host, am_to_father);
					
				} else {
					
					// Je suis la racine j'ai les infos necessaires !
					this.id_leader = this.potential_leader;
					this.desirability_leader = this.desirability_potential_leader;
					this.state = (this.id_leader == host.getID())? 2 : 0;			// auto election?
					
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
		
		// etat
		this.state = (lm.getMostValuedNode()  == host.getID()) ? 2 : 0; // 
		
		// passe en mode election
		this.is_electing = false;
		
		// patch les variables d'election
		//this.source_election = lm.getSource_election(); 	
		//this.source_ieme_election = lm.getIeme_election();
		//this.ieme_election_max = lm.getIeme_election();
		
		// patch des variables leader potentiel
		this.potential_leader = lm.getMostValuedNode();
		this.desirability_potential_leader = lm.getMostValuedNodeDesirability();
		
		// Patch des variables leader
		this.id_leader = lm.getMostValuedNode();
		this.desirability_leader = lm.getMostValuedNodeDesirability();
	}
	

	/**
	 * @param host
	 * @param event
	 * @return
	 */
	private boolean worthierLeader(Node host, LeaderMessage event){
	
		return this.ieme_election_max > event.getIeme_election()
				|| this.ieme_election_max == event.getIeme_election() 
				&& this.desirability_potential_leader > event.getMostValuedNodeDesirability();
	}

	
	/**
	 * @param host
	 * @param event
	 */
	private void mergeLeader(Node host, LeaderMessage event) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));	
		
		// le leader message que j'ai recu est meilleur
		LeaderMessage lm_propagate = new LeaderMessage(host.getID(), event.getIdSrc(), 
				this.my_pid,
				event.getMostValuedNode(),
				event.getMostValuedNodeDesirability(),
				event.getSource_election(),
				event.getIeme_election());
		
		// Je suis un meilleur leader
		if (worthierLeader(host, event)) {
					lm_propagate = new LeaderMessage(host.getID(), event.getIdSrc(), 
					this.my_pid,
					this.id_leader,
					this.desirability_leader,
					this.source_election,
					this.source_ieme_election);
		} 
		
		// patch et envoie un des deux messages (factorisation est-elle fonctionnelle?) // TODOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO
		patchLeader(host, lm_propagate);
		emp.emit(host, lm_propagate);
	}
	
	/**
	 * Methode de receptioon d'un message de leader qui realise verif + merge
	 * 
	 * @param host
	 * @param event
	 */
	private void recvLeaderlMsg(Node host, LeaderMessage event) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		if (event.getIdSrc() != host.getID()					// ce n'est pas mon propre message (infini)
				&& id_leader != event.getMostValuedNode()) {	// ce n'est pas mon leader (infini)
			
			// 1 : leader_unknown 
			if (state == 1 ) { 			
				patchLeader(host, event);			
				LeaderMessage lm_propagate = new LeaderMessage(host.getID(), ALL, 
						this.my_pid,
						this.id_leader,
						this.desirability_leader,
						event.getSource_election(),
						event.getIeme_election()); // TODO
				emp.emit(host, lm_propagate);
			} else {
				
					// 0:2 : leader connu
					mergeLeader(host, event);
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
		/*
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));

		switch (state) {
		case 0:							// 0 : leader_known
		case 2: 						// 2 : leader_isMe
			// Echange de leader
			// Je rediffuse pour qu'il s'insere avec nous ou pas
			// Deux cas :
			// 1) je suis un nouveau parmi un groupe => beaucoup d'envoie
			// 2) Je suis un groupe et j'accueil un nouveau => peu d'envoie, beaucoup de receptions (tester l'egalite pour ne pas flicker)
			LeaderMessage lm_cible = new LeaderMessage(host.getID(), ALL,
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
				ElectionDynamicMessage em_propagation = new ElectionDynamicMessage(host.getID(), ALL,
						this.my_pid,
						this.potential_leader,
						this.desirability_potential_leader,
						this.source_election,
						this.source_ieme_election);
				
				emp.emit(host, em_propagation);
			}
			break;
		default:
			break;
		}
		*/
	}

	/***********************Lost Neighbor Detected ******************************/
	/**
	 * @param host
	 * @param id_lost_neighbor
	 */
	
	private void patchNeighbors(Node host, long id_lost_neighbor) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));

		// Recuperation du protocol de Neighbor
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol((neighbor_pid));
		

		if (id_lost_neighbor != host.getID()) {
			
			if (state == 1) {			// leader inconnu
				// perte pendant une election non finie
				if (is_electing) {
					
					// la perte est la personne qui devait etre elue
					// la perte de la personne qui avait le lien vers la personne qui devait etre elu?

					if (id_lost_neighbor == parent) {
						/*
						System.err.println("LPDE : " + host.getID() 
						+ " id_lost: " + id_lost_neighbor 
						+ " parent: " + parent
						+ " size compo: " + np.getNeighbors().size());
						*/
						ElectionDynamicMessage edm = new ElectionDynamicMessage(host.getID(), ALL,
								this.my_pid,
								host.getID(),
								this.desirability,
								host.getID(),
								this.ieme_election);
						
						// le dernier est parti je suis seul
						if (np.getNeighbors().size() <= 2) {
							patchAfterElectionMessage(host, edm);
						}
						emp.emit(host, edm);
					}
				}
				
			} else {					// leader connu
				
				// on perd le lien en direction du leader
				if (parent == id_lost_neighbor 
						|| id_leader == id_lost_neighbor) {
					/*
					System.err.println("LDLLP : " + host.getID() 
					+ " id_lost: " + id_lost_neighbor 
					+ " parent: " + parent
					+ " size compo: " + np.getNeighbors().size());
					*/
					LeaderMessage lm = new LeaderMessage(host.getID(), ALL, 
							this.my_pid,
							host.getID(),
							this.desirability,
							host.getID(),
							this.ieme_election);
					
					// le dernier est parti je suis seul
					if (np.getNeighbors().size() <= 2) {
						patchLeader(host, lm);
					}
					emp.emit(host, lm);
				}
			}
		}
	}
	
	

	/**
	 *
	 */
	@Override
	public void lostNeighborDetected(Node host, long id_lost_neighbor) {
		/*
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
	
		patchNeighbors(host, id_lost_neighbor);
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
				VKT04ElectionTrigger(host);
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
