package ara.manet.algorithm.election;

import java.util.ArrayList;
import java.util.List;

import ara.manet.Monitorable;
import ara.manet.communication.EmitterProtocolImplNextGeneration;
import ara.manet.communication.WrapperEmitter;
import ara.manet.communication.WrapperInterfaceEmitter;
import ara.manet.detection.NeighborProtocolVKTImpl;
import ara.manet.detection.NeighborhoodListener;
import ara.util.AckMessageNextGeneration;
import ara.util.BeaconMessage;
import ara.util.ElectionMessageNextGeneration;
import ara.util.LeaderMessage;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
	
public class VKT04ElectionNextGeneration implements ElectionProtocol, Monitorable, NeighborhoodListener {

	private static final long ALL = -2; // Broadcast == true
	
	private static final String PAR_PERIODE_LEADER = "periode_leader";
	private static final String PAR_PERIODE_BEACON = "periode_beacon";
	private static final String PAR_TIMER_BEACON = "periode_timer_beacon";
	
	public static final String leader_event = "LEADEREVENT";
	public static final String beacon_event = "BEACONEVENT";
	public static final String timer_beacon_event = "BEACONTIMEREVENT";
	
	private int my_pid; 							// protocol
	
	private final int periode_leader;				// duree entre deux elections 
	private final int periode_beacon;				// temps entre deux beacons
	private final int periode_timer_beacon;			// temps entre deux timeout quand le beacono n'est pas recu.
	
	private List<Long> neighbors;					// Liste de voisins.
	private List<Long> neighbors_ack;				// Liste de voisins.
	private int desirability; 						// desirabilité du noeud									(-1 si inconnu)
	private long parent; 							// permet de connaître son père et remonter dans l'arbre 	(-1 si inconnu)
	private long id_leader;							// id du leader actuel, -1 si aucun leader.					(-1 si inconnu)
	private long desirability_leader;				// desirabilité du noeud leader								(-1 si inconnu)
	private long potential_leader;					// id du leader potentiel, -1 si aucun leader.				(-1 si inconnu)
	private long desirability_potential_leader;		// désirabilité du leader potentiel, -1 si aucun leader.	(-1 si inconnu)
	
	// new variables for dynamic protocol
	private boolean is_electing;					// Variable qui dit si ce noeud est en train de faire une éléction.			(0 si inconnu)
	private boolean ack_2_parent;					// Variable qui dit si ce noeud a envoyé son ack à son père.				(false si inconnu)
	private long source_election;					// Noeud d'où provient l'élection dans laquelle je suis.					(-1 si inconnu)
	private long ieme_election;						// indique pour ce node la ieme election qu'il lance.						(0 si inconnu)
													// utile pour differencier les elections et choisir parmi elles.			
													// Plus un node lance d'election plus il a de chance d'en lancer.
													// Soit i,j Node² : (i.ieme_election(), i.getID()) > (j.ieme_election(), j.getID())
													// <=> i.ieme_election() > j.ieme_election() ||
													// (i.ieme_election() == j.ieme_election()) &&  (i.getID() >  j.getID())
	
	private long source_ieme_election; 				// ieme election de la source a laquelle je participe
	private long ieme_election_max;					// La plus grande election à laquelle j'ai participé.						(0 si inconnu)
	private boolean ok_quantum;	 					// presence du leader pour la detection de perte de leader
	private int state;								// 0 : leader_known
													// 1 : leader_unknown
													// 2 : leader_isMe
	private static int ack_message;
	private static int leader_message;
	private static int election_dynamic_message;
	private static int beacon_message;
	
	boolean first = false;							// SI c'est la premiere execution ALORS il faut attendre
	private int timeout_leader;						// Nombre de fois que je peux ne pas recevoir le beacon du leader avant de le
													// considere mort.
	private boolean timeout_need_2b_check;

	
	
