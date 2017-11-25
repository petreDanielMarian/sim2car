package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ComputeAverageSpeeds {

	public static List<Pair<Long, Double>> computeStreetAverageSpeeds(String folder, String prefix) throws IOException {
		HashMap<Long, List<Double>> speedData = new HashMap<Long, List<Double>>();
		
		File dir = new File(folder);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				if (!child.isFile() || !child.getName().startsWith(prefix))
					continue;
				BufferedReader br = new BufferedReader(new FileReader(child));
				String line;
				while ((line = br.readLine()) != null) {
					String[] tokens = line.split(" ");
					Long wayId = Long.parseLong(tokens[0]);
					Double speed = Double.parseDouble(tokens[1]);
					if (speedData.containsKey(wayId)) {
						List<Double> speeds = speedData.get(wayId);
						speeds.add(speed);
					} else {
						List<Double> speeds = new ArrayList<Double>();
						speeds.add(speed);
						speedData.put(wayId, speeds);
					}
				}
				br.close();
			}
		}
		
		HashMap<Long, Double> result = new HashMap<Long, Double>();
		for (Long way : speedData.keySet()) {
			List<Double> speeds = speedData.get(way);
			double sum = 0.0;
			for (Double speed : speeds)
				sum += speed;
			Double avgSpeed = sum / speeds.size();
			result.put(way, avgSpeed);
		}
		return ComputeUtils.sortHashMap(result);
	}

	public static HashMap<Long, Double> getStreetSpeeds(String filename) throws IOException {
		HashMap<Long, Double> result = new HashMap<Long, Double>();
		BufferedReader br = new BufferedReader(new FileReader(filename));
		String line;
		while ((line = br.readLine()) != null) {
			String[] tokens = line.split(" ");
			Long wayId = Long.parseLong(tokens[0]);
			Double visits = Double.parseDouble(tokens[1]);
			result.put(wayId, visits);
		}
		br.close();
		return result;
	}
	
	public static void main(String[] args) throws IOException {
		//List<Pair<Long, Double>> streetSpeeds = computeStreetAverageSpeeds("traces\\InterpolateBeijing\\", "speeds_");
		List<Pair<Long, Double>> streetSpeeds = computeStreetAverageSpeeds("trunk\\traces\\InterpolateSanFrancisco\\", "speeds_");
		PrintWriter pw = new PrintWriter("road_speeds_sorted.txt");
		for (Pair<Long, Double> pair : streetSpeeds) {
			pw.println(pair.getFirst() + " " + pair.getSecond());
		}
		pw.close();
	}
}
