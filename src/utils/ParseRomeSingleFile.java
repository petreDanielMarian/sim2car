package utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class ParseRomeSingleFile {

	/**
	 * @param args
	 */
	
	static final String dirPath = "res\\res\\romecabs\\";
	static final String inputFile = "taxi_february.txt";
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		separateCabs();
	}

	/**
	 * Iterates through the large unified cabs file and moves each cab trace in a separate file
	 */
	public static void separateCabs() {
	
		HashMap<String,RandomAccessFile> files = new HashMap<String,RandomAccessFile>();
		
		try {
			RandomAccessFile f = new RandomAccessFile(dirPath + inputFile, "r");
			String line;
			
			long count = 0;
			
			while((line = f.readLine()) != null) {
				
				String[] lineData = line.split(";");
				String cabId = lineData[0];
				String date = lineData[1];
				String point2D = lineData[2].substring(lineData[2].indexOf("(") + 1, lineData[2].indexOf(")"));
				
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				Date timestamp = (Date)df.parse(date);
				
				if (!files.containsKey(cabId)) {
					
					RandomAccessFile raf = new RandomAccessFile(dirPath + cabId + ".txt", "rw");
					
					String outputLine = point2D + " " + timestamp.getTime();
					raf.write(outputLine.getBytes("UTF8"));
					
					files.put(cabId, raf);
				}
				else {
					
					RandomAccessFile raf = files.get(cabId);
					String outputLine = "\n" + point2D + " " + timestamp.getTime();
					
					raf.write(outputLine.getBytes("UTF8"));
				}
				
				count ++;
				
				
				if (count % 10000 == 0) {
				
					System.out.println(count);
				}
			}
			
			
			for (RandomAccessFile raf : files.values()) {
				
				raf.close();
			}
			
			System.out.println("END");
			
		} catch (FileNotFoundException e) {
			
			
			System.out.println("FileNotFoundException " + e.getMessage());
		} catch( IOException e) {
			
			System.out.println("IOException " + e.getMessage());
		} catch (ParseException e) {
			
			System.out.println("ParseException " + e.getMessage());
		}
	}
}