	public VKT04ElectionNextGeneration(String prefix) {
		
		String tmp[] = prefix.split("\\.");
		this.my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.periode_leader = Configuration.getInt(prefix + "." + PAR_PERIODE_LEADER, -1);
		this.periode_beacon = Configuration.getInt(prefix + "." + PAR_PERIODE_BEACON, -1);
		this.periode_timer_beacon = Configuration.getInt(prefix + "." + PAR_TIMER_BEACON, -1);
	
		// Creation de liste privees.
		this.neighbors = new ArrayList<Long>(); 		// Liste des voisins
		this.neighbors_ack = new ArrayList<Long>(); 
	
		this.parent = -1;

		this.id_leader = -1;
		this.desirability_leader = -1;
		this.ok_quantum = false;
		this.timeout_leader = 6;
		this.timeout_need_2b_check = false;
		
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
	
	
	private WrapperEmitter getEmitterProtocol(Node host) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImplNextGeneration emp = (EmitterProtocolImplNextGeneration) host.getProtocol((emitter_pid));
		WrapperEmitter wm = new WrapperEmitter((WrapperInterfaceEmitter) emp);
		
		return wm;
	}
	
	private NeighborProtocolVKTImpl getNeighborProtocol(Node host) {
		
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol(neighbor_pid);
		
		return np;
	}
	
