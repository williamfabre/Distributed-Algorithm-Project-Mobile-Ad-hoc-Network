package ara.manet;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import ara.manet.algorithm.election.ElectionProtocol;
import ara.manet.communication.Emitter;
import ara.manet.communication.EmitterProtocolImpl;
import ara.manet.communication.WrapperEmitter;
import ara.manet.positioning.Position;
import ara.manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.util.IncrementalStats;

public class Echantillon implements Control {

	private static final long serialVersionUID = -4639751772079773440L;

	private static final String PAR_ELECTIONPID = "electionprotocol";
	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_NEIGHBORPID = "neighborprotocol";
	private static final String PAR_EMITTER = "emitter";
	private static final String PAR_MONITORABLEPID = "monitorableprotocol";
	private static final String PAR_TIMER= "timer";
	

	private double finaltime;
	private int time;
	
	private long scope;
	private double timer;
	private final int election_pid;
	private final int position_pid;
	private final int neighbor_pid;
	private final int emitter_pid;
	private final int monitorable_pid;
	
	
	private FileWriter AverageConByScope_file;
	private FileWriter VarianceByScope_file;
	private PrintWriter AverageConByScope_printer= null;
	private PrintWriter VarianceByScope_printer= null;
	
	private FileWriter Message_file;
	private FileWriter TauxInst_file;
	private PrintWriter Message_printer= null;
	private PrintWriter TauxInst_printer= null;

	private IncrementalStats is;
	private IncrementalStats successful;

	
	public Echantillon(String prefix) {

		this.time = 0;
		election_pid = Configuration.getPid(prefix + "." + PAR_ELECTIONPID, -1);
		neighbor_pid= Configuration.getPid(prefix+"."+PAR_NEIGHBORPID,-1);
		position_pid=Configuration.getPid(prefix+"."+PAR_POSITIONPID);
		emitter_pid=Configuration.getPid(prefix+"."+PAR_EMITTER,-1);
		monitorable_pid=Configuration.getPid(prefix+"."+PAR_MONITORABLEPID,-1);
		timer=Configuration.getDouble(prefix+"."+PAR_TIMER);
		
		
		finaltime = Configuration.getDouble(EDSimulator.PAR_ENDTIME); 
		
		is = new IncrementalStats();
		successful = new IncrementalStats();
		
		Emitter em = (Emitter) Network.get(0).getProtocol(emitter_pid);
		scope = em.getScope();
		
		try {
			AverageConByScope_file = new FileWriter("AverageConByScope.txt", false); // true pour append
			AverageConByScope_printer = new PrintWriter(AverageConByScope_file);
			
			VarianceByScope_file = new FileWriter("VarianceByScope.txt", false);
			VarianceByScope_printer = new PrintWriter(VarianceByScope_file);
			
			Message_file = new FileWriter("MSG.txt", true);
			Message_printer = new PrintWriter(Message_file);
			
			TauxInst_file = new FileWriter("TAUX_INST.txt", true);
			TauxInst_printer = new PrintWriter(TauxInst_file);
		
		} catch (IOException e) {
			e.printStackTrace();
		}

	}	
	
	/**
	 * Calcule du nombre de composantes connexes
	 * @return le bomnbre de composantes connexes
	 */
	private long nbConnexeComponents() {
		
		Map<Long, Position> positions = PositionProtocol.getPositions(position_pid);
		Emitter em = (Emitter) Network.get(0).getProtocol(emitter_pid);
		Map<Integer, Set<Node>> connected_components = PositionProtocol.getConnectedComponents(positions, em.getScope());
		
		return connected_components.size();	
	}
	
	

	/**
	 * @param i
	 */
	private void getSamples(double i) {
		is.add(i);
	}
	

	private void getSamplesSuccessfulness(float successfulness) {
		successful.add(successfulness);
	}
	
	/**
	 * @return
	 */
	private double averageConnexity() {
		return (is.getSum() / is.getN());
	}
	
	
	/**
	 * @return
	 */
	private double averageSuccessfulness() {
		return (successful.getSum() / successful.getN());
	}
	

	/**
	 * [somme de l'�cart a la moyenne au carr�] � nombre d'observations = variance
	 * @return
	 */
	private double variance() {
		return is.getVar();
		}
	
	
	/**
	 * @param f float
	 * @return la racine de la variance
	 */
	private double stdDeviation() {
		return is.getStD();
	}
	
	
	
