package ara.manet.detection;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import ara.manet.algorithm.election.ElectionProtocol;
import ara.manet.communication.EmitterProtocolImplNextGeneration;
import ara.util.ProbeMessage;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class NeighborProtocolVKTImpl implements NeighborProtocol, EDProtocol {

	public static final String heart_event = "HEARTEVENT";
	public static final String timer_event = "TIMEREVENT";

	private static final String PAR_PERIODE = "periode";
	private static final String PAR_TIMER = "timer";
	private static final String PAR_LISTENER = "listener";

	private final int my_pid; // Protocol

	private final int periode; // "The number of time units before the event is
								// scheduled.Has to be non-negative".

	private final long timer; // Tant qu'il est armee, les noeuds de la liste
								// des neighbors sont consideres comme voisins
								// apres timer seconde ils disparaissent de la liste.

	private final boolean listener; // Boolean provenant de la configuration qui permet
									// de rendre facultatifs le NeighborhoodListener.

	private List<Long> neighbors; // Liste de voisins.
	private List<Long> timers;
	//private List<Integer> values; // Valeurs
	private boolean t = false;

	/**
	 * Constructor pour peersim
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
		//values = new ArrayList<Integer>(); // liste des valeurs
		timers = new ArrayList<Long>();
	}

	/**
	 * Retourne la valeur du noeud dans notre liste.
	 * 
	 * @return la valeur du noeud id_new_neighbor
	 */
	/*public int getNeighborValue(long id_new_neighbor) {
		return values.get(neighbors.indexOf(id_new_neighbor));
	}*/

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

		NeighborProtocolVKTImpl np = null;

		try {
			np = (NeighborProtocolVKTImpl) super.clone();
			np.neighbors = new ArrayList<Long>();
			np.timers = new ArrayList<Long>();
			//np.values = new ArrayList<Integer>();
		} catch (CloneNotSupportedException e) {
			System.err.println(e.toString());
		}

		return np;
	}

	/**
	 * M�thode de reception d'un message p�riodique dans le champ scope Permet
	 * d'ajouter de nouveaux voisins
	 * 
	 * @param host noeud associ� � cet �v�nement
	 * @param msg  le message re�u
	 */
	public void recvProbMsg(Node host, ProbeMessage msg) {

		// Je n'ai pas re�u de message provenant de ce neighbor
		if (!neighbors.contains(msg.getIdSrc())) {

			long idNeighbor = msg.getIdSrc();

			neighbors.add(idNeighbor); // Je l'ajoute � mes neighbor
			//values.add(msg.getValue()); // value for LP

			// je cr�e un timer avant lequel je dois recevoir un message
			// provenant de ce node sinon il sera supprim� de ma liste des voisins.
			timers.add(timer);
			// EDSimulator.add(1, timer_event, host, my_pid);

			// Gestion du NeighborhoodListener pour certains algorithme d'�lection.
			if (listener) {

				int listener_pid = Configuration.lookupPid("election");
				NeighborhoodListener nl = (NeighborhoodListener) host.getProtocol(listener_pid);

				/* appel�e lorsque le noeud host d�tecte un nouveau voisin */
				nl.newNeighborDetected(host, idNeighbor);
			}
		} else {
			timers.set(neighbors.indexOf(msg.getIdSrc()), timer);
			// EDSimulator.add(1, timer_event, host, my_pid);
		}
		if (!t) {
			EDSimulator.add(1, timer_event, host, my_pid);
			t = true;
		}
	}

	/**
	 * M�thoode permettant de supprimer un voisin qui n'a pas su renvoyer un
	 * hearbeat avant que son timer soit arriv� � son terme.
	 * 
	 * @param host noeud associ� � cet �v�nement
	 */
	public void timer(Node host) {
		Vector<Integer> remove = new Vector<Integer>();

		for (Long n : neighbors) {
			int i = neighbors.indexOf(n);

			timers.set(i, timers.get(i) - 1);

			if (timers.get(i) == 0) {
				long idNeighbor = neighbors.get(i);
				remove.add(i);

				// Gestion du NeighborhoodListener pour certains algorithme d'�lection.
				if (listener) {

					int listener_pid = Configuration.lookupPid("election");
					NeighborhoodListener nl = (NeighborhoodListener) host.getProtocol(listener_pid);

					/* appel�e lorsque le noeud host d�tecte la perte d'un voisin */
					nl.lostNeighborDetected(host, idNeighbor);
				}

			}

		}

		for (int i = 0; i < remove.size(); i++) {

			int j = remove.elementAt(i);

			neighbors.set(j, null);
			timers.set(j, null);
			//values.set(j, null);
		}
		
		ListIterator<Long> iter = neighbors.listIterator();
		while(iter.hasNext()){
		    if(iter.next() == null){
		        iter.remove();
		    }
		}
		
		iter = timers.listIterator();
		while(iter.hasNext()){
		    if(iter.next() == null){
		        iter.remove();
		    }
		}
		
		iter = timers.listIterator();
		while(iter.hasNext()){
		    if(iter.next() == null){
		        iter.remove();
		    }
		}

		EDSimulator.add(1, timer_event, host, my_pid);

	}

	/**
	 * M�thode permettant d'envoyer un message de probe tous les periode secondes en
	 * broadcast � tous les noeuds dans mon scope.
	 * 
	 * @param host noeud associ� � cet �v�nement
	 */
	public void heartbeat(Node host) {

		// R�cup�re la valeur provenant du Leader protocol de mani�re g�n�ique
		int election_pid = Configuration.lookupPid("election");
		ElectionProtocol ep = (ElectionProtocol) host.getProtocol((election_pid));
		int value = ep.getValue();

		// Cr�e un message de broadcast local de type ProbeMessage
		// tagu� avec mon ID, la valeur provenant du protocol d'�lection et �
		// destination de tous.
		ProbeMessage probmsg = new ProbeMessage(host.getID(), ALL, my_pid, value);

		// Envoie du message
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImplNextGeneration emp = (EmitterProtocolImplNextGeneration) host.getProtocol((emitter_pid));
		emp.emit(host, probmsg);

		// Armement d'un timer pour me rappeller d'envoyer un nouveau heartbeat
		// local broadcast dans periode secondes � tous mes voisins.
		EDSimulator.add(periode, heart_event, host, my_pid);
	}

	/**
	 * Permet la gestion d'�v�nement de type ProbeMessage Permet la gestion
	 * d'�v�nement de type heartbeat ou timer
	 * 
	 * @param host  node
	 * @param pid   protocol
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
				timer(host);
				return;
			}
		}
		throw new RuntimeException("Receive unknown Event");
	}

}
