package ara.manet.communication;

import ara.util.Message;
import peersim.core.Node;

public interface WrapperInterfaceEmitter {

	public void processEvent(Node node, int pid, Object event);

	public void emit(Node host, Message msg);

	public int getLatency();

	public int getScope();

}
