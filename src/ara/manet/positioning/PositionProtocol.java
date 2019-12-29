package ara.manet.positioning;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ara.manet.MANETGraph;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;
import peersim.graph.Graph;
import peersim.graph.GraphAlgorithms;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public interface PositionProtocol extends Protocol {

	/**** Attribut communs à tous les noeuds **/

	/*
	 * Renvoie la vitesse maximale qu'un noeud peut avoir losque'il est en mouvement
	 */
	public int getMaxSpeed();

	/*
	 * Renvoie la vitesse minimale qu'un noeud peut avoir losque'il est en mouvement
	 */
	public int getMinSpeed();

	/* Renvoie l'abscisse maximale (largeur du terrain) */
	public double getMaxX();

	/* Renvoie l'ordonnée maximale (largeur du terrain) */
	public double getMaxY();

	/* Renvoie le temps d'immobilité */
	public int getTimePause();

	/**** Attributs propre à chaque noeud **/

	/* renvoie la position courante, assure que (0 <= x <=MaxX) et (0 <= y <=MaxY) */
	public Position getCurrentPosition();

	/* renvoie la position destination */
	public Position getCurrentDestination();

	/* renvoie la vitesse courante */
	public int getCurrentSpeed();

	/* renvoie vrai si le noeud est en mouvement, faux si le noeud est en pause */
	public boolean isMoving();

	/* Initialisation de la position courante */
	public void initialiseCurrentPosition(Node host);

	/****
	 * méthodes statiques utiles pour toutes informations relative à la position des
	 * noeuds et à la topographie du système
	 **/

	/*
	 * méthode statique renvoyant la poistion de tous les noeuds du système
	 */
	public static Map<Long, Position> getPositions(int position_protocol_pid) {
		Set<Node> nodes = new HashSet<>();
		for (int i = 0; i < Network.size(); i++) {
			nodes.add(Network.get(i));
		}
		return getPositionsOfNodeSet(position_protocol_pid, nodes);
	}

	/*
	 * méthode statique renvoyant la poistion de chaque noeud de l'ensemble passé en
	 * paramètre (id, position)
	 */
	public static Map<Long, Position> getPositionsOfNodeSet(int position_protocol_pid, Set<Node> nodes) {
		Map<Long, Position> res = new HashMap<>();
		for (Node n : nodes) {
			PositionProtocol pos_proto_n = (PositionProtocol) n.getProtocol(position_protocol_pid);
			Position cur = pos_proto_n.getCurrentPosition();
			res.put(n.getID(), cur);
		}
		return res;
	}

	/*
	 * méthode statique renvoyant l'ensemble des composantes connexes du système,
	 * renvoie une map associant un id de la composante connexe à son ensemble de
	 * noeuds
	 */
	public static Map<Integer, Set<Node>> getConnectedComponents(Map<Long, Position> positions, int scope) {
		Graph g = new MANETGraph(positions, scope);
		final GraphAlgorithms ga = new GraphAlgorithms();
		ga.weaklyConnectedClusters(g);
		Map<Integer, Set<Node>> res = new HashMap<>();
		for (int i = 0; i < Network.size(); i++) {
			int connected_component = ga.color[i];
			Set<Node> nodes = res.getOrDefault(connected_component, new HashSet<>());
			nodes.add(Network.get(i));
			res.put(connected_component, nodes);
		}
		return res;
	}

	/*
	 * méthode statique renvoyant l'ensemble des composantes connexes du système
	 * sous la forme de MANETGraph, renvoie une map associant un id de la composante
	 * connexe à son graphe
	 */
	public static Map<Integer, MANETGraph> getConnectedComponentsAsManetGraphs(Map<Long, Position> positions,
			int position_protocol_pid, int scope) {
		Map<Integer, Set<Node>> con_compo = getConnectedComponents(positions, scope);
		Map<Integer, MANETGraph> res = new HashMap<>();
		for (Entry<Integer, Set<Node>> e : con_compo.entrySet()) {
			res.put(e.getKey(), new MANETGraph(getPositionsOfNodeSet(position_protocol_pid, e.getValue()), scope));
		}
		return res;
	}

}
