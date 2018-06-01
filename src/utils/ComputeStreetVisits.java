package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

public class ComputeStreetVisits {

	public static List<Pair<Long, Integer>> computeStreetVisits(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		HashMap<Long, Integer> roadData = new HashMap<Long, Integer>();

		String line;
		while ((line = br.readLine()) != null) {
			Long wayId = Long.parseLong(line);
			if (roadData.containsKey(wayId)) {
				roadData.put(wayId, roadData.get(wayId) + 1);
			} else {
				roadData.put(wayId, 1);
			}
		}
		br.close();
		return ComputeUtils.sortHashMap(roadData);
	}

	public static HashMap<Long, Integer> getStreetVisits(String filename) throws IOException {
		HashMap<Long, Integer> result = new HashMap<Long, Integer>();
		BufferedReader br = new BufferedReader(new FileReader(filename));
		String line;
		while ((line = br.readLine()) != null) {
			String[] tokens = line.split(" ");
			Long wayId = Long.parseLong(tokens[0]);
			Integer visits = Integer.parseInt(tokens[1]);
			result.put(wayId, visits);
		}
		br.close();
		return result;
	}
	
	public static void main(String[] args) throws IOException {
		List<Pair<Long, Integer>> streetVisits = computeStreetVisits("trunk\\traces\\SanFranciscoRouteTimeData\\road_visits.txt");
		
//		PrintWriter pw = new PrintWriter("traces\\BeijingRouteTimeData\\road_visits_sorted.txt");
		PrintWriter pw = new PrintWriter("trunk\\traces\\SanFranciscoRouteTimeData\\road_visits_sorted.txt");
		for (Pair<Long, Integer> pair : streetVisits) {
			pw.println(pair.getFirst() + " " + pair.getSecond());
		}
		pw.close();
	}
}
