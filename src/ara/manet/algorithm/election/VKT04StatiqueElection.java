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
	
	
	private int my_pid; 				// protocol
	
	private final int periode_leader;	// duree entre deux elections 

	private final int periode_neighbor;	// duree entre deux check de mes voisins

	private final long timer_event;		// Tant qu'il est armee, les noeuds de la liste 
										// des neighbors sont consideres comme voisins
										// apres timer seconde ils disparaissent de la liste.
	
	private int scope;					// visibilit� d'un node
	
	private List<Long> neighbors;		// Liste de voisins.
	private List<Integer> values; 		// Valeur n�cessaire pour les leader protocol.
	private List<Long> nb_ack;			// permet de compter le nombre de ack TODO					
	private int desirability; 			// desirabilit� du noeud									(-1 si inconnu)
	private long parent; 				// permet de conna�tre son p�re et remonter dans l'arbre 	(-1 si inconnu)
	private long nb_child;				// permet d'attendre ses fils lors de l'�lection.			(-1 si inconnu)
	private long id_leader;				// id du leader actuel, -1 si aucun leader.					(-1 si inconnu)

	private int state;					// 0 : leader_known
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
		nb_ack = new ArrayList<Long>(); 	// liste noeuds qui ont ack
		parent = -1;
		nb_child = -1;
		id_leader = -1;
		state = 1;
	}
	
	public Object clone() {
		VKT04StatiqueElection vtk = null;
		try {
			vtk = (VKT04StatiqueElection) super.clone();
			vtk.neighbors = new ArrayList<Long>(); 	// Liste des voisins
			vtk.values = new ArrayList<Integer>(); 	// liste des valeurs
			vtk.nb_ack = new ArrayList<Long>(); 	// liste noeuds qui ont ack
		} catch (CloneNotSupportedException e) {
		}
		return vtk;
	}

	
	/**
	 * Fonction utilis� par la classe d'initialisation qui est appel�e
	 * en d�but de programme pour tous les noeuds.
	 * Elle a pour but d'initialis� la d�sirability du node avec son ID en param�tre.
	 * 
	 * @param node le node en lui m�me
	 */
	public void initialisation(Node node) {
		ExtendedRandom my_random = new ExtendedRandom(10);
		this.desirability = (int) (my_random.nextInt(1000) / (node.getID() + 1));
	}

	
	/**
	 * Partie d�tection statique
	 * D�tecteur statique de voisins qui va d�terminer 
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
		}
	}
	
	/**
	 * Partie �lection statique
	 * 
	 * @param host
	 */
	void VKT04StaticElectionTrigger(Node host) {
		// Cr�ation d'un message 

		// R�cup�ration du protocol de communication
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		// V�rification de l'�tat du noeud pour savoir ce qu'il doit faire.
		// TODO verifier si je suis en train de faire une election ?
		switch (state) {
		case 0: 	// 0 : leader_known
			// Si le leader a disparu depuis la mise � jour des neighbors.
			// Je commence une �l�ction
			if (!neighbors.contains(this.id_leader)) {
				state = 1;
				ElectionMessage em = new ElectionMessage(host.getID(), ALL, my_pid);
				emp.emit(host, em);
			}
			break;
		case 1: 	// 1 : leader_unknown
			// Je n'ai pas de leader je dois en trouver un.
			// Je commence une �l�ction
			ElectionMessage em = new ElectionMessage(host.getID(), ALL, my_pid);
			emp.emit(host, em);
			break;	
		case 2:		// 2 : leader_isMe
			// TODO ne rien faire?
			break;
		default: 
			break;
		}
		// cr�ation du r�veil pour refaire une v�rification de mes voisins
		EDSimulator.add(periode_neighbor, timer_event, host, my_pid);
	}
	

	/**
	 * Fonction appel�e lors d'�v�nement timer_event.
	 * 
	 * @param host
	 */
	private void NeighborStaticDetectionElection(Node host) {
			
		staticDetection(host);
		VKT04StaticElectionTrigger(host);
	}

	
	/**
	 * @param host
	 * @param event
	 */
	private void recvElectionMsg(Node host, ElectionMessage event) {
		
		int emitter_pid = Configuration.lookupPid("emit");
		EmitterProtocolImpl emp = (EmitterProtocolImpl) host.getProtocol((emitter_pid));
		
		ElectionMessage em = (ElectionMessage)event;
		
		// Si je n'ai pas de parent j'ajoute l'envoyeur comme mon p�re
		// le ack message attendra que j'ai re�u une r�ponse de tous
		// mes fils.
		if (this.parent == -1) {
			this.parent = em.getIdSrc();
		} else {
			// J'ai d�j� un parent, r�ponse immediate.
			AckMessage am = new AckMessage(host.getID(), em.getIdSrc(), my_pid);
			emp.emit(host, am);
		}
		return;
	}

	/**
	 * @param host
	 * @param event
	 */
	private void recvLeaderlMsg(Node host, LeaderMessage event) {
		// TODO
	}

	/**
	 * @param host
	 * @param event
	 */
	private void recvAckMsg(Node host, AckMessage event) {
		// TODO
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
	/* NeighborProtocol. Puisque nous sommes dans un syst�me
	 * statique, nous allons nous passer ici de la couche de d�tection dynamique de
	 * voisins via heartbeat (cod�e dans le pr�c�dent exercice) en calculant directement
	 * et statiquement les voisins selon leur position par rapport au rayon d'�mission.
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
			NeighborStaticDetectionElection(host);
			return;
		}
		
		// Ev�nement p�riodique d'affichage d'�lections.
		if (event instanceof String) {
			String ev = (String) event;

			if (ev.equals(leader_event)) {
				System.out.println(host.getIndex() + " : Leader " + getIDLeader());
				EDSimulator.add(periode_leader, leader_event, host, my_pid);
				return;
			}
		}
		
		throw new RuntimeException("Receive unknown Event");
	}
}