package ara.manet.algorithm.election;

import java.util.ArrayList;
import java.util.List;

import ara.manet.Monitorable;
import ara.manet.communication.EmitterProtocolImpl;
import ara.manet.communication.WrapperEmitter;
import ara.manet.detection.NeighborProtocolImpl;
import ara.manet.detection.NeighborhoodListener;
import ara.util.AckMessageNextGeneration;
import ara.util.BeaconMessage;
import ara.util.ElectionMessageNextGeneration;
import ara.util.LeaderMessage;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
	
public class VKT04Election implements ElectionProtocol, Monitorable, NeighborhoodListener {

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
	
	private List<Long> neighbors;							// Liste de voisins.
	private List<Long> neighbors_ack;						// Liste de voisins.
	private List<LeaderMessage> neighbors_leader_merge;		// Un node m'a envoye un message leader et je suis en election
	private List<Long> neighbors_merge;						// Un node vient d'arriver dans mon neighbor et je suis en cours d'election
	
	
	private int desirability; 						// desirabilit� du noeud									(-1 si inconnu)
	private long parent; 							// permet de conna�tre son p�re et remonter dans l'arbre 	(-1 si inconnu)
	private long id_leader;							// id du leader actuel, -1 si aucun leader.					(-1 si inconnu)
	private long desirability_leader;				// desirabilit� du noeud leader								(-1 si inconnu)
	private long potential_leader;					// id du leader potentiel, -1 si aucun leader.				(-1 si inconnu)
	private long desirability_potential_leader;		// d�sirabilit� du leader potentiel, -1 si aucun leader.	(-1 si inconnu)
	
	// new variables for dynamic protocol
	private boolean is_electing;					// Variable qui dit si ce noeud est en train de faire une �l�ction.			(0 si inconnu)
	private boolean ack_2_parent;					// Variable qui dit si ce noeud a envoy� son ack � son p�re.				(false si inconnu)
	private long source_election;					// Noeud d'o� provient l'�lection dans laquelle je suis.					(-1 si inconnu)
	private long ieme_election;						// indique pour ce node la ieme election qu'il lance.						(0 si inconnu)
													// utile pour differencier les elections et choisir parmi elles.			
													// Plus un node lance d'election plus il a de chance d'en lancer.
													// Soit i,j Node� : (i.ieme_election(), i.getID()) > (j.ieme_election(), j.getID())
													// <=> i.ieme_election() > j.ieme_election() ||
													// (i.ieme_election() == j.ieme_election()) &&  (i.getID() >  j.getID())
	
	private long source_ieme_election; 				// ieme election de la source a laquelle je participe
	private long ieme_election_max;					// La plus grande election � laquelle j'ai particip�.						(0 si inconnu)
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


	
	
	/**
	 * TODO
	 * @param prefix
	 */
	public VKT04Election(String prefix) {
		
		String tmp[] = prefix.split("\\.");
		this.my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.periode_leader = Configuration.getInt(prefix + "." + PAR_PERIODE_LEADER, -1);
		this.periode_beacon = Configuration.getInt(prefix + "." + PAR_PERIODE_BEACON, -1);
		this.periode_timer_beacon = Configuration.getInt(prefix + "." + PAR_TIMER_BEACON, -1);
	
		// Creation de liste privees.
		this.neighbors = new ArrayList<Long>();
		this.neighbors_ack = new ArrayList<Long>(); 
		this.neighbors_leader_merge  = new ArrayList<LeaderMessage>(); 
		this.neighbors_merge = new ArrayList<Long>();		
		
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
	
	
	/**
	 * TODO
	 * @param host
	 * @return
	 */
	private WrapperEmitter getEmitterProtocol(Node host) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		WrapperEmitter wm = new WrapperEmitter((EmitterProtocolImpl) emp);
		
		return wm;
	}
	
	/**
	 * TODO
	 * @param host
	 * @return
	 */
	private NeighborProtocolImpl getNeighborProtocol(Node host) {
		
		int neighbor_pid = Configuration.lookupPid("neighbor");
		NeighborProtocolImpl np = (NeighborProtocolImpl) host.getProtocol(neighbor_pid);
		
		return np;
	}
	
