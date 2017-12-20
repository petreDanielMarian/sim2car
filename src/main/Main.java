package main;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import model.parameters.Globals;

import controller.engine.EngineInterface;
import controller.newengine.SimulationEngine;
import downloader.Downloader;

public class Main {
	
    /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(Main.class.getName());
	
	public static String[] args;


	
	static EngineInterface simulator;
	
	public static void main(String[] args) {
		
		// Download the traces
		Downloader.getInstance().downloadTraces();
		
		try {
			FileInputStream fis =  new FileInputStream("src/configurations/logging.properties");
			 LogManager.getLogManager().readConfiguration(fis);
			 fis.close();
		 } 
		 catch(IOException e) {
			 e.printStackTrace();
		 }
		
		Main.args = args;

		Globals.setUp( args );
		if (Globals.propertiesFile == null) {
			logger.severe("option -prop is mandatory");
			System.exit(0);
		}
		
		/* enable proxy connection if settings are present */
		utils.Proxy.checkForProxySettings();
		
		simulator = SimulationEngine.getInstance();
		simulator.setUp();
		simulator.start();
	}
}
