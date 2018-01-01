package downloader;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;

import com.dropbox.core.v2.users.FullAccount;

public class DownloadCore {
	List<String> fileList;
	//Default download mode Dropbox
	private String mode = "D";
	// Used for dropbox API
	private static final String ACCESS_TOKEN = "1rhx1CwxQt8AAAAAAABAWW9BYue31WkKZ2dan-lc2uaC50rjrDZe8vgVksNXrH6v";
	
	public static void copyInputStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int len;
		
		while ((len = in.read(buffer)) >= 0) {
			out.write(buffer, 0, len);
		}
		
		in.close();
		out.close();
	
	}
	
	/*
	 * Download archive using 
	 */
    public void downloadFileDropbox(String archiveName, String archivePath){
        // Create Dropbox client
        DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial");
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);

        try {
            
        	// Get current account info
            FullAccount account = client.users().getCurrentAccount();
        	System.out.println("Accesing dropbox account and start download: " + account.getName().getDisplayName());
            
        	//output file for download --> storage location on local system to download file
            OutputStream downloadFile = new FileOutputStream(archivePath);
            
            try {
            	@SuppressWarnings("unused")
				FileMetadata metadata = client.files().downloadBuilder("/" + archiveName)
            			.download(downloadFile);
            } finally {
                downloadFile.close();
            }
        
        } catch (DbxException e) {
            System.err.println("Dropbox API error while downloading archive");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error while downloading archive");
            e.printStackTrace();
        }
        
        System.out.println("Download finised " + archiveName + System.lineSeparator());
    }
    
    /*
     * Download archive using URL
     */
	public void downloadFile(String ZIP_FILE_URL, String INPUT_ZIP_FILE) {
		try {
			
			long startTime = System.currentTimeMillis();
			URL url = new URL(ZIP_FILE_URL);
			url.openConnection();

			InputStream reader = url.openStream();
			FileOutputStream writer = new FileOutputStream(INPUT_ZIP_FILE);
			
			byte[] buffer = new byte[102400];
			long totalBytesRead = 0;
			int bytesRead = 0;
			
			System.out.println("Start downloading archive from: " + ZIP_FILE_URL);
			
			while ((bytesRead = reader.read(buffer)) > 0) {
				writer.write(buffer, 0, bytesRead);
				buffer = new byte[102400];
				totalBytesRead += bytesRead;
			}
			
			long endTime = System.currentTimeMillis();
			System.out.println("Download finished: " + totalBytesRead + " bytes read (" + new Long(endTime - startTime).toString() + " millseconds).\n");
			
			writer.close();
			reader.close();
		
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
	}
	
	/*
	 * Unzips an archive .zip
	 */
	public void unZipIt(String INPUT_ZIP_FILE, String OUTPUT_FOLDER) {
		
		try {
			
			System.out.println("Unzip start: " + INPUT_ZIP_FILE);
			
			ZipFile zipFile = new ZipFile(INPUT_ZIP_FILE);
			Enumeration<?> zipEntries = zipFile.entries();
			File OUTFILEFOLD = new File(OUTPUT_FOLDER);
			
			if (!OUTFILEFOLD.exists()) {
				OUTFILEFOLD.mkdir();
			}
			
			String OUTDIR = OUTPUT_FOLDER + File.separator;
	
			while (zipEntries.hasMoreElements()) {
				ZipEntry zipEntry = (ZipEntry) zipEntries.nextElement();
				
				if (zipEntry.isDirectory()) {
					new File(OUTDIR + zipEntry.getName()).mkdir();
					continue;
				}
				
				copyInputStream(zipFile.getInputStream(zipEntry), new BufferedOutputStream(new FileOutputStream(OUTDIR + zipEntry.getName())));
			}

			zipFile.close();
			System.out.println("Unzip Step finished: " + INPUT_ZIP_FILE);
		
		} catch (IOException ioe) {
			System.err.println("Unhandled exception:");
			ioe.printStackTrace();
		}
	}
	
	/*
	 * Downloads the traces for a city and then unzips them
	 */
	public void execute(String city) {
		// Create archives folder
		try {
			File curDir = new File(".");
			File t = new File(curDir.getAbsolutePath() + File.separator + "ARCHIVES");
			t.mkdir();

			String traces = curDir.getAbsolutePath();
			
			parseConfigFile();
			
			if (city.equalsIgnoreCase("rome")) {
				String archive = t.getAbsolutePath() + File.separator + "rome.zip";
				
				if (mode.equalsIgnoreCase("D")) {
					downloadFileDropbox("rome.zip", archive);
				} else {
					downloadFile(DownloadLinks.ROME_MOBIWAY, archive);
				}
				
				unZipIt(archive, traces);
				
			} else if (city.equalsIgnoreCase("beijing")) {
				String archive = t.getAbsolutePath() + File.separator + "beijing.zip";
				
				if (mode.equalsIgnoreCase("D")) {
					downloadFileDropbox("beijing.zip", archive);
				} else {
					downloadFile(DownloadLinks.BEIJING_MOBIWAY, archive);
				}
				
				unZipIt(archive, traces);
				
			} else if (city.equalsIgnoreCase("sanfrancisco")) {
				String archive = t.getAbsolutePath() + File.separator + "sanfrancisco.zip";
				
				if (mode.equalsIgnoreCase("D")) {
					downloadFileDropbox("sanfrancisco.zip", archive);
				} else {
					downloadFile(DownloadLinks.SANFRANCISCO_MOBIWAY, archive);
				}
				
				unZipIt(archive, traces);
				
			}
		} catch (SecurityException e) {
			System.err.println("You don't have read or write rights");
			e.printStackTrace();
		}
	}
	
	/*
	 * Reads the configuration download file
	 */
	private void parseConfigFile() {
		String line = null;

		try {
			File dir = new File(".");
			File fin = new File(dir.getAbsolutePath() + File.separator + "configureDownload.txt");
			
			// Construct BufferedReader from FileReader
			BufferedReader br = new BufferedReader(new FileReader(fin));
			// Read the first line witch is a comment
			line = br.readLine();
			
			System.out.println("Reading configureDownload.txt");
			while ((line = br.readLine()) != null) {
				System.out.println(line);
				String aux[] = line.split("=");
				
				if (aux[0].equalsIgnoreCase("mode")) {
					this.mode = aux[1];
				} else if (aux[0].equalsIgnoreCase("rome")){
					DownloadLinks.ROME_MOBIWAY = aux[1];
				} else if (aux[0].equalsIgnoreCase("beijing")) {
					DownloadLinks.BEIJING_MOBIWAY = aux[1];
				} else if (aux[0].equalsIgnoreCase("sanfrancisco")) {
					DownloadLinks.SANFRANCISCO_MOBIWAY = aux[1];
				}
			}
			
			System.out.println();
			br.close();
		
		} catch (SecurityException e) {
			System.err.println("You don't have read permission");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
