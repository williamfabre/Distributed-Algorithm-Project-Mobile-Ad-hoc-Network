package ara.manet.communication;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
/*
import ara.util.AckMessageNextGeneration;
import ara.util.BeaconMessage;
import ara.util.ElectionMessageNextGeneration;
import ara.util.KnowledgeMessage;
import ara.util.LeaderMessage;*/
import ara.util.Message;
import ara.util.ProbeMessage;
import peersim.core.Node;

public class WrapperEmitter implements WrapperInterfaceEmitter{

	/*private int leader_message_count;
	private int ack_message_count;
	private int election_message_count;
	private int beacon_message_count;
	private int knowledge_message_count;
	private int edit_message_count;*/
	static int total = 0;
	
	private double finaltime;
	private int time;
	
	private  Emitter em;;
	
	 /**
	 * Constructeur permettant de recuperer les parametres de config.
	 * 
	 * @param prefix prefix
	 */
	public WrapperEmitter(Emitter em) {
		
		this.em = em;
		
	
	}

	public Object clone() {

		WrapperEmitter we = null;
		try {
			we = (WrapperEmitter) super.clone();
		} catch (CloneNotSupportedException e) {
		}

		return we;
	}

	//pas besoin ?
	public void emit(Node host, Message msg) {
		em.emit(host, msg);
	}
	
	//pas besoin ?
	public int getLatency() {
		return em.getLatency();
	}
	
	//pas besoin ?
	public int getScope() {
		return em.getScope();
	}
	
	public int getTotal() {
		return total;
	}
	
	public void processEvent(Node node, int pid, Object event) {
		
		//test pid ?
		
		if (event instanceof Message) {
			Message msg = (Message) event;
			if (msg instanceof ProbeMessage) {
				System.out.println("Probe"); 
				// in fact no probe msg -> no wrapper in Neighbor part
			}
			else
				total++;
			
			emit(node, (Message) event);
		}
		
		//System.out.println(total);
		
		/*
		
		if (event instanceof KnowledgeMessage) {
			knowledge_message_count++;
			emit(node, (Message) event);
		}
		
		if (event instanceof KnowledgeMessage) {
			edit_message_count++;
			emit(node, (Message) event);
		}

		// Gestion de la r�ception d'un message de type ElectionMessage
		if (event instanceof ElectionMessageNextGeneration) {
			election_message_count++;
			emit(node, (Message) event);
		}
		
		// Gestion de la r�ception d'un message de type LeaderMessage
		if (event instanceof LeaderMessage) {
			leader_message_count++;
			emit(node, (Message) event);
		}

		// Gestion de la r�ception d'un message de type AckMessage
		if (event instanceof AckMessageNextGeneration) {
			ack_message_count++;
			emit(node, (Message) event);
		}
		
		// Gestion de la r�ception d'un message de type AckMessage
		if (event instanceof BeaconMessage) {
			beacon_message_count++;
			emit(node, (Message) event);
		}
		*/
		
		/*
		pw.println(election_message_count
				+ " " + leader_message_count
				+ " " + ack_message_count
				+ " " + beacon_message_count);
		*/
	
	}
}
