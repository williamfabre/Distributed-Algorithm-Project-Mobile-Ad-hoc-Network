package ara.util;

import peersim.Simulator;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;


public class LaunchMain {
	public static void main(String[] args) {

		//String config_path = System.getProperty("user.dir") + "/src/ara/config";
		//String config_path = System.getProperty("user.dir") + "/src/ara/configVKT04Statique";
		//String config_path = System.getProperty("user.dir") + "/src/ara/configVKT04";
		//String config_path = System.getProperty("user.dir") + "/src/ara/configVKT04Printer";
		if (args.length == 0) {
			System.err.println("Choose the mode [1..4] ! Exit."); 
			return;
		}
		
		String s;
		String s2 = args[0];	//check paramps
		String config_path;
		
		switch(s2) {
			case "2": config_path = System.getProperty("user.dir") + "/src/ara/config" ;
				break;
			case "1": config_path = System.getProperty("user.dir") + "/src/ara/configVKT04Statique" ;
				break;
			case "4": config_path = System.getProperty("user.dir") + "/src/ara/configPrinter" ;
				break;
			case "3": config_path = System.getProperty("user.dir") + "/src/ara/configVKT04Printer";
			default : System.err.println("Choose the right mode [1..4] ! Exit."); return;
					
		}
			String[] path = {config_path};
			Simulator.main(path); 
	}
}
