package ara.manet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ara.manet.algorithm.election.ElectionProtocol;
import ara.manet.communication.Emitter;
import ara.manet.detection.NeighborProtocol;
import ara.manet.positioning.Position;
import ara.manet.positioning.PositionProtocol;
import peersim.Simulator;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class Echantillon implements Control {

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
	
	private float average_connexity_acu_sample;
	private float average_connexity_acu;
	private float average_connexity;
	private List<Long> mesures_connexity;
	
	private FileWriter AverageConByScope_file;
	private FileWriter VarianceByScope_file;
	private PrintWriter AverageConByScope_printer= null;
	private PrintWriter VarianceByScope_printer= null;
	private long scope;

	
	public Echantillon(String prefix) {
		
		election_pid = Configuration.getPid(prefix + "." + PAR_ELECTIONPID, -1);
		neighbor_pid= Configuration.getPid(prefix+"."+PAR_NEIGHBORPID,-1);
		position_pid=Configuration.getPid(prefix+"."+PAR_POSITIONPID);
		emitter_pid=Configuration.getPid(prefix+"."+PAR_EMITTER,-1);
		monitorable_pid=Configuration.getPid(prefix+"."+PAR_MONITORABLEPID,-1);
		timer=Configuration.getDouble(prefix+"."+PAR_TIMER);
		
		
		
		average_connexity = 0;
		average_connexity_acu_sample = 0;
		mesures_connexity = new ArrayList<Long>();
		
		Emitter em = (Emitter) Network.get(0).getProtocol(emitter_pid);
		scope = em.getScope();
		
		try {
			AverageConByScope_file = new FileWriter("AverageConByScope.txt", true); // true poour append
			AverageConByScope_printer = new PrintWriter(AverageConByScope_file);
			VarianceByScope_file = new FileWriter("VarianceByScope.txt", true);
			VarianceByScope_printer = new PrintWriter(VarianceByScope_file);		
		} catch (IOException e) {
			e.printStackTrace();
		}

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
	
	
	/**
	 * Calcule du nombre de composantes connexes
	 * @return le bomnbre de composantes connexes
	 */
	private int nbConnexeComponents() {
		
		Map<Long, Position> positions = PositionProtocol.getPositions(position_pid);
		Emitter em = (Emitter) Network.get(0).getProtocol(emitter_pid);
		Map<Integer, Set<Node>> connected_components = PositionProtocol.getConnectedComponents(positions, em.getScope());
		
		return connected_components.size();	
	}
	
	
	/**
	 * Recuperation de donnees
	 */
	private void GetSamples() {
		average_connexity_acu_sample++;
		average_connexity_acu += nbConnexeComponents();
		mesures_connexity.add((long) nbConnexeComponents());
	}
	
	private void averageConnexity() {
		average_connexity = (average_connexity_acu / average_connexity_acu_sample);
	}
	
	
	/**
	 *  [somme de l'écart a la moyenne au carré] ÷ nombre d'observations = variance
	 */
	private float variance() {
		
		float acu = 0;
		for (Long l : mesures_connexity) {
				acu += Math.pow((average_connexity - l), 2);
				
		}
		return (acu / average_connexity_acu_sample);
		
	}
	
	
	/**
	 * @param f float
	 * @return la racine de la variance
	 */
	private float stdDeviation(float f) {

		return (float) Math.sqrt(f);
		
	}
	
	
	/**
	 * Calcule de la connexite 
	 */
	private String Successfulness(){
		
		long good_elections = 0;
		long size = 0;
		
		Map<Long, Position> positions = PositionProtocol.getPositions(position_pid);
		
		// Tout le monde possede le meme scope
		Emitter em = (Emitter) Network.get(0).getProtocol(emitter_pid);
		
		// recuperation de toutes les composantes connexes.
		Map<Integer, Set<Node>> connected_components = PositionProtocol.getConnectedComponents(positions, em.getScope());
		
		float percentage = 0;

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
						//|| (ep.getIDLeader() == -1 
						//&& connected_components.size() == 1)
						//|| entry.getValue().size() == 1
						|| ep.getIDLeader() == n.getID()) {
					good_elections++;
				}
			}
			percentage = (float) good_elections / Network.size();
		}
		return ""+percentage;
	}
	

	
	@Override
	public boolean execute() {

		GetSamples();
		averageConnexity();
		//System.err.println("[" + Successfulness() + "%] Conex["+ nbConnexeComponents()+ "] Avg[" + average_connexity+ "] stdDev[" + stdDeviation(variance()) + "]");
		//p.println("[" + Successfulness() + "%] Conex["+ nbConnexeComponents()+ "] Avg[" + average_connexity+ "] stdDev[" + stdDeviation(variance()) + "]");
		//p.println("[" + Successfulness() + "%] Conex["+ nbConnexeComponents()+ "] Avg[" + average_connexity+ "] stdDev[" + stdDeviation(variance()) + "]");
		

		AverageConByScope_printer.println(average_connexity );
		VarianceByScope_printer.println(variance());

		return false;
    }
}
