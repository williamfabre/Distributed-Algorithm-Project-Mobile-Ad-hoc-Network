package ara.manet.detection;

import java.util.ArrayList;
import java.util.List;

import ara.manet.algorithm.election.ElectionProtocol;
import ara.manet.communication.EmitterProtocolImpl;
import ara.util.Pair;
import ara.util.ProbeMessage;
import ara.util.ReplyMessage;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class NeighborProtocolVKTImpl implements NeighborProtocol, EDProtocol {

	public static final String heart_event = "HEARTEVENT";
	public static final String timer_event = "TIMEREVENT";

	private static final String PAR_PERIODE = "periode";
	private static final String PAR_TIMER = "timer";
	private static final String PAR_LISTENER = "listener";

	private final int my_pid;			// Protocol

	private final int periode;			// "The number of time units before the event is 
										// scheduled.Has to be non-negative".
	
	private final long timer;			// Tant qu'il est armee, les noeuds de la liste 
										// des neighbors sont consideres comme voisins
										// apres timer seconde ils disparaissent de la liste.
	
	private final boolean listener; 		// Boolean provenant de la configuration qui permet
											// de rendre facultatifs le NeighborhoodListener.
	private List<Long> neighbors;			// Liste de voisins.
	private List<Pair<Long, Boolean>> neighbors_reply;	// Pour chaque voisin, vrai si j'ai entendu un reply.

	
    /**
     * Constructor  pour peersim
     * 
     * @param prefix prefix, le bon prefix � utiliser.
     */
	public NeighborProtocolVKTImpl(String prefix) {

		String tmp[] = prefix.split("\\.");
		my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		
		this.periode = Configuration.getInt(prefix + "." + PAR_PERIODE);
		this.timer = Configuration.getInt(prefix + "." + PAR_TIMER);
		this.listener = Configuration.getBoolean(prefix + "." + PAR_LISTENER);
		
		// Creation de liste privees.
		neighbors = new ArrayList<Long>(); // Liste des voisins
		neighbors_reply = new ArrayList<Pair<Long, Boolean>>(); 
	}
	
	@Override
	public Object clone() {
		
		NeighborProtocolVKTImpl np = null;

		try {
			np = (NeighborProtocolVKTImpl) super.clone();
			np.neighbors = new ArrayList<Long>();
			np.neighbors_reply = new ArrayList<Pair<Long, Boolean>>(); 
		} catch (CloneNotSupportedException e) {
			System.err.println(e.toString());
		}

		return np;
	}

	
	/**
	 * TODO a verifier. INTERDITE, c'est tricher.
     * Retourne la valeur de desirabilite du noeud dans notre liste.
     * 
     * @return la valeur du noeud id_new_neighbor
     */
	public int getNeighborValue(long id_new_neighbor) {
		
		// R�cup�re la valeur provenant du Leader protocol de mani�re g�n�rique
		//int election_pid = Configuration.lookupPid("election");
		//ElectionProtocol ep = (ElectionProtocol) Network.get((int) id_new_neighbor).getProtocol((election_pid));
		//int value = ep.getValue();
		return 0;
	}
	
    /**
     * Retourne la liste des neighbours du noeud courant
     * 
     * @return np liste de noeud
     */
	@Override
	public List<Long> getNeighbors() {
		
		return this.neighbors;
	}

	
	
	/**
	 * M�thode de reception d'un message p�riodique dans le champ scope
	 * Permet d'ajouter de nouveaux voisins
	 * 
	 * @param host noeud associ� � cet �v�nement
	 * @param msg  le message re�u
	 */
	public void recvProbMsg(Node host, ProbeMessage msg) {
		
		// Je n'ai pas re�u de message provenant de ce neighbor
		if (!neighbors.contains(msg.getIdSrc())) {
		
			long idNeighbor = msg.getIdSrc();
			neighbors.add(idNeighbor); // Je l'ajoute � mes neighbor
			
			Pair<Long, Boolean> pair = new Pair<Long, Boolean>(idNeighbor, false); 
			neighbors_reply.add(pair);
			
			// je cr�e un timer avant lequel je dois recevoir un message
			// provenant de ce node sinon il sera supprim� de ma liste des voisins.
			EDSimulator.add(timer, timer_event, host, my_pid);

			// Gestion du NeighborhoodListener pour certains algorithme d'�lection.
			if (listener) {
				int listener_pid = Configuration.lookupPid("election");
				NeighborhoodListener nl = (NeighborhoodListener) host.getProtocol(listener_pid);
				/* appel�e lorsque le noeud host d�tecte un nouveau voisin */
				nl.newNeighborDetected(host, idNeighbor);
			}
		}

		// Envoie du message de reply apres reception d'un probe.
		ReplyMessage replymsg = new ReplyMessage(host.getID(), msg.getIdSrc(), my_pid, -1); 
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		emp.emit(host, replymsg);

	}
	
	/**
	 * TODO est-ce qu'on ne devrait pas faire du probe/reply seulement
	 * pour la liste des noeuds neighbors_ack?
	 * 
	 * @param host
	 * @param event
	 */
	private void recvReplyMessage(Node host, ReplyMessage event) {
		
		// Recherche dans la liste et mise a jour du boolean comme quoi j'ai recu
		// un message de type reply
		for (Pair<Long, Boolean> pair : neighbors_reply) {
			if (pair.getId() == event.getIdSrc()) {
				pair.setBool(true);
				return;
			}
		}

	}
	
	
	/**
	 * M�thode permettant de supprimer un voisin qui n'a pas su
	 * renvoyer un hearbeat avant que son timer soit arriv� 
	 * � son terme.
	 * 
	 * @param host  noeud associ� � cet �v�nement
	 */
	public void delNeighbor(Node host) {

		// On prend le premier de la liste qui a le timer le plus petit
		long idNeighbor = neighbors.get(0);
		
		// Gestion du NeighborhoodListener pour certains algorithmes d'�lections.
		if (neighbors_reply.get(0).getBool() == false) {
			
			if (listener) {
				
				int listener_pid = Configuration.lookupPid("election");
				NeighborhoodListener nl = (NeighborhoodListener) host.getProtocol(listener_pid);
				
				/* appel�e lorsque le noeud host d�tecte la perte d'un voisin */
				nl.lostNeighborDetected(host, idNeighbor); 
			}
			
			// Supression de la liste des valeurs et de la liste des voisins.
			neighbors.remove(0);
			neighbors_reply.remove(0);
			
		} else {
			
			// On le remet a la fin de la fifo, car on a recu un message de type reply
			neighbors.add(neighbors.remove(0));
			Pair<Long, Boolean> p = neighbors_reply.remove(0);
			p.setBool(false); 	// On remet a 0 car on attend un nouveau 
			neighbors_reply.add(p);
			
			// je cr�e un timer avant lequel je dois recevoir un message
			// provenant de ce node sinon il sera supprim� de ma liste des voisins.
			EDSimulator.add(timer, timer_event, host, my_pid);
		}
	}

	/**
	 * M�thode permettant d'envoyer un message de probe tous les
	 * periode secondes en broadcast � tous les noeuds dans mon scope.
	 * 
	 * @param host noeud associ� � cet �v�nement
	 */
	public void heartbeat(Node host) {

		// R�cup�re la valeur provenant du Leader protocol de mani�re g�n�rique
		int election_pid = Configuration.lookupPid("election");
		ElectionProtocol ep = (ElectionProtocol) host.getProtocol((election_pid));
		int value = ep.getValue();

		
		// Cr�e un message de broadcast local de type ProbeMessage
		// tagu� avec mon ID, la valeur provenant du protocol d'�lection et � destination de tous.
		ProbeMessage probmsg = new ProbeMessage(host.getID(), ALL, my_pid, value); 

		// Envoie du message
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		emp.emit(host, probmsg);

		// Armement d'un timer pour me rappeller d'envoyer un nouveau heartbeat 
		// local broadcast dans periode secondes � tous mes voisins.
		EDSimulator.add(periode, heart_event, host, my_pid);
	}

	/**
	 * Permet la gestion d'�v�nement de type ProbeMessage
	 * Permet la gestion d'�v�nement de type heartbeat ou timer 
	 * 
	 * @param host node 
	 * @param pid protocol
	 * @param event evenement
	 */
	public void processEvent(Node host, int pid, Object event) {
		
		if (pid != my_pid) {
			throw new RuntimeException("Receive Event for wrong protocol");
		}

		if (event instanceof ProbeMessage) {
			recvProbMsg(host, (ProbeMessage) event);
			return;
		}
		
		// Gestion de la reception d'un message de type ProbeMessage
		if (event instanceof ReplyMessage) {
			recvReplyMessage(host, (ReplyMessage) event);
			return;
		}
		
		if (event instanceof String) {
			
			// Gestion d'evennement de type heatbeat
			String ev = (String) event;
			if (ev.equals(heart_event)) {
				heartbeat(host);
				return;
			}
			
			
			if (ev.equals(timer_event)) {
				delNeighbor(host);
				return;
			}
		}
		throw new RuntimeException("Receive unknown Event");
	}



}
