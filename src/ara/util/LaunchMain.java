package ara.util;

import ara.manet.communication.EmitterProtocolImpl;
import ara.manet.communication.EmitterProtocolImplNextGeneration;
import ara.manet.communication.WrapperEmitter;
import ara.manet.detection.NeighborhoodListener;
import peersim.Simulator;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;


public class LaunchMain {
	//private static final String dir = "";
	public static void main(String[] args) {
		
		//TODO general main input
		
		//String config_path = System.getProperty("user.dir") + "/src/ara/config";
		//String config_path = System.getProperty("user.dir") + "/src/ara/configVKT04Statique";
		//String config_path = System.getProperty("user.dir") + "/src/ara/configVKT04";
		String config_path = System.getProperty("user.dir") + "/src/ara/configVKT04Printer";
		//String config_path = System.getProperty("user.dir") + "/src/ara/configPrinter";
		String[] path = {config_path};
		Simulator.main(path);
		
	}
}
