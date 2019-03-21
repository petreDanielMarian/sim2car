package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class RouteTimeComputeMain {
	
	public static void computeAverageTravelTime(String city) {
		// Folder with the routes time for a city
		File cityDir = new File(city);
		
		// Time of sampling
		String time1 = "1798";
		String time2 = "3598";
		String time3 = "5398";
		
		long Time1TotalRoutes = 0;
		long Time2TotalRoutes = 0;
		long Time3TotalRoutes = 0;
		
		long time1AVG = 0;
		long time2AVG = 0;
		long time3AVG = 0;
		
		if (cityDir.exists() && cityDir.isDirectory()) {
			// Get all the files with route times for every sampling time
			File [] files = cityDir.listFiles();
			
			if (files != null) {
				for (File currentFile : files) {
					try {
						
						BufferedReader br = new BufferedReader(new FileReader(currentFile));
						String line;
					    
						System.out.println(currentFile.getName());
						while ((line = br.readLine()) != null) {
							String []tokens = line.split(" ");
							
							long val = Long.parseLong(tokens[1]);
							
							if (currentFile.getName().startsWith(time1)) {
								Time1TotalRoutes++;
								time1AVG += val;
							} else if (currentFile.getName().startsWith(time2)) {
								Time2TotalRoutes++;
								time2AVG += val;
							} else if (currentFile.getName().startsWith(time3)) {
								Time3TotalRoutes++;
								time3AVG += val;
							}
						}
					
						br.close();
					
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				time1AVG = time1AVG / Time1TotalRoutes;
				time2AVG = time2AVG / Time2TotalRoutes;
				time3AVG = time3AVG / Time3TotalRoutes;
				
				System.out.println("TIME " + time1 + " NR ROUTES: " + Time1TotalRoutes + " AVG TIME: " + time1AVG);
				System.out.println("TIME " + time2 + " NR ROUTES: " + Time2TotalRoutes + " AVG TIME: " + time2AVG);
				System.out.println("TIME " + time3 + " NR ROUTES: " + Time3TotalRoutes + " AVG TIME: " + time3AVG);
			}
		}
		
	}
	public static void main(String[] args) {
		computeAverageTravelTime("beijing");
	}

}
