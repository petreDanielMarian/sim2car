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
		String city  = "rome";
		//readCarsData("sanfrancisco");
		//writeCarsAggregateData("sanfrancisco");
		//readCarsData("beijing");
		//writeCarsAggregateData("beijing");
		//readCarsDataNATL(city);
		//writeCarsAggregateDataNATL(city);
		
		carsData.clear();
		carsData = new TreeMap<Long, CarData>();
		readCarsDataTLDTL(city);
		writeCarsAggregateDataTLDTL(city);
	}
	
	/**
	 * 
	 * @param city
	 */
	public static void readCarsDataNATL(String city) {
		findMinimumNoRoutes(city + "/TL", "", city + "/NA");
		readRouteData(city + "/TL", "", city + "/NA");
	}
	
	/**
	 * 
	 * @param city
	 */
	public static void readCarsDataTLDTL(String city) {
		findMinimumNoRoutes(city + "/TL", city + "/DTL", "");
		readRouteData(city + "/TL", city + "/DTL", "");
	}
	
	public static void findMinimumNoRoutes(String dirNameTL, String dirNameDTL, String dirNameNA) {
		FileInputStream fstreamNA = null;
		FileInputStream fstreamTL = null;
		FileInputStream fstreamDTL = null;

		if (dirNameNA != "") {
			final File dirNA = new File(dirNameNA);
			for (final File fileEntry : dirNA.listFiles()) {
				try {
					fstreamNA = new FileInputStream(fileEntry.getAbsolutePath());
					
					BufferedReader brNA = new BufferedReader(new InputStreamReader(fstreamNA));
	
					String lineNA, prevLine = "";
					
					Long carId = Long.parseLong(fileEntry.getName()
							.substring(fileEntry.getName().lastIndexOf("_") + 1, fileEntry.getName().lastIndexOf(".txt"))); 
					int countNA = 0;
					while ( (lineNA = brNA.readLine()) != null) {
						if (lineNA.equals(prevLine))
							continue;
						countNA++;
						prevLine = lineNA;
					}
					
					CarData carData = new CarData();
					if (carsData.containsKey(carId)) {
						carData = carsData.get(carId);
					}
					if (carData.getMinNoRoutes() > countNA && countNA > 0) {
						carData.setMinNoRoutes(countNA);
						carsData.put(carId, carData);
					}
					brNA.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				} finally {
					try {
						fstreamNA.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
				
			}
		}
		if (dirNameTL != "") {
			final File dirTL = new File(dirNameTL);
			for (final File fileEntry : dirTL.listFiles()) {
				try {
					fstreamTL = new FileInputStream(fileEntry.getAbsolutePath());
					
					BufferedReader brTL = new BufferedReader(new InputStreamReader(fstreamTL));
	
					String lineTL, prevLine = "";
					
					Long carId = Long.parseLong(fileEntry.getName()
							.substring(fileEntry.getName().lastIndexOf("_") + 1, fileEntry.getName().lastIndexOf(".txt"))); 
					int countTL = 0;
					while ( (lineTL = brTL.readLine()) != null) {
						if (lineTL.equals(prevLine))
							continue;
						countTL++;
						prevLine = lineTL;
					}
					
					CarData carData = new CarData();
					if (carsData.containsKey(carId)) {
						carData = carsData.get(carId);
					}
					if (carData.getMinNoRoutes() > countTL && countTL > 0) {
						carData.setMinNoRoutes(countTL);
						carsData.put(carId, carData);
					}
					brTL.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				} finally {
					try {
						fstreamTL.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
				
			}
		}
		
		if (dirNameDTL != "") {
			final File dirDTL = new File(dirNameDTL);
			for (final File fileEntry : dirDTL.listFiles()) {
				try {
					fstreamDTL = new FileInputStream(fileEntry.getAbsolutePath());
					
					BufferedReader brDTL = new BufferedReader(new InputStreamReader(fstreamDTL));
	
					String lineDTL, prevLine = "";
					
					Long carId = Long.parseLong(fileEntry.getName()
							.substring(fileEntry.getName().lastIndexOf("_") + 1, fileEntry.getName().lastIndexOf(".txt"))); 
					int countDTL = 0;
					while ( (lineDTL = brDTL.readLine()) != null) {
						if (lineDTL.equals(prevLine))
							continue;
						countDTL++;
						prevLine = "";
					}
					
					CarData carData = new CarData();
					if (carsData.containsKey(carId)) {
						carData = carsData.get(carId);
					}
					if (carData.getMinNoRoutes() > countDTL && countDTL > 0) {
						carData.setMinNoRoutes(countDTL);
						carsData.put(carId, carData);
					}
					brDTL.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				} finally {
					try {
						fstreamDTL.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
				
			}
		}
	}
	
	/**
	 * 
	 * @param fileName
	 * @param fileType
	 */
	public static void readRouteData(String dirNameTL, String dirNameDTL, String dirNameNA) {
		FileInputStream fstreamNA = null;
		FileInputStream fstreamTL = null;
		FileInputStream fstreamDTL = null;

		if (dirNameNA != "") {
			final File dirNA = new File(dirNameNA);
			for (final File fileEntry : dirNA.listFiles()) {
				try {
					fstreamNA = new FileInputStream(fileEntry.getAbsolutePath());
					
					BufferedReader brNA = new BufferedReader(new InputStreamReader(fstreamNA));
	
					String lineNA, prevLine = "";
					
					Long carId = Long.parseLong(fileEntry.getName()
							.substring(fileEntry.getName().lastIndexOf("_") + 1, fileEntry.getName().lastIndexOf(".txt"))); 
					
					Long timeReachDestNA = 0l;
					Double avgSpeedNA = 0d;
					Double avgFuelNA = 0d;
					int countNA = 0;
					CarData carData = new CarData();
					if (carsData.containsKey(carId)) {
						carData = carsData.get(carId);
					}
					
						while ( (lineNA = brNA.readLine()) != null) {
							if (lineNA.equals(prevLine))
								continue;
							StringTokenizer stNA = new StringTokenizer(lineNA, " ", false);
							stNA.nextToken(); // route id
							countNA++;
							if (countNA <= carData.getMinNoRoutes()) {
								timeReachDestNA += Long.parseLong(stNA.nextToken());
								avgSpeedNA += Double.parseDouble(stNA.nextToken());
								avgFuelNA += Double.parseDouble(stNA.nextToken());
							}
							prevLine = lineNA;
						}
					
	
					if ( countNA > 0) {
						
						avgSpeedNA = avgSpeedNA/carData.getMinNoRoutes();
						avgFuelNA = avgFuelNA/carData.getMinNoRoutes();
						
						carData.setNoRoutesNA(countNA);
						carData.setTime(DataType.NoTrafficLights, timeReachDestNA);
						carData.setFuel(DataType.NoTrafficLights, avgFuelNA);
						carData.setSpeed(DataType.NoTrafficLights, avgSpeedNA);
		
						carsData.put(carId, carData);
					}
					brNA.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				} finally {
					try {
						fstreamNA.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		}
			if (dirNameDTL != "") {
				final File dirDTL = new File(dirNameDTL);
				for (final File fileEntry : dirDTL.listFiles()) {
					try {
						fstreamDTL = new FileInputStream(fileEntry.getAbsolutePath());
						BufferedReader brDTL = new BufferedReader(new InputStreamReader(fstreamDTL));
	
						String lineDTL, prevLine = "";
						
						Long carId = Long.parseLong(fileEntry.getName()
								.substring(fileEntry.getName().lastIndexOf("_") + 1, fileEntry.getName().lastIndexOf(".txt"))); 
						
						Long timeReachDestDTL = 0l;
						Double avgSpeedDTL = 0d;
						Double avgFuelDTL = 0d;
						int countDTL = 0;
						CarData carData = new CarData();
						if (carsData.containsKey(carId)) {
							carData = carsData.get(carId);
						}
	
							while ((lineDTL = brDTL.readLine()) != null) {	
								if (lineDTL.equals(prevLine))
									continue;
								StringTokenizer stDTL = new StringTokenizer(lineDTL, " ", false);
								stDTL.nextToken(); // route id
								countDTL++;
								if (countDTL <= carData.getMinNoRoutes()) {
									timeReachDestDTL += Long.parseLong(stDTL.nextToken());
									avgSpeedDTL += Double.parseDouble(stDTL.nextToken());
									avgFuelDTL += Double.parseDouble(stDTL.nextToken());
								}
								prevLine = lineDTL;
							}
	
						if ( countDTL > 0) {
							
							avgSpeedDTL = avgSpeedDTL/carData.getMinNoRoutes();
							avgFuelDTL = avgFuelDTL/carData.getMinNoRoutes();
							
							carData.setNoRoutesDTL(countDTL);
							carData.setTime(DataType.WithDynamicTrafficLights, timeReachDestDTL);
							carData.setFuel(DataType.WithDynamicTrafficLights, avgFuelDTL);
							carData.setSpeed(DataType.WithDynamicTrafficLights, avgSpeedDTL);
			
							carsData.put(carId, carData);
						}
						brDTL.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					} finally {
						try {
							fstreamDTL.close();
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				}
			}
				
				if (dirNameTL != "") {
					final File dirTL = new File(dirNameTL);
					for (final File fileEntry : dirTL.listFiles()) {
						try {
							fstreamTL = new FileInputStream(fileEntry.getAbsolutePath());
							
							BufferedReader brTL = new BufferedReader(new InputStreamReader(fstreamTL));
	
							String lineTL, prevLine = "";		
							
							Long carId = Long.parseLong(fileEntry.getName()
									.substring(fileEntry.getName().lastIndexOf("_") + 1, fileEntry.getName().lastIndexOf(".txt"))); 
							
							Long timeReachDestTL = 0l;
							Double avgSpeedTL = 0d;
							Double avgFuelTL = 0d;
							int countTL = 0;
							CarData carData = new CarData();
							if (carsData.containsKey(carId)) {
								carData = carsData.get(carId);
							}
							
								while ((lineTL = brTL.readLine()) != null) {
									if (lineTL.equals(prevLine))
										continue;
									StringTokenizer stTL = new StringTokenizer(lineTL, " ", false);
									stTL.nextToken(); // route id
									countTL++;
									if (countTL <= carData.getMinNoRoutes()) {
										timeReachDestTL += Long.parseLong(stTL.nextToken());
										avgSpeedTL += Double.parseDouble(stTL.nextToken());
										avgFuelTL += Double.parseDouble(stTL.nextToken());
									}
									prevLine = lineTL;
								}
							
							if (countTL > 0) {
								avgSpeedTL = avgSpeedTL/carData.getMinNoRoutes();
								avgFuelTL = avgFuelTL/carData.getMinNoRoutes();

								carData.setNoRoutesTL(countTL);
								carData.setTime(DataType.WithTrafficLights, timeReachDestTL);
								carData.setFuel(DataType.WithTrafficLights, avgFuelTL);
								carData.setSpeed(DataType.WithTrafficLights, avgSpeedTL);
				
								carsData.put(carId, carData);
							}
							brTL.close();
						} catch (IOException ex) {
							ex.printStackTrace();
						} finally {
							try {
								fstreamTL.close();
							} catch (IOException ex) {
								ex.printStackTrace();
							}
						}
					}
				}	
	}
	
	/**
	 * 
	 * @param city
	 */
	public static void writeCarsAggregateDataNATL(String city) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(city + "/CarsDataNAvsTL.txt", "UTF-8");
			writer.println("carId "
					+ " MinNoRoutes NAnoRoutes TLnoRoutes DTLnoRoutes"
					+ " NAtimeReachDest(sec) TLtimeReachDest(sec) DTLtimeReachDest(sec)"
					+ " NAavgSpeed(km/h) TLavgSpeed(km/h) DTLavgSpeed(km/h)"
					+ " NAavgFuel(L/h) TLavgFuel(L/h) DTLavgFuel(L/h)");
			/* Write data about cars that reached destination when no traffic light was used */
			for( Map.Entry<Long, CarData> entry : carsData.entrySet() )
			{
				if ( entry.getValue().getNoRoutesTL() > 0 && entry.getValue().getNoRoutesNA() > 0) {
					writer.println(entry.getKey() + " " + entry.getValue().toString());	
				}
			}
			writer.println("");
			/* Write data about cars that reached destination when no traffic light was used */
			for( Map.Entry<Long, CarData> entry : carsData.entrySet() )
			{
				if ( entry.getValue().getNoRoutesTL() == 0 && entry.getValue().getNoRoutesNA() > 0)
					writer.println(entry.getKey() + " " + entry.getValue().toString());	
			}
			writer.close();

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}				
	}
	
	/**
	 * 
	 * @param city
	 */
	public static void writeCarsAggregateDataTLDTL(String city) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(city + "/CarsDataTLvsDTL.txt", "UTF-8");
			writer.println("carId "
					+ " MinNoRoutes NAnoRoutes TLnoRoutes DTLnoRoutes"
					+ " NAtimeReachDest(sec) TLtimeReachDest(sec) DTLtimeReachDest(sec)"
					+ " NAavgSpeed(km/h) TLavgSpeed(km/h) DTLavgSpeed(km/h)"
					+ " NAavgFuel(L/h) TLavgFuel(L/h) DTLavgFuel(L/h)");
			for( Map.Entry<Long, CarData> entry : carsData.entrySet() )
			{
				if ( entry.getValue().getNoRoutesTL() > 0 && entry.getValue().getNoRoutesDTL() > 0
						&& entry.getValue().getSpeed().getAvgDTL() > 0 && entry.getValue().getSpeed().getAvgTL() > 0)
					writer.println(entry.getKey() + " " + entry.getValue().toString());	
			}
			writer.println("");
			
			for( Map.Entry<Long, CarData> entry : carsData.entrySet() )
			{
				if ( entry.getValue().getNoRoutesTL() == 0 && entry.getValue().getNoRoutesDTL() > 0
						&& entry.getValue().getSpeed().getAvgDTL() > 0)
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
