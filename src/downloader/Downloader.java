package downloader;

import java.io.File;

/**
 * This class is used to download the traces for the simulator
 * @author Alex
 *
 */
public class Downloader {
	private static Downloader INSTANCE = new Downloader();
	
	private Downloader(){}
	
	public static Downloader getInstance() {
		return Downloader.INSTANCE;
	}
	
	public void downloadTraces() {
		
		if (checkIfTracesExist()) {
			System.out.println("Trace files exist - Skipping DOWNLOAD STEP");
		} else {
			System.out.println("Trace files don't exist - Starting DOWNLOAD STEP");
			DownloadCore core = new DownloadCore();
			
			// Create archives folder
			File curDir = new File(".");
			File t = new File(curDir.getAbsolutePath() + File.separator + "ARCHIVES");
			t.mkdir();

			String traces = curDir.getAbsolutePath();
			
			// Download and unzip BEIJING
			String archive = t.getAbsolutePath() + File.separator + "beijing.zip";

			core.downloadFile(DownloadLinks.BEIJING_MOBIWAY, archive);
			core.unZipIt(archive, traces);
			
			// Download and unzip ROME
			archive = t.getAbsolutePath() + File.separator + "rome.zip";

			core.downloadFile(DownloadLinks.ROME_MOBIWAY, archive);
			core.unZipIt(archive, traces);
			
			// Download and unzip SANFRANCISCO
			archive = t.getAbsolutePath() + File.separator + "sanfrancisco.zip";

			core.downloadFile(DownloadLinks.SANFRANCISCO_MOBIWAY, archive);
			core.unZipIt(archive, traces);

		}
	}

	private boolean checkIfTracesExist() {
		File curDir = new File(".");

		for (File f : curDir.listFiles()) {
			if (f.getName().equalsIgnoreCase("rawdata") ||
					f.getName().equalsIgnoreCase("processeddata")) {
				return true;
			}
		}

		return false;
	}
}
