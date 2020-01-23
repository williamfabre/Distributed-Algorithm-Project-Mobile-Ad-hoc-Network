package ara.manet.communication;

import ara.util.Message;
import peersim.core.Node;

public class WrapperEmitter implements WrapperInterfaceEmitter{
	
	private int count;
	
	private  WrapperInterfaceEmitter wie;
	
	 /**
	 * Constructeur permettant de r�cup�rer les param�tres de config.
	 * 
	 * @param prefix prefix
	 */
	public WrapperEmitter(WrapperInterfaceEmitter wie) {
		this.wie = wie;
	}

	public Object clone() {

		EmitterProtocolImpl emp = null;
		try {
			emp = (EmitterProtocolImpl) super.clone();
		} catch (CloneNotSupportedException e) {
		}

		return emp;
	}



	public void emit(Node host, Message msg) {

		System.err.println("toto");
		wie.emit(host, msg);
		
	}

	public int getLatency() {
		return wie.getLatency();
	}

	public int getScope() {
		return wie.getScope();
	}
	
	public void processEvent(Node node, int pid, Object event) {
		System.err.println("toto");
		wie.processEvent(node, pid, event);
		
	}
	
}