	public Object clone() {
		VKT04ElectionNextGeneration vkt = null;
		try {
			vkt = (VKT04ElectionNextGeneration) super.clone();
			
			vkt.neighbors = new ArrayList<Long>(); 		// Liste des voisins
			vkt.neighbors_ack = new ArrayList<Long>(); 
			vkt.parent = -1;
			
			vkt.id_leader = -1;
			vkt.desirability_leader = -1;
			vkt.ok_quantum = false;
			vkt.timeout_need_2b_check = false;
			vkt.timeout_leader = 6;
			
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
		
		// Récupération des protocoles
		WrapperEmitter wm = this.getEmitterProtocol(host);
		NeighborProtocolVKTImpl np = this.getNeighborProtocol(host);

		// List initialisation
		this.neighbors = new ArrayList<Long>(np.getNeighbors());
		this.neighbors_ack = new ArrayList<Long>(np.getNeighbors());

		// etat
		this.state = 1;
		
		// lien de parente
		this.parent = -1;
		
		// leader
		this.id_leader = -1;
		this.desirability_leader = -1;
		this.ok_quantum = false;
		this.timeout_leader = 6;
		this.timeout_need_2b_check = false;
		
		// valeur
		this.desirability_potential_leader = host.getID(); 		// ma valeur par defaut
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
		
		ElectionMessageNextGeneration emng = new ElectionMessageNextGeneration(host.getID(), ALL, this.my_pid ,this.source_election, this.ieme_election);
		wm.emit(host, emng);
		
		// Ajout d'evenemnt recurrent d'election possible ici
		// EDSimulator.add(periode_leader, leader_event, host, my_pid);
	}
	
	
	/********************************** ELECTION MESSAGE ****************************************/	
	/**
	 * Met à jour les champs nécessaire à l'élection avec les valeurs worthy
	 * 
	 * du nouveau parent.
	 * @param host moi même
	 * @param edm message d'éléction dynamique
	 */
	private void patchAfterElectionMessage(Node host, ElectionMessageNextGeneration emng) {
		
		// Recuperation du protocol de Neighbor
		NeighborProtocolVKTImpl np = this.getNeighborProtocol(host);
		
		// patch le parent *new father*
		parent = emng.getIdSrc();
		ack_2_parent = false;
		
		// variable leader remis a 0
		id_leader = -1;
		desirability_leader = -1;
		timeout_leader = 6;
		timeout_need_2b_check = false;
		
		// List initialisation
		neighbors = new ArrayList<Long>(np.getNeighbors());
		neighbors_ack = new ArrayList<Long>(np.getNeighbors());
		neighbors_ack.remove(parent);
		neighbors.remove(parent);
		
		// passe en mode election
		is_electing = true;
		
		// patch les variables d'election
		source_election = emng.getSource_election();
		source_ieme_election = emng.getIeme_election();
		
		// ma plus grande election
		ieme_election_max = Math.max(this.ieme_election_max, source_ieme_election);
		
		//patch state
		state = 1;
	}
	
	/**
	 * @param host moi même
	 * @param edm message d'éléction dynamique
	 * @return Vrai si je suis plus worthy
	 */
	private boolean worthierElection(Node host, ElectionMessageNextGeneration emng) {
		
		return this.ieme_election_max > emng.getIeme_election()				// strictly superior
				|| (this.ieme_election_max == emng.getIeme_election() 		// same election, descrimination by source ID
					&& this.source_election >= emng.getSource_election());	// if i'm the same election i'm worthier (no redonduncy)
	}

	
	/**
	 * Le but est de propager le l'éléction provenant du message en paramètre
	 * @param host moi même
	 * @param edm message d'éléction dynamique
	 */
	private void PropagateElection2children(Node host, ElectionMessageNextGeneration emng) {
		
		ElectionMessageNextGeneration em_propagation;
		AckMessageNextGeneration amng;
		
		WrapperEmitter wm = this.getEmitterProtocol(host);
				
		// J'ai des fils, 1 ca veut dire qu'il ne reste que mon pere?
		if (neighbors_ack.size() > 0) {
			for (Long neinei : neighbors_ack) {
				
				Node dest = Network.get(neinei.intValue());
				if (dest.getID() == parent) { continue; } // Skip l'id du pere
				em_propagation = new ElectionMessageNextGeneration(host.getID(), dest.getID(), my_pid, emng.getSource_election(), emng.getIeme_election());
				wm.emit(host, em_propagation);
			}
		} else {
			// je n'ai pas de fils je dois repondre a mon pere.
			amng = new AckMessageNextGeneration(host.getID(), emng.getIdSrc(), my_pid, potential_leader, desirability_potential_leader, emng.getSource_election(), emng.getIeme_election());
			wm.emit(host, amng);
		}
	}

	private boolean sameElection(Node host, ElectionMessageNextGeneration emng) {
		
		return source_election == emng.getSource_election()			// meme source d'election (verifier le changement de source)
				&& source_ieme_election == emng.getIeme_election();	// meme ieme election
	}

	/**
	 * Fonction appelée en cas de réception d'un message d'éléction dynamique
	 * Elle sert a résoudre les conflits si une éléction est déjà en cours
	 * ou si j'ai un parent illégitime de devenir leader.
	 * 
	 * @param host moi même
	 * @param edm message d'éléction dynamique
	 */
	private void recvElectionDynamicMsg(Node host, ElectionMessageNextGeneration event) {

		AckMessageNextGeneration am;
		WrapperEmitter wm = getEmitterProtocol(host);
		
		if (state == 1) { 	// Je ne connais pas mon leader
			if (is_electing) { // Je suis en cours d'election
				if (!sameElection(host, event)) { // Ce n'est pas la meme election
					if (!worthierElection(host, event)) { // son election vaut plus que la mienne
						patchAfterElectionMessage(host, event);
						PropagateElection2children(host, event);
					} else {
						// ne pas repondre a une election pourrie. Ou alors...
						// Repondre pour propager ma propre election?
					}
				} else {
					// c'est la meme election, je dois ack avec ma valeur, je suis attendu
					am = new AckMessageNextGeneration(host.getID(), event.getIdSrc(), my_pid, potential_leader, desirability_potential_leader, source_election, source_ieme_election);
					wm.emit(host, am);
				}
			} else {
				// Je ne suis pas en cours d'election + pas de leader => bug
			}
		}
		
		if (state == 2 || state == 0) {
			if (!worthierElection(host, event)) { // son election vaut plus que la mienne
				patchAfterElectionMessage(host, event);
				PropagateElection2children(host, event);
			}
		}
	}

	/********************************** ACK MESSAGE ****************************************/
	/**
	 * Surcharge de la fonction pour les messages de type AckMessge
	 * @param host
	 * @param ack
	 */
	
	
	private boolean sameElection(Node host, AckMessageNextGeneration amng) {
		
		return source_election == amng.getSource_election()
				&& source_ieme_election == amng.getIeme_election();
	}
	
	private void patchAfterAckMessage(Node host, AckMessageNextGeneration am) {
		
		this.potential_leader = Integer.max((int) potential_leader, Integer.max((int)am.getMostValuedNode(), (int) host.getID()));
		this.desirability_potential_leader = Integer.max((int) desirability_potential_leader, Integer.max((int)am.getMostValuedNodeDesirability(), (int) host.getID()));
	}
	
	/**
	 * TODO 
	 * 
	 * @param host
	 * @param event
	 */
	private void recvAckMsg(Node host, AckMessageNextGeneration event) {

		AckMessageNextGeneration am_father;
		LeaderMessage lm_broadcast;
		
		WrapperEmitter wm = this.getEmitterProtocol(host);
		AckMessageNextGeneration am = (AckMessageNextGeneration)event;
		
		if (state == 1) { 									// etat inconnu	
			if (sameElection(host, event)) { 				// l'election est la meme.
				neighbors_ack.remove(event.getIdSrc()); 	// remove is empty safe.
				patchAfterAckMessage(host, am);				// mise a jour des valeurs en fonction du ack.
			} else {
				// un ack d'une election differente mais je recupere quand meme sa valeur TODO ?
			}

			if (neighbors_ack.isEmpty() && !ack_2_parent) {

				ack_2_parent = true; 						// J'ai repondu a mon pere (tres important sinon le nombre de messages explose
				
				if (this.parent >= 0) { 					// reponse a mon pere si j'en ai un (0 inclu)
					// Envoie d'un ack à mon père, je suis une feuille
					am_father = new AckMessageNextGeneration(host.getID(), parent, my_pid, potential_leader, desirability_potential_leader, source_election, source_ieme_election);
					wm.emit(host, am_father);
				} else {
					
					patchAfterAckMessage(host, am); 					// mise a jour des valeurs en fonction du ack.
					is_electing = false; 							// on sort de l'election
					id_leader = potential_leader; 						// Je propose l'election de mon potentiel
					desirability_leader = desirability_potential_leader;// mise a jour de la valeur de desirabilite
					state = (id_leader == host.getID())? 2 : 0;			// auto election?
	
					// Broadcast du message de leader
					lm_broadcast = new LeaderMessage(host.getID(), ALL, my_pid, id_leader, desirability_leader, source_election, ieme_election);
					wm.emit(host, lm_broadcast);
					
					//EDSimulator.add(periode_beacon, beacon_event, host, my_pid);
				}
			}
		}
		
		if (state == 2 || state == 0) {
			
		}
	}



	/********************************** LEADER MESSAGE ****************************************/
	
	private boolean sameElection(Node host, LeaderMessage lm) {
		
		return this.source_election == lm.getSource_election()
				&& this.source_ieme_election == lm.getIeme_election()
				&& this.desirability_potential_leader == lm.getMostValuedNodeDesirability();
	}
	
	
	/**
	 * @param host
	 * @param event
	 */
	private void patchLeader(Node host, LeaderMessage lm) {
		
		// Patch des variables leader
		id_leader = lm.getMostValuedNode();
		desirability_leader = lm.getMostValuedNodeDesirability();
		ieme_election_max = lm.getIeme_election();
		ok_quantum = false; 					// TODO il commence a faux car je n'ai pas encore recu de beacon
		timeout_need_2b_check = true;
		
		// etat
		state = lm.getMostValuedNode() == host.getID() ? 2 : 0;
	}
	

	/**
	 * @param host
	 * @param event
	 * @return
	 */
	private boolean worthierLeader(Node host, LeaderMessage event){
		
		return this.ieme_election_max > event.getIeme_election()
				|| (this.ieme_election_max == event.getIeme_election()
					&& this.source_election >= event.getSource_election());	
	}

	
	/**
	 * Le but est de propager le l'éléction provenant du message en paramètre
	 * @param host moi même
	 * @param edm message d'éléction dynamique
	 */
	private void PropagateLeader(Node host, LeaderMessage lm) {

		LeaderMessage lm_p;
		WrapperEmitter wm = this.getEmitterProtocol(host);

		for (Long neinei : neighbors) {
			
			Node dest = Network.get(neinei.intValue());

			if (dest.getID() == lm.getIdSrc()) { continue; }
			
			lm_p = new LeaderMessage(host.getID(), dest.getID(), my_pid, lm.getMostValuedNode(), lm.getMostValuedNodeDesirability(), lm.getSource_election(), lm.getIeme_election());
			wm.emit(host, lm_p);
		}
	}
	
	/**
	 * @param host
	 * @param event
	 */
	private void mergeLeader(Node host, LeaderMessage event) {
		
		WrapperEmitter wm = this.getEmitterProtocol(host);
		
		if (worthierLeader(host, event)) {
			LeaderMessage lm_local = new LeaderMessage(host.getID(), event.getIdSrc(),  my_pid, id_leader, desirability_leader, source_election, source_ieme_election);
			wm.emit(host, lm_local);	
		}
		else {
			patchLeader(host, event);
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
		
		if (event.getIdSrc() != host.getID()) {
		
			if (this.state == 1) { 	 // 1 : leader_unknown
				patchLeader(host, event);
				PropagateLeader(host, event);
				//EDSimulator.add(periode_timer_beacon, timer_beacon_event, host, my_pid);
			} else {
				if (is_electing) {
					if (sameElection(host, event)) {
						// On essaye d'elire le meme, ne rien faire?
					} else {
						// bizarre
					}
				} else {
					//mergeLeader(host, event);
				}
			}
		}
	}

	/********************************** BEACON MESSAGE ****************************************/
	
	/**
	 * Le but est de propager le l'éléction provenant du message en paramètre
	 * @param host moi même
	 * @param edm message d'éléction dynamique
	 */
	private void PropagateBeacon(Node host, BeaconMessage bm) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImplNextGeneration emp = (EmitterProtocolImplNextGeneration) host.getProtocol((emitter_pid));
		WrapperEmitter wm = new WrapperEmitter((WrapperInterfaceEmitter) emp);
		
		// Recuperation du protocol de Neighbor
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol(neighbor_pid);
	
		for (Long neinei : np.getNeighbors()) {
			
			Node dest = Network.get(neinei.intValue());
			
			// ne pas propager a son pere.
			if (dest.getID() == bm.getIdSrc()) { continue; }
			
			BeaconMessage em_propagation = new BeaconMessage(host.getID(), dest.getID(),
					this.my_pid,
					bm.getMostValuedNode(),
					bm.getMostValuedNodeDesirability(),
					bm.getSource_election(),
					bm.getIeme_election());
			
			wm.emit(host, em_propagation);
		}
	}

	
	
	private boolean sameLeader(Node host, BeaconMessage bm) {
		
		return this.source_election == bm.getSource_election()
				&& this.source_ieme_election == bm.getIeme_election()
				&& this.desirability_potential_leader == bm.getMostValuedNodeDesirability();
	}
	

	private void recvBeaconMessage(Node host, BeaconMessage event) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImplNextGeneration emp = (EmitterProtocolImplNextGeneration) host.getProtocol((emitter_pid));
		WrapperEmitter wm = new WrapperEmitter((WrapperInterfaceEmitter) emp);
		
		if (event.getIdSrc() != host.getID()
				&& !ok_quantum){
			
			if (state == 1) {
				timeout_need_2b_check = false;
				this.timeout_leader = 6;
				ok_quantum = false;
				// je n'ai pas de leader et je recois un beacon
				
			} else {
				
				// j'ai un leader, est-ce un beacon pour le meme
				if (sameLeader(host, event) && timeout_need_2b_check){

					// re-armement du timout, le leader est en vie.
					this.timeout_leader = 6;
					ok_quantum = true;
					PropagateBeacon(host, event);
				}
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
		EmitterProtocolImplNextGeneration emp = (EmitterProtocolImplNextGeneration) host.getProtocol((emitter_pid));
		WrapperEmitter wm = new WrapperEmitter((WrapperInterfaceEmitter) emp);

		if (state == 0 || state == 2) {
			// Echange de leader
			LeaderMessage lm_cible = new LeaderMessage(host.getID(), id_new_neighbor,
					this.my_pid,
					this.id_leader,
					this.desirability_leader,
					this.source_election,
					this.source_ieme_election);
			wm.emit(host, lm_cible);
		} else {
			// Propagation d'election
			ElectionDynamicMessage em_propagation = new ElectionDynamicMessage(host.getID(), id_new_neighbor,
					this.my_pid,
					this.potential_leader,
					this.desirability_potential_leader,
					this.source_election,
					this.source_ieme_election);
			wm.emit(host, em_propagation);
		}
		*/
		
	}
	
	
	/***********************Lost Neighbor Detected ******************************/
	/**
	 *
	 */
	@Override
	public void lostNeighborDetected(Node host, long id_lost_neighbor) {
		
		/*
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImplNextGeneration emp = (EmitterProtocolImplNextGeneration) host.getProtocol((emitter_pid));

		// Recuperation du protocol de Neighbor
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolVKTImpl np = (NeighborProtocolVKTImpl) host.getProtocol(neighbor_pid);

		// leader connu
		if (this.state == 2 || this.state == 0) {	
			if (
					//this.leader_direction == id_lost_neighbor ||
					id_lost_neighbor == this.id_leader) {
				
				LeaderMessage lm = new LeaderMessage(host.getID(), ALL, 
						this.my_pid,
						host.getID(),
						this.desirability_potential_leader,
						host.getID(),
						this.ieme_election);
				//patchLeader(host, lm);
				// y'a plus personne je suis sur que si je me patch y'a pas de probleme
				if (np.getNeighbors().size() <= 2) {
					//patchLeader(host, lm);
				}
				// en envoyant un faux message j'espere recevoir l'identite du leader
				// de la composante connexe /////////sinon je serai elu?
				//emp.emit(host, lm);
			}
		} else {
				
					if (is_electing) {
						if (id_lost_neighbor == parent) {
						
						System.err.println("LPDE : " + host.getID() 
						+ " id_lost: " + id_lost_neighbor 
						+ " parent: " + parent
						+ " size compo: " + np.getNeighbors().size());
						
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
			}*/
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
		if(ok_quantum) {
			res.add("P");
		} else {
			res.add("----");
		}

		return res;
	}
	
	
	/********************************ProcessEvent**********************************/	
	@Override
	public void processEvent(Node host, int pid, Object event) {
		
		if (pid != my_pid) {
			throw new RuntimeException("Receive Event for wrong protocol");
		}
		
		// Gestion de la réception d'un message de type ElectionMessage
		if (event instanceof ElectionMessageNextGeneration) {
			election_dynamic_message++;
			recvElectionDynamicMsg(host, (ElectionMessageNextGeneration) event);
			return;
		}
		
		// Gestion de la réception d'un message de type LeaderMessage
		if (event instanceof LeaderMessage) {
			leader_message++;
			recvLeaderlMsg(host, (LeaderMessage) event);
			return;
		}

		// Gestion de la réception d'un message de type AckMessage
		if (event instanceof AckMessageNextGeneration) {
			ack_message++;
			recvAckMsg(host, (AckMessageNextGeneration) event);
			return;
		}
		
		// Gestion de la réception d'un message de type AckMessage
		if (event instanceof BeaconMessage) {
			beacon_message++;
			//recvBeaconMessage(host, (BeaconMessage) event);
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

				return;
			}
			
			
			// Le leader recoit un evenement beacon_event il doit prevenir tout le monde qu'il est en vie.
			/*
			if (ev.equals(beacon_event)) {
				if (state == 2) {
				
					//System.err.println("BEACON" + host.getID() + "TO all");
					int emitter_pid = Configuration.lookupPid("emit");
					EmitterProtocolImplNextGeneration emp = (EmitterProtocolImplNextGeneration) host.getProtocol((emitter_pid));
					WrapperEmitter wm = new WrapperEmitter((WrapperInterfaceEmitter) emp);
					
					BeaconMessage bm_bcast = new BeaconMessage(host.getID(), ALL,
							this.my_pid,
							this.id_leader,
							this.desirability_leader,
							this.source_election,
							this.ieme_election);
					wm.emit(host, bm_bcast);
					
					EDSimulator.add(periode_beacon, beacon_event, host, my_pid);
				}
				return;
			}
			
			
			// Un noeud du graph vient de recevoir une alarme, si le leader n'a pas repondu alors il
			// faut declencher une nouvelle election.
			if (ev.equals(timer_beacon_event)){ 
				
					if (timeout_leader > 0 && timeout_need_2b_check) {
						
						timeout_leader--;	 // j'ai timeout * periode_timer_beacon, apres il est considere comme deco.
						ok_quantum = false;
						EDSimulator.add(periode_timer_beacon, timer_beacon_event, host, my_pid);
					} else {
						if (state == 0) {
							System.err.println(host.getID() + "perte complete du leader");
							VKT04ElectionTrigger(host);
						}
					}
				return;
			}
					*/
		}

		 
		throw new RuntimeException("Receive unknown Event");
	}


}
