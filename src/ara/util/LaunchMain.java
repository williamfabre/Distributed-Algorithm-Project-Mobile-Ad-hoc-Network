package ara.util;

import peersim.Simulator;


public class LaunchMain {
	//private static final String dir = "";
	public static void main(String[] args) {
		//String config_path = System.getProperty("user.dir") + "/src/ara/config";
		//String config_path = System.getProperty("user.dir") + "/src/ara/configVKT04Statique";
		String config_path = System.getProperty("user.dir") + "/src/ara/configVKT04";
		String[] path = {config_path};
		Simulator.main(path);
		
	}
}
