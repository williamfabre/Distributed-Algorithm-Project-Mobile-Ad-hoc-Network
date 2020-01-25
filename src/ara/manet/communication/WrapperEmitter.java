package ara.manet.communication;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import ara.util.AckMessageNextGeneration;
import ara.util.BeaconMessage;
import ara.util.ElectionMessageNextGeneration;
import ara.util.LeaderMessage;
import ara.util.Message;
import peersim.core.Node;

public class WrapperEmitter implements WrapperInterfaceEmitter{

	private int leader_message_count;
	private int ack_message_count;
	private int election_message_count;
	private int beacon_message_count;
	

	private double finaltime;
	private int time;
	
	private FileWriter fw;
	private PrintWriter pw= null;
	
	private  WrapperInterfaceEmitter wie;
	
	 /**
	 * Constructeur permettant de recuperer les parametres de config.
	 * 
	 * @param prefix prefix
	 */
	public WrapperEmitter(WrapperInterfaceEmitter wie) {
		
		this.wie = wie;
		
		try {
			fw = new FileWriter("MessageCount.txt", true); // true poour append
			pw = new PrintWriter(fw);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Object clone() {

		EmitterProtocolImpl emp = null;
		try {
			fw = new FileWriter("MessageCount.txt", true); // true poour append
			pw = new PrintWriter(fw);
			emp = (EmitterProtocolImpl) super.clone();
		} catch (CloneNotSupportedException | IOException e) {
		}

		return emp;
	}


	public void emit(Node host, Message msg) {
		wie.emit(host, msg);
	}

	public int getLatency() {
		return wie.getLatency();
	}

	public int getScope() {
		return wie.getScope();
	}
	
	public void processEvent(Node node, int pid, Object event) {

		// Gestion de la réception d'un message de type ElectionMessage
		if (event instanceof ElectionMessageNextGeneration) {
			election_message_count++;
			emit(node, (Message) event);
		}
		
		// Gestion de la réception d'un message de type LeaderMessage
		if (event instanceof LeaderMessage) {
			leader_message_count++;
			emit(node, (Message) event);
		}

		// Gestion de la réception d'un message de type AckMessage
		if (event instanceof AckMessageNextGeneration) {
			ack_message_count++;
			emit(node, (Message) event);
		}
		
		// Gestion de la réception d'un message de type AckMessage
		if (event instanceof BeaconMessage) {
			beacon_message_count++;
			emit(node, (Message) event);
		}
		
		pw.println(election_message_count
				+ " " + leader_message_count
				+ " " + ack_message_count
				+ " " + beacon_message_count);

		if (!(event instanceof BeaconMessage)
				&& !(event instanceof ElectionMessageNextGeneration) 
				&& !(event instanceof LeaderMessage) 
				&& !(event instanceof AckMessageNextGeneration)
				&& event instanceof Message) {
			wie.processEvent(node, pid, event);
		}
	}
}
