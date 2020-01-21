package ara.manet;

import java.awt.Dimension;
import java.util.concurrent.TimeUnit;

import ara.manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class Echantillon implements Control{

	private static final long serialVersionUID = -4639751772079773440L;
	private static final String PAR_TIMER= "timer";

	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_NEIGHBORPID = "neighborprotocol";
	private static final String PAR_EMITTER = "emitter";
	private static final String PAR_TIMESLOW = "time_slow";
	private static final String PAR_MONITORABLEPID = "monitorableprotocol";
	
	private double timer;
	private final int position_pid;
	private final int neighbor_pid;
	private final int emitter_pid;
	private final int monitorable_pid;
	

	
	public Echantillon(String prefix) {		
		neighbor_pid= Configuration.getPid(prefix+"."+PAR_NEIGHBORPID,-1);
		position_pid=Configuration.getPid(prefix+"."+PAR_POSITIONPID);
		
		emitter_pid=Configuration.getPid(prefix+"."+PAR_EMITTER,-1);
		monitorable_pid=Configuration.getPid(prefix+"."+PAR_MONITORABLEPID,-1);
				
		timer=Configuration.getDouble(prefix+"."+PAR_TIMER);
		
		Node n = Network.get(0);
		PositionProtocol pos = (PositionProtocol) n.getProtocol(position_pid);

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
	

	private void init(){
		System.err.println("test");
	}
	
	
	@Override
	public boolean execute() {

			init();

		try {
			Thread.sleep((long)timer*1000);
		} catch (InterruptedException e) {}
		return false;
	}

}