	/**
	 * TODO
	 */
	public Object clone() {
		VKT04Election vkt = null;
		try {
			vkt = (VKT04Election) super.clone();
			
			vkt.neighbors = new ArrayList<Long>(); 		// Liste des voisins
			vkt.neighbors_ack = new ArrayList<Long>(); 
			vkt.neighbors_leader_merge  = new ArrayList<LeaderMessage>(); 
			vkt.neighbors_merge = new ArrayList<Long>();	
			
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
		
		// R�cup�ration des protocoles
		WrapperEmitter wm = this.getEmitterProtocol(host);
		NeighborProtocolImpl np = this.getNeighborProtocol(host);

		// List initialisation
		this.neighbors = new ArrayList<Long>(np.getNeighbors());
		this.neighbors_ack = new ArrayList<Long>(np.getNeighbors());
		this.neighbors_leader_merge  = new ArrayList<LeaderMessage>(); 
		this.neighbors_merge = new ArrayList<Long>();
		neighbors.remove(host.getID());
		neighbors_ack.remove(host.getID());
		
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
		wm.processEvent(host, my_pid, emng);
		
		// Ajout d'evenemnt recurrent d'election possible ici
		// EDSimulator.add(periode_leader, leader_event, host, my_pid);
	}
	
	
	/********************************** ELECTION MESSAGE ****************************************/	
	/**
	 * Met � jour les champs n�cessaire � l'�lection avec les valeurs worthy
	 * 
	 * du nouveau parent.
	 * @param host moi m�me
	 * @param edm message d'�l�ction dynamique
	 */
	private void patchAfterElectionMessage(Node host, ElectionMessageNextGeneration emng) {
		
		// Recuperation du protocol de Neighbor
		NeighborProtocolImpl np = this.getNeighborProtocol(host);
		
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
		neighbors.remove(host.getID());
		neighbors_ack.remove(host.getID());
		
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
	 * @param host moi m�me
	 * @param edm message d'�l�ction dynamique
	 * @return Vrai si je suis plus worthy
	 */
	private boolean worthierElection(Node host, ElectionMessageNextGeneration emng) {
		
		return this.ieme_election_max > emng.getIeme_election()				// strictly superior
				|| (this.ieme_election_max == emng.getIeme_election() 		// same election, descrimination by source ID
					&& this.source_election >= emng.getSource_election());	// if i'm the same election i'm worthier (no redonduncy)
	}

	
	/**
	 * Le but est de propager le l'�l�ction provenant du message en param�tre
	 * @param host moi m�me
	 * @param edm message d'�l�ction dynamique
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
				wm.processEvent(host, my_pid, em_propagation);
			}
		} else {
			// je n'ai pas de fils je dois repondre a mon pere.
			amng = new AckMessageNextGeneration(host.getID(), emng.getIdSrc(), my_pid, potential_leader, desirability_potential_leader, emng.getSource_election(), emng.getIeme_election());
			wm.processEvent(host, my_pid, amng);
		}
	}

	/**
	 * TODO
	 * @param host
	 * @param emng
	 * @return
	 */
	private boolean sameElection(Node host, ElectionMessageNextGeneration emng) {
		
		return source_election == emng.getSource_election()			// meme source d'election (verifier le changement de source)
				&& source_ieme_election == emng.getIeme_election();	// meme ieme election
	}

	/**
	 * Fonction appel�e en cas de r�ception d'un message d'�l�ction dynamique
	 * Elle sert a r�soudre les conflits si une �l�ction est d�j� en cours
	 * ou si j'ai un parent ill�gitime de devenir leader.
	 * 
	 * @param host moi m�me
	 * @param edm message d'�l�ction dynamique
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
					wm.processEvent(host, my_pid, am);	
				}
			} else {
				// gros probleme
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
	
	
	/**
	 * @param host
	 * @param am
	 */
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
					am_father = new AckMessageNextGeneration(host.getID(), parent, my_pid, potential_leader, desirability_potential_leader, source_election, source_ieme_election);		
					wm.processEvent(host, my_pid, am_father);// Envoie d'un ack � mon p�re, je suis une feuille
				} else {
					patchAfterAckMessage(host, am); 					// mise a jour des valeurs en fonction du ack.
					lm_broadcast = new LeaderMessage(host.getID(), ALL, my_pid, potential_leader, desirability_potential_leader, source_election, ieme_election);
					patchLeader(host, lm_broadcast);
					wm.processEvent(host, my_pid, lm_broadcast);
				}
			}
		}
		
		if (state == 2 || state == 0) {
			
		}
	}



	/********************************** LEADER MESSAGE ****************************************/
	
	/**
	 * @param host
	 * @param lm
	 * @return
	 */
	private boolean sameElection(Node host, LeaderMessage lm) {
		
		return this.source_election == lm.getSource_election()
				&& this.source_ieme_election == lm.getIeme_election();
	}
	
	
	/**
	 * @param host
	 * @param event
	 */
	private void patchLeader(Node host, LeaderMessage lm) {
		
		// Patch des variables leader
		is_electing = false;
		id_leader = lm.getMostValuedNode();
		desirability_leader = lm.getMostValuedNodeDesirability();
		ieme_election_max = lm.getIeme_election();
		
		ok_quantum = false; 	// TODO il commence a faux car je n'ai pas encore recu de beacon
		timeout_need_2b_check = true;
		
		// etat
		state = lm.getMostValuedNode() == host.getID() ? 2 : 0;
		if (state == 2) {			
			EDSimulator.add(periode_beacon, beacon_event, host, my_pid); // propagation du beacon si je suis leader
		} else {
			EDSimulator.add(periode_timer_beacon, timer_beacon_event, host, my_pid); // debut du timer
		}
	}
	

	/**
	 * @param host
	 * @param event
	 * @return
	 */
	private boolean worthierLeader(Node host, LeaderMessage event){
			
		return this.desirability_leader >= event.getMostValuedNodeDesirability();
	}

	
	/**
	 * Le but est de propager le l'�l�ction provenant du message en param�tre
	 * @param host moi m�me
	 * @param edm message d'�l�ction dynamique
	 */
	private void propagateLeader(Node host, LeaderMessage lm) {

		LeaderMessage lm_p;
		WrapperEmitter wm = this.getEmitterProtocol(host);

		for (Long neinei : neighbors) {
			
			Node dest = Network.get(neinei.intValue());
			if (dest.getID() == lm.getIdSrc()) { continue; }
			
			lm_p = new LeaderMessage(host.getID(), dest.getID(), my_pid, lm.getMostValuedNode(), lm.getMostValuedNodeDesirability(), lm.getSource_election(), lm.getIeme_election());
			wm.processEvent(host, my_pid, lm_p);
		}
		
		for (Long neinei : neighbors_merge) {
			
			Node dest = Network.get(neinei.intValue());
			if (dest.getID() == lm.getIdSrc()) { continue; }
			
			lm_p = new LeaderMessage(host.getID(), dest.getID(), my_pid, lm.getMostValuedNode(), lm.getMostValuedNodeDesirability(), lm.getSource_election(), lm.getIeme_election());
			wm.processEvent(host, my_pid, lm_p);
		}
		
		//System.err.println(host.getID() +" "+ neighbors);
		//System.err.println(host.getID() +" "+ neighbors_merge);
		neighbors_merge = new ArrayList<Long>();
		
	}
	
	/**
	 * @param host
	 * @param event
	 */
	private void mergeLeader(Node host, LeaderMessage event) {
		
		neighbors_leader_merge.add(event);
		LeaderMessage lm = find_max_leader(neighbors_leader_merge);
		patchLeader(host, lm);
		propagateLeader(host, lm);
		neighbors_leader_merge = new ArrayList<LeaderMessage>();
	}
	
	/**
	 * @param nlm
	 * @return
	 */
	private LeaderMessage find_max_leader(List<LeaderMessage> nlm) {
		LeaderMessage res = nlm.remove(0);
		
		for (LeaderMessage lm : nlm) {
			if (lm.getMostValuedNodeDesirability() > res.getMostValuedNodeDesirability()) {
				res = lm;
			}
		}
		//System.err.println(this.desirability +" "+ nlm);
		//System.err.println(this.desirability +" "+ res);
		
		return res;
	}
	
	/**
	 * TODO 
	 * 
	 * @param host
	 * @param event
	 */
	private void recvLeaderlMsg(Node host, LeaderMessage event) {
		
		if (this.state == 1) { 	 								// 1 : leader_unknown
			if (is_electing) {
				if (sameElection(host, event)) {				// On essaye d'elire le meme, ne rien faire?
					
					if (neighbors_leader_merge.isEmpty()) {		// personne ne voulait merge
						patchLeader(host, event);
						propagateLeader(host, event);
					} else {								//quelqu'un voulait merge
						mergeLeader(host, event);
					}
				} else {
					neighbors_leader_merge.add(event);			// une personne qui veut merge
				}
			} else {
				mergeLeader(host, event);
			}
		}
		
		if (state == 0 || state == 2) {				// 0:2: leader_known/is_leader
			if (!sameElection(host, event)) {		// Ce n'est pas la meme election il faut merge
				mergeLeader(host, event);
			}
		}
	}

	


	/********************************** BEACON MESSAGE ****************************************/
	
	/**
	 * Le but est de propager le l'�l�ction provenant du message en param�tre
	 * @param host moi m�me
	 * @param edm message d'�l�ction dynamique
	 */
	private void PropagateBeacon(Node host, BeaconMessage bm) {

		WrapperEmitter wm = this.getEmitterProtocol(host);
	
		for (Long neinei : neighbors) {
			
			Node dest = Network.get(neinei.intValue());
			if (dest.getID() == bm.getIdSrc()) { continue; } // ne pas propager a son pere.
			if (dest.getID() == host.getID()) { continue; } // ne pas propager a soir meme
			BeaconMessage em_propagation = new BeaconMessage(host.getID(), dest.getID(),
					my_pid,
					bm.getMostValuedNode(),
					bm.getMostValuedNodeDesirability(),
					bm.getSource_election(),
					bm.getIeme_election());
			
			wm.processEvent(host, my_pid, em_propagation);
		}
	}

	
	
	/**
	 * @param host
	 * @param bm
	 * @return
	 */
	private boolean sameLeader(Node host, BeaconMessage bm) {
		
		return source_election == bm.getSource_election()
				&& source_ieme_election == bm.getIeme_election()
				&& id_leader == bm.getMostValuedNodeDesirability();
	}
	

	/**
	 * @param host
	 * @param event
	 */
	private void recvBeaconMessage(Node host, BeaconMessage event) {

		if (!ok_quantum) {	
			if (state == 1) {
				timeout_need_2b_check = false;
				this.timeout_leader = 6;
				ok_quantum = false;
			}
			if (state == 0 || state == 2) {
				if (sameLeader(host, event) && timeout_need_2b_check){ // j'ai un leader, est-ce un beacon pour le meme
					this.timeout_leader = 6; // re-armement du timout, le leader est en vie.
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

	/**
	 * @param host
	 */
	private void leaderEmptyNeighbor(Node host) {
		
		LeaderMessage lm_broadcast;
		
		if (neighbors.isEmpty()) {					// dernier noeud dans la composante
			
			lm_broadcast = new LeaderMessage(host.getID(), ALL, my_pid, host.getID(), host.getID(), host.getID(), ieme_election);
			patchLeader(host, lm_broadcast);			// patch definitif de la valeur du leader
		}
	}
	
	/**
	 * @param host
	 */
	private void electLeader(Node host) {
		
		ack_2_parent = true; 						// J'ai repondu a mon pere vu q'il n'existe pas
		is_electing = false;
		id_leader = potential_leader;
		desirability_leader = desirability_potential_leader;						
		ok_quantum = false; 						// TODO il commence a faux car je n'ai pas encore recu de beacon						
		timeout_need_2b_check = true;
		
		// etat
		state = id_leader == host.getID() ? 2 : 0;
	}
	
	/**
	 * @param host
	 */
	private void sendBeaconOrTimer(Node host) {
		if (state == 2) {			
			EDSimulator.add(periode_beacon, beacon_event, host, my_pid); // propagation du beacon si je suis leader
		} else {
			EDSimulator.add(periode_timer_beacon, timer_beacon_event, host, my_pid); // debut du timer
		}	
	}
	
	
	/***********************New Neighbor Detected ******************************/
	
	/**
	 *
	 */
	@Override
	public void newNeighborDetected(Node host, long id_new_neighbor) {
	
		LeaderMessage lm_cible;
		WrapperEmitter wm = this.getEmitterProtocol(host);
		
		if (state == 0 || state == 2) {
			lm_cible = new LeaderMessage(host.getID(), id_new_neighbor, my_pid, id_leader, desirability_leader, source_election, source_ieme_election);		
			wm.processEvent(host, my_pid, lm_cible); // Echange de leader
		}
		
		if (state == 1) {
			if (is_electing) {
				neighbors_merge.add(id_new_neighbor);
				//System.err.println(" merge " + host.getID() + " " + neighbors_merge);
				//System.err.println(" ack " + host.getID() + " " + neighbors_ack);
				//System.err.println(" nei " + host.getID() + " " + neighbors);
			} else {
				// Debut de la simulation
			}
		}
	}
	
	
	/***********************Lost Neighbor Detected ******************************/
	/**
	 *
	 */
	@Override
	public void lostNeighborDetected(Node host, long id_lost_neighbor) {

		WrapperEmitter wm = this.getEmitterProtocol(host);
		AckMessageNextGeneration am;
		AckMessageNextGeneration am_father;
		LeaderMessage lm_broadcast;
		
		neighbors.remove(id_lost_neighbor);
		neighbors_ack.remove(id_lost_neighbor);
		neighbors_merge.remove(id_lost_neighbor);

		if (state == 1) {
			if (is_electing) {										// Election en cours
				if (id_lost_neighbor == parent) {					// Perte du parent pendant une election
					parent = -1;									// perte du pere mise a -1
					
					if (neighbors_ack.isEmpty() && !ack_2_parent) {	// perte du parent et liste vide
						electLeader(host);
						sendBeaconOrTimer(host);
						lm_broadcast = new LeaderMessage(host.getID(), ALL, my_pid, potential_leader, desirability_potential_leader, source_election, ieme_election);
						propagateLeader(host, lm_broadcast);
					}
					
					if (!neighbors_ack.isEmpty()) {
						ElectionMessageNextGeneration emng = new ElectionMessageNextGeneration(host.getID(), ALL, my_pid , source_election, ieme_election);
						wm.processEvent(host, my_pid, emng);
						
					}
					
					leaderEmptyNeighbor(host);
				} else {								// Perte de mon frere							
					if (neighbors_ack.isEmpty() && !ack_2_parent) {
						ack_2_parent = true; 
						am_father = new AckMessageNextGeneration(host.getID(), parent, my_pid, potential_leader, desirability_potential_leader, source_election, source_ieme_election);			
						wm.processEvent(host, my_pid, am_father);// Envoie d'un ack � mon p�re, je suis une feuille
					}
					
					if (!neighbors_ack.isEmpty()) {
						//ElectionMessageNextGeneration emng = new ElectionMessageNextGeneration(host.getID(), ALL, my_pid , source_election,ieme_election);
						//wm.processEvent(host, my_pid, emng);
					}
					
					leaderEmptyNeighbor(host);
				}
			} 
		}
		
		if (state == 0 || state == 2) {
			leaderEmptyNeighbor(host);
		}
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
		if(ok_quantum) {
			res.add("P");
		} else {
			res.add("----");
		}
		if(is_electing) {
			res.add(parent+"");
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
		
		// Gestion de la r�ception d'un message de type ElectionMessage
		if (event instanceof ElectionMessageNextGeneration) {
			election_dynamic_message++;
			recvElectionDynamicMsg(host, (ElectionMessageNextGeneration) event);
			return;
		}
		
		// Gestion de la r�ception d'un message de type LeaderMessage
		if (event instanceof LeaderMessage) {
			leader_message++;
			recvLeaderlMsg(host, (LeaderMessage) event);
			return;
		}

		// Gestion de la r�ception d'un message de type AckMessage
		if (event instanceof AckMessageNextGeneration) {
			ack_message++;
			recvAckMsg(host, (AckMessageNextGeneration) event);
			return;
		}
		
		// Gestion de la r�ception d'un message de type AckMessage
		if (event instanceof BeaconMessage) {
			beacon_message++;
			recvBeaconMessage(host, (BeaconMessage) event);
			return;
		}
		

		// Ev�nement p�riodique d'�lections.		
		if (event instanceof String) {
			
			String ev = (String) event;

			if (ev.equals(leader_event)) {
				if (first) { VKT04ElectionTrigger(host); } 
				else {
					first = true;
					EDSimulator.add(periode_leader, leader_event, host, my_pid);
				}
				return;
			}
			
			
			// Le leader recoit un evenement beacon_event il doit prevenir tout le monde qu'il est en vie.
			if (ev.equals(beacon_event)) {
				if (state == 2) {	
					//System.err.println("BEACON" + host.getID() + "TO all");
					WrapperEmitter wm = this.getEmitterProtocol(host);
					
					BeaconMessage bm_bcast = new BeaconMessage(host.getID(), ALL,
							this.my_pid,
							this.id_leader,
							this.desirability_leader,
							this.source_election,
							this.ieme_election);
					wm.processEvent(host, my_pid, bm_bcast);
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
							//System.err.println(host.getID() + "perte complete du leader");
							VKT04ElectionTrigger(host);
						}
					}
				return;
			}
		}
		throw new RuntimeException("Receive unknown Event");
	}
}
