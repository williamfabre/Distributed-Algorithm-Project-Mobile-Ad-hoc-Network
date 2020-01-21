package ara.manet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ara.manet.algorithm.election.ElectionProtocol;
import ara.manet.communication.Emitter;
import ara.manet.detection.NeighborProtocol;
import ara.manet.positioning.Position;
import ara.manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class Echantillon implements Control{

	private static final long serialVersionUID = -4639751772079773440L;

	private static final String PAR_ELECTIONPID = "electionprotocol";
	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_NEIGHBORPID = "neighborprotocol";
	private static final String PAR_EMITTER = "emitter";
	private static final String PAR_MONITORABLEPID = "monitorableprotocol";
	private static final String PAR_TIMER= "timer";
	
	private double timer;
	private final int election_pid;
	private final int position_pid;
	private final int neighbor_pid;
	private final int emitter_pid;
	private final int monitorable_pid;
	

	
	public Echantillon(String prefix) {
		
		election_pid = Configuration.getPid(prefix + "." + PAR_ELECTIONPID, -1);
		neighbor_pid= Configuration.getPid(prefix+"."+PAR_NEIGHBORPID,-1);
		position_pid=Configuration.getPid(prefix+"."+PAR_POSITIONPID);
		emitter_pid=Configuration.getPid(prefix+"."+PAR_EMITTER,-1);
		monitorable_pid=Configuration.getPid(prefix+"."+PAR_MONITORABLEPID,-1);
		timer=Configuration.getDouble(prefix+"."+PAR_TIMER);
	}

	private static final Monitorable defaultmonitorable = new Monitorable() {
		@Override
		public Object clone()  {
			Monitorable res=null;
			try {
				res=(Monitorable)super.clone();
			} catch (CloneNotSupportedException e) {}
			return res;
		}
	};
	

	private void Connexity(){
		
		long good_elections = 0;
		long size = 0;
		
		Map<Long, Position> positions = PositionProtocol.getPositions(position_pid);
		
		//System.err.println(positions);
		// Tout le monde possede le meme scope
		Emitter em = (Emitter) Network.get(0).getProtocol(emitter_pid);
		// recuperation de toutes les composantes connexes.
		Map<Integer, Set<Node>> connected_components = PositionProtocol.getConnectedComponents(positions, em.getScope());
		
		//size = connected_components.size();
		//System.err.println("testconnexity" + connected_components);
		// calcule du pourcentage de bon leader.
		for (Map.Entry<Integer, Set<Node> > entry : connected_components.entrySet()) {

			
			long max = -1;
			
			for (Node n : entry.getValue()) {
				// On joue sur le fait que les id sont la valeur de desirability
				max = Math.max(max, n.getID());
			}
			for (Node n : entry.getValue()) {
				ElectionProtocol ep = (ElectionProtocol) n.getProtocol(election_pid);
				// Ajout de la condition pour les algos de marya.
				if (ep.getIDLeader() == max 
						|| (ep.getIDLeader() == -1 
							&& connected_components.size() == 1)
						//|| entry.getValue().size() == 1
						|| ep.getIDLeader() == n.getID()) {
					good_elections++;
				}
			}
			float percentage = (float) good_elections / Network.size();
			System.err.println("Pourcentage d'election correct par noeud : " + percentage);	
		}
	}
	
	
	@Override
	public boolean execute() {
	
		Node n = Network.get(0);
		PositionProtocol pos = (PositionProtocol) n.getProtocol(position_pid);
		
		Connexity();

		File file = new File("Echantillon.txt");
		try {
			PrintWriter p = new PrintWriter(file);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		try {
			// wait 10s
			//Thread.sleep((long)timer*100);
			Thread.sleep(0);
		} catch (InterruptedException e) {}
		return false;
		
	}

}