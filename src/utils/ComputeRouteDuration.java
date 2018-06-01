package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ComputeRouteDuration {
	
	public static HashMap<Double, ArrayList<Double>> readRouteLengthData(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		HashMap<Double, ArrayList<Double>> data = new HashMap<Double, ArrayList<Double>>();

		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("<")) {
				String[] tokens = line.split(" ");
				if (tokens.length != 6)
					continue;
				ArrayList<Double> route = new ArrayList<Double>();
				route.add(Double.parseDouble(tokens[1]));	// start lat
				route.add(Double.parseDouble(tokens[2]));	// start lon
				route.add(Double.parseDouble(tokens[3]));	// end lat
				route.add(Double.parseDouble(tokens[4]));	// end lon
				route.add((double) Integer.parseInt(tokens[5]));	// duration
				data.put(route.get(0), route);
			}
		}
		br.close();
		return data;
	}
	
	public static List<List<Long>> computeRouteDataCar(String traceFile, String normalFile, String enhancedFile) throws IOException {
		List<List<Long>> routeData = new ArrayList<List<Long>>();
		HashMap<Double, ArrayList<Double>> tracesData = readRouteLengthData(traceFile);
		HashMap<Double, ArrayList<Double>> normalData = readRouteLengthData(normalFile);
		HashMap<Double, ArrayList<Double>> enhancedData = readRouteLengthData(enhancedFile);

		Iterator<Double> it = tracesData.keySet().iterator();
		while (it.hasNext()) {
			double startLat = it.next();
			if (!normalData.containsKey(startLat) || !enhancedData.containsKey(startLat))
				continue;
			double normalDuration = normalData.get(startLat).get(4);
			double enhancedDuration = enhancedData.get(startLat).get(4);
			double traceDuration = tracesData.get(startLat).get(4);
			
			ArrayList<Long> times = new ArrayList<Long>();
			times.add((long)traceDuration);
			times.add((long)normalDuration);
			times.add((long)enhancedDuration);
			routeData.add(times);
		}
		return routeData;
	}
	
	public static void computeRouteData(String folder) throws IOException {
		List<List<Long>> routeData = new ArrayList<List<Long>>();
		
		File dir = new File(folder);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				if (!child.isFile() || !child.getName().startsWith("joints_"))
					continue;
				String jointsFilename = child.getCanonicalPath();
				String id = child.getName().substring(7, child.getName().length() - 4);
				String normalFilename = "normal_" + id + ".txt";
				String enhancedFilename = "enhanced_" + id + ".txt";
				routeData.addAll(computeRouteDataCar(jointsFilename, normalFilename, enhancedFilename));
			}
		}
	}

	public static void main(String[] args) throws IOException {

		//PrintWriter pw = new PrintWriter("traces\\BeijingRouteTimeData\\route_data_all_3.txt");
		PrintWriter pw = new PrintWriter("trunk\\traces\\SanFranciscoRouteTimeData\\route_data_all_3.txt");
		for (int i = 0; i < 100; i++) {
			if (i == 6 || i == 7 || i == 12 || i == 21 || i == 35 || i == 33 || i == 36 || i == 39 || i == 42 || i == 44 || i == 46 || i == 51 || i == 66 || i == 78 || i == 90 || i == 92 || i == 47)
				continue;
			List<List<Long>> routeData = computeRouteDataCar(
					"trunk\\traces\\SanFranciscoRouteTimeData\\routes_" + i + ".txt",
					"trunk\\traces\\SanFranciscoRouteTimeData\\normal_" + i + ".txt",
					"trunk\\traces\\SanFranciscoRouteTimeData\\enhanced_" + i + ".txt");
			for (List<Long> list : routeData) {
				pw.println(list.get(0) + " " + list.get(1) + " " + list.get(2));
			}
		}
		pw.close();
	}
}
