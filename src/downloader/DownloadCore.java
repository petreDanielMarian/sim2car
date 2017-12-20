package downloader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DownloadCore {
	List<String> fileList;
	
	public static void copyInputStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int len;
		
		while ((len = in.read(buffer)) >= 0) {
			out.write(buffer, 0, len);
		}
		
		in.close();
		out.close();
	
	}
	
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
}
