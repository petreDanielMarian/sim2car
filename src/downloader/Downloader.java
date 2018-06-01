package downloader;

import java.io.File;

import model.parameters.Globals;

/**
 * This class is used to download the traces for the simulator
 * @author Alex
 *
 */
public class Downloader {
	private static Downloader INSTANCE = null;
	private String city;
	
	private Downloader(){}
	
	public static Downloader getInstance() {
		if (Downloader.INSTANCE == null)
			Downloader.INSTANCE = new Downloader();
		
		return Downloader.INSTANCE;
	}
	
	/*
	 * Extract the city name from the give properties file path
	 */
	private void extractCity(String propFile) {
		
		String path [] = Globals.propertiesFile.split("\\\\");
		System.out.println("Prop file path: " + Globals.propertiesFile);
		System.out.println("Prop file: " + path[path.length -1]);
		
		// Extract city
		String propF [] = path[path.length -1].split(".p");
		System.out.println("City: " + propF[0]);
		System.out.println();
		
		city = propF[0];
	}
	
	public void downloadTraces(String propFile) {
		
		// Extract the city name
		extractCity(propFile);
		
		if (checkIfTracesExist(this.city)) {
			System.out.println("Trace files exist - Skipping DOWNLOAD STEP");
		} else {
			System.out.println("Trace files don't exist - Starting DOWNLOAD STEP");
			DownloadCore core = new DownloadCore();
			core.execute(this.city);
		}
	}
	
	/*
	 * Checks if the folders for traces exist for the wanted city
	 */
	private boolean checkIfTracesExist(String city) {
		
		try {
			File curDir = new File(".");
			File f1 = new File(curDir.getAbsolutePath() + File.separator + "rawdata" +
							File.separator + "traces" + File.separator + city + "cabs");
			File f2 = new File(curDir.getAbsolutePath() + File.separator + "processeddata" +
					File.separator + "traces" + File.separator + city);
			
			if (f1.exists() && f2.exists())
				return true;
			
		}catch (SecurityException e) {
			System.err.println("You don't have read rights");
			e.printStackTrace();
		}
		
		return false;
	}
}
