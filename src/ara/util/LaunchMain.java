package ara.util;

import peersim.Simulator;


public class LaunchMain {
	//private static final String dir = "";
	public static void main(String[] args) {
		String config_path = System.getProperty("user.dir") + "/src/ara/config";
		String[] path = {config_path};
		Simulator.main(path);
		
	}
}
