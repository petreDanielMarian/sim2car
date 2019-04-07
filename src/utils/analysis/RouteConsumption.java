package utils.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import utils.analysis.CarData.DataType;

	/***
	 * Class used to analyze travel time, fuel consumption and average speed using 4 cases:
	 * no traffic control, normal traffic control, dynamic traffic control
	 * 
	 * @author Andreea
	 *
	 */
public class RouteConsumption {
	private static TreeMap<Long, CarData> carsData = new TreeMap<Long, CarData>();

	public static void main(String[] args) {
		readCarsData("sanfrancisco");
		writeCarsAggregateData("sanfrancisco");
		//readCarsData("beijing");
		//writeCarsAggregateData("beijing");
	}
	
	/**
	 * 
	 * @param city
	 */
	public static void readCarsData(String city) {
		//readTimeReachDestData(city + "timereachdestination_statistics_noTrafficLights.txt", CarData.FileType.NoTrafficLights);
		readRouteData(city + "/TL", city + "/DTL");
	}
	
	/**
	 * 
	 * @param fileName
	 * @param fileType
	 */
	public static void readRouteData(String dirNameTL, String dirNameDTL) {
		FileInputStream fstreamTL = null;
		FileInputStream fstreamDTL = null;

		final File dirTL = new File(dirNameTL);
		final File dirDTL = new File(dirNameDTL);
		for (final File fileEntry : dirDTL.listFiles()) {
			try {
				fstreamTL = new FileInputStream(fileEntry.getAbsolutePath().replace("DTL", "TL"));
				BufferedReader brTL = new BufferedReader(new InputStreamReader(fstreamTL));
				
				fstreamDTL = new FileInputStream(fileEntry.getAbsolutePath());
				BufferedReader brDTL = new BufferedReader(new InputStreamReader(fstreamDTL));
				String lineTL;
				String lineDTL;
				
				Long carId = Long.parseLong(fileEntry.getName()
						.substring(fileEntry.getName().lastIndexOf("_") + 1, fileEntry.getName().lastIndexOf(".txt"))); 
				
				Long timeReachDestTL = 0l;
				Double avgSpeedTL = 0d;
				Double avgFuelTL = 0d;
				int countTL = 0;
				
				Long timeReachDestDTL = 0l;
				Double avgSpeedDTL = 0d;
				Double avgFuelDTL = 0d;
				int countDTL = 0;
				while ((lineTL = brTL.readLine()) != null && (lineDTL = brDTL.readLine()) != null) {
					StringTokenizer stTL = new StringTokenizer(lineTL, " ", false);
					stTL.nextToken(); // route id
					countTL++;
					timeReachDestTL += Long.parseLong(stTL.nextToken());
					avgSpeedTL += Double.parseDouble(stTL.nextToken());
					avgFuelTL += Double.parseDouble(stTL.nextToken());
					
					StringTokenizer stDTL = new StringTokenizer(lineDTL, " ", false);
					stDTL.nextToken(); // route id
					countDTL++;
					timeReachDestDTL += Long.parseLong(stDTL.nextToken());
					avgSpeedDTL += Double.parseDouble(stDTL.nextToken());
					avgFuelDTL += Double.parseDouble(stDTL.nextToken());
				}
				while ((lineTL = brTL.readLine()) != null) {
					countTL++;
				}
				while ((lineDTL = brDTL.readLine()) != null) {
					countDTL++;
				}
				if (countTL > 0 && countDTL > 0) {
					avgSpeedTL = avgSpeedTL/countTL;
					avgFuelTL = avgFuelTL/countTL;
					
					avgSpeedDTL = avgSpeedDTL/countDTL;
					avgFuelDTL = avgFuelDTL/countDTL;
					
					CarData carData = new CarData();
					if (carsData.containsKey(carId)) {
						carData = carsData.get(carId);
					}
					carData.setNoRoutesTL(countTL);
					carData.setTime(DataType.WithTrafficLights, timeReachDestTL);
					carData.setFuel(DataType.WithTrafficLights, avgFuelTL);
					carData.setSpeed(DataType.WithTrafficLights, avgSpeedTL);
					
					carData.setNoRoutesDTL(countDTL);
					carData.setTime(DataType.WithDynamicTrafficLights, timeReachDestDTL);
					carData.setFuel(DataType.WithDynamicTrafficLights, avgFuelDTL);
					carData.setSpeed(DataType.WithDynamicTrafficLights, avgSpeedDTL);
	
					carsData.put(carId, carData);
				}
				brTL.close();
				brDTL.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				try {
					fstreamTL.close();
					fstreamDTL.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
		
	}
	
	/**
	 * 
	 * @param city
	 */
	public static void writeCarsAggregateData(String city) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(city + "/CarsData.txt", "UTF-8");
			writer.println("carId "
					+ " TLnoRoutes DTLnoRoutes"
					+ " NAtimeReachDest(sec) TLtimeReachDest(sec) DTLtimeReachDest(sec)"
					+ " NAavgSpeed(km/h) TLavgSpeed(km/h) DTLavgSpeed(km/h)"
					+ " NAavgFuel(L/h) TLavgFuel(L/h) DTLavgFuel(L/h)");
			for( Map.Entry<Long, CarData> entry : carsData.entrySet() )
			{
				writer.println(entry.getKey() + " " + entry.getValue().toString());	
			}
			writer.close();

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}				
	}
}
