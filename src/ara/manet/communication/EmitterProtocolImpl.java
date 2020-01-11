package ara.manet.communication;

import ara.manet.positioning.PositionProtocol;
import ara.util.Message;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class EmitterProtocolImpl implements Emitter {
	
	private static final String PAR_LATENCY = "latency";
	private static final String PAR_SCOPE = "scope";
	private static final String PAR_VARIANCE = "variance";
	
	private final int my_pid;
	private final int latency;
	private final int scope;
	private final boolean variance;

	public EmitterProtocolImpl(String prefix) {
	
		String tmp[] = prefix.split("\\.");
		my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		
		this.latency = Configuration.getInt(prefix + "." + PAR_LATENCY);
		this.scope = Configuration.getInt(prefix + "." + PAR_SCOPE);
		this.variance = Configuration.getBoolean(prefix + "." + PAR_VARIANCE);
	}

	public Object clone() {
		
		EmitterProtocolImpl emp = null;
		
		try {
			emp = (EmitterProtocolImpl) super.clone();
		} catch (CloneNotSupportedException e) {
		}
		
		return emp;
	}

	public void deliver(Node host, Message msg) {
		
		Protocol p = (Protocol) host.getProtocol(msg.getPid());
		((EDProtocol) p).processEvent(host, msg.getPid(), msg);
	
	}

	public void recvMsg(Node host, Message msg) {
	
		Node emitter = null;
		
		for (int i = 0; i < Network.size(); i++) {
		
			if (Network.get(i).getID() == msg.getIdSrc()) {
			
				emitter = Network.get(i);
				break;
			}
		}
		if (emitter == null) {
			return;
		}

		if (emitter == host) {
			
			deliver(host, msg);
			return;
		}
		
		int position_pid = Configuration.lookupPid("position");
		PositionProtocol pemitter = (PositionProtocol) emitter.getProtocol(position_pid);
		PositionProtocol phost = (PositionProtocol) host.getProtocol(position_pid);
		double distance = pemitter.getCurrentPosition().distance(phost.getCurrentPosition());
		
		if (distance <= getScope()) {
		
			deliver(host, msg);
			return;
		}
	}

	@Override
	public void emit(Node host, Message msg) {
		
		Node dest = null;
		boolean broadcast = false;
		
		if (msg.getIdDest() == ALL)
			broadcast = true;
		
		for (int i = 0; i < Network.size(); i++) {
			
			dest = Network.get(i);
		
			if (dest.getID() == msg.getIdDest() || broadcast) {
			
				EDSimulator.add(getLatency(), msg, dest, my_pid);
				
				if (!broadcast)
					return;
			}
		}
	}

	@Override
	public int getLatency() {
		
		if (this.variance) {
			
			return CommonState.r.nextPoisson(latency);
		}
		return this.latency;
	}

	@Override
	public int getScope() {
		
		return this.scope;
	}

	@Override
	public void processEvent(Node host, int pid, Object event) {
		
		if (pid != my_pid) {
			
			throw new RuntimeException("Receive Event for wrong protocol");
		}
		
		if (event instanceof Message) {
		
			Message msg = (Message) event;
			recvMsg(host, msg);
			return;
		}
		
		throw new RuntimeException("Receive unknown Event");
	}
}
