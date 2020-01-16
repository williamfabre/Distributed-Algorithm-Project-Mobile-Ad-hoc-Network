package ara.manet.detection;

import java.util.ArrayList;
import java.util.List;

import ara.manet.algorithm.election.ElectionProtocol;
import ara.manet.communication.EmitterProtocolImpl;
import ara.util.ProbeMessage;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class NeighborProtocolImpl implements NeighborProtocol, EDProtocol {

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
	
	private final boolean listener; 	// Boolean provenant de la configuration qui permet
										// de rendre facultatifs le NeighborhoodListener.

	private List<Long> neighbors;		// Liste de voisins.
	private List<Integer> values; 		// Valeur nécessaire pour les leader protocol. // TODO LISTE INUTILE ET N'A RIEN A FAIRE ICI

	
    /**
     * Constructor  pour peersim
     * 
     * @param prefix prefix, le bon prefix à utiliser.
     */
	public NeighborProtocolImpl(String prefix) {

		String tmp[] = prefix.split("\\.");
		my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		
		this.periode = Configuration.getInt(prefix + "." + PAR_PERIODE);
		this.timer = Configuration.getInt(prefix + "." + PAR_TIMER);
		this.listener = Configuration.getBoolean(prefix + "." + PAR_LISTENER);
		
		// Creation de liste privees.
		neighbors = new ArrayList<Long>(); // Liste des voisins
		values = new ArrayList<Integer>(); // liste des valeurs
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
     * Retourne la liste des neighbours du noeud courant
     * 
     * @return np liste de noeud
     */
	@Override
	public List<Long> getNeighbors() {
		
		return this.neighbors;
	}

	
	
	@Override
	public Object clone() {
		
		NeighborProtocolImpl np = null;

		try {
			np = (NeighborProtocolImpl) super.clone();
			np.neighbors = new ArrayList<Long>();
			np.values = new ArrayList<Integer>(); // for leader protocol
		} catch (CloneNotSupportedException e) {
			System.err.println(e.toString());
		}

		return np;
	}

	
	/**
	 * Méthode de reception d'un message périodique dans le champ scope
	 * Permet d'ajouter de nouveaux voisins
	 * 
	 * @param host noeud associé à cet évènement
	 * @param msg  le message reçu
	 */
	public void recvProbMsg(Node host, ProbeMessage msg) {
		
		// Je n'ai pas reçu de message provenant de ce neighbor
		if (!neighbors.contains(msg.getIdSrc())) {
		
			long idNeighbor = msg.getIdSrc();
			neighbors.add(idNeighbor); // Je l'ajoute à mes neighbor
			values.add(msg.getValue()); // value for LP
			
			// je crée un timer avant lequel je dois recevoir un message
			// provenant de ce node sinon il sera supprimé de ma liste des voisins.
			EDSimulator.add(timer, timer_event, host, my_pid);

			// Gestion du NeighborhoodListener pour certains algorithme d'élection.
			if (listener) {
				int listener_pid = Configuration.lookupPid("election");
				NeighborhoodListener nl = (NeighborhoodListener) host.getProtocol(listener_pid);
				/* appelée lorsque le noeud host détecte un nouveau voisin */
				nl.newNeighborDetected(host, idNeighbor);
			}
		}
	}

	
	/**
	 * Méthoode permettant de supprimer un voisin qui n'a pas su
	 * renvoyer un hearbeat avant que son timer soit arrivé 
	 * à son terme.
	 * 
	 * @param host  noeud associé à cet évènement
	 */
	public void delNeighbor(Node host) {

		// On prend le premier de la liste qui a le timer le plus petit
		long idNeighbor = neighbors.get(0);
		
		// Gestion du NeighborhoodListener pour certains algorithme d'élection.
		if (listener) {
			int listener_pid = Configuration.lookupPid("election");
			NeighborhoodListener nl = (NeighborhoodListener) host.getProtocol(listener_pid);
			/* appelée lorsque le noeud host détecte la perte d'un voisin */
			
			// TODO lost?
			nl.newNeighborDetected(host, idNeighbor); 
		}
		// Supression de la liste des valeurs et de la liste des voisins.
		values.remove(0);
		neighbors.remove(0);
	}

	/**
	 * Méthode permettant d'envoyer un message de probe tous les
	 * periode secondes en broadcast à tous les noeuds dans mon scope.
	 * 
	 * @param host noeud associé à cet évènement
	 */
	public void heartbeat(Node host) {

		// Récupère la valeur provenant du Leader protocol de manière généique
		int election_pid = Configuration.lookupPid("election");
		ElectionProtocol ep = (ElectionProtocol) host.getProtocol((election_pid));
		int value = ep.getValue();

		
		// Crée un message de broadcast local de type ProbeMessage
		// tagué avec mon ID, la valeur provenant du protocol d'élection et à destination de tous.
		ProbeMessage probmsg = new ProbeMessage(host.getID(), ALL, my_pid, value); 

		// Envoie du message
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		emp.emit(host, probmsg);

		// Armement d'un timer pour me rappeller d'envoyer un nouveau heartbeat 
		// local broadcast dans periode secondes à tous mes voisins.
		EDSimulator.add(periode, heart_event, host, my_pid);
	}

	/**
	 * Permet la gestion d'évènement de type ProbeMessage
	 * Permet la gestion d'évènement de type heartbeat ou timer 
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

		if (event instanceof String) {
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