	/**
	 * Calcule de la connexite 
	 */
	//"connexite" ?? via Id leader ??
	private float Successfulness(){
		
		long good_elections = 0;
		float percentage = 0;
		
		Map<Long, Position> positions = PositionProtocol.getPositions(position_pid);
		Emitter em = (Emitter) Network.get(0).getProtocol(emitter_pid);
		Map<Integer, Set<Node>> connected_components = PositionProtocol.getConnectedComponents(positions, em.getScope());

		for (Map.Entry<Integer, Set<Node> > entry : connected_components.entrySet()) {
			long max = -1;
			for (Node n : entry.getValue()) {
				max = Math.max(max, n.getID());
			}
			for (Node n : entry.getValue()) {
				ElectionProtocol ep = (ElectionProtocol) n.getProtocol(election_pid);
				if (ep.getIDLeader() == max) {
						//|| (ep.getIDLeader() == -1 
						//&& connected_components.size() == 1)
						//|| entry.getValue().size() == 1
						//|| ep.getIDLeader() == n.getID()) {
					good_elections++;
				}
			}
			percentage = (float) good_elections / Network.size();
		}
		return percentage;
	}
	
	/*GET NB MSGS*/
	private int getTotalMsgs() {
		
		int total = 0;
		for (int i = 0; i < Network.size(); i++) {

			Node node = Network.get(i);
					
			int emitter_pid = Configuration.lookupPid("emit");
					
			//TODO change emitter and neighbor in algo1 (the same classes of algo2)
					
			EmitterProtocolImpl emp = (EmitterProtocolImpl) node.getProtocol((emitter_pid));
			WrapperEmitter wm = new WrapperEmitter((EmitterProtocolImpl) emp);
			
			/*
			EmitterProtocolImpl emp = (EmitterProtocolImpl) node.getProtocol((emitter_pid));
			WrapperEmitter wm = new WrapperEmitter((EmitterProtocolImpl) emp);
				*/	
			total = total + wm.getTotal();
					
			}
		return total;
	}
	
	/*TAUX D'INSTABILITE*/
	private float getTauxInst(int t) {
		
		int N = Network.size();
		int Err = 0;
		int taux_inst = 0;
		Map<Long, Position> positions = PositionProtocol.getPositions(position_pid);
		Emitter em = (Emitter) Network.get(0).getProtocol(emitter_pid);
		Map<Integer, Set<Node>> connected_components = PositionProtocol.getConnectedComponents(positions, em.getScope());

		for (Map.Entry<Integer, Set<Node> > entry : connected_components.entrySet()) {
			
			long max = -1;
			
			for (Node n : entry.getValue()) {
				max = Math.max(max, n.getID());
			}
			
			for (Node n : entry.getValue()) {
				ElectionProtocol ep = (ElectionProtocol) n.getProtocol(election_pid);
				
				if (ep.getIDLeader() != max) {
					/*Err*/
					Err = Err + t;
					//System.out.println(t + " Node " + n.getID() + " Err ");
				} else {
					//System.out.println(t + " Node " + n.getID() + " Ok");
				}
					
				}
			}
		//System.out.println(" T = " + (float)Err/(N*t));
		return (float)Err/(N*t);
	}
	
	@Override
	public boolean execute() {
		time++;	
		getSamples(nbConnexeComponents());
		getSamplesSuccessfulness(Successfulness());
		
			//System.err.println("[" + Successfulness() + "%] Conex["+ nbConnexeComponents()+ "] Avg[" + average_connexity+ "] stdDev[" + stdDeviation(variance()) + "]");
			
			//file without append, coherent to one exec
			AverageConByScope_printer.println("[" + 
					String.format("%(.3f", averageSuccessfulness()) + 
					"%] Conex["+ nbConnexeComponents() + 
					"] Avg[" + String.format("%(.3f", averageConnexity()) +
					"] stdDev[" + String.format("%(.3f",stdDeviation()) + "]");
			
		
			//format py
			/*
			AverageConByScope_printer.println( 
				String.format("%(.2f", averageSuccessfulness()) + 
				","+ nbConnexeComponents() + 
				"," + String.format("%(.2f", averageConnexity()) +
				"," + String.format("%(.2f",stdDeviation()));
			*/
			
			//p.println("[" + Successfulness() + "%] Conex["+ nbConnexeComponents()+ "] Avg[" + average_connexity+ "] stdDev[" + stdDeviation(variance()) + "]");
		
		
		//System.out.println(CommonState.getTime() + " : " + finaltime + " : " + time);
		
		if ( CommonState.getTime() == finaltime-finaltime*0.1) {
			
			/*NB DE MESSAGES*/	
			int m = getTotalMsgs();
			System.out.println("TOTAL NB MESS = " + m);
			//file with append if we want tester different metrics from diff exec
			Message_printer.println(m);
			
			/*TAUX D'INSTABILITE*/
			//?? what time to use...
			float ti = getTauxInst(time);
			System.out.println("TAUX INST t_END = " + ti);
			
			//file with append if we want tester different metrics from diff exec
			TauxInst_printer.println(ti/*String.format("%(.4f",getTauxInst(time))*/);
			
			//TODO doesn't want to write in files
				
		}
		
		
			
		return false;
    }

}
