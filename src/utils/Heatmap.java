package utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFrame;

import model.OSMgraph.Node;
import model.OSMgraph.Way;
import model.parameters.MapConfig;
import model.parameters.MapConfiguration;

import org.jfree.ui.OverlayLayout;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;

import controller.newengine.EngineUtils;

public class Heatmap extends JFrame {
	private static final long serialVersionUID = 1L;
	private JMapViewer map;
	MapConfig mapConfig;
	
	public Heatmap() {
		super("Heatmap");
		setLayout(new OverlayLayout());
		mapConfig = MapConfiguration.getInstance("e:/Cursuri/master/cercetare/Sim2Car/trunk/src/beijing.properties");
		map = new JMapViewer();
		//map_lat_min 39.4140061 map_lon_min 115.6860661 map_lat_max 40.426 map_lon_max 117.1189678
		map.setDisplayPositionByLatLon(mapConfig.getMapCentre().getX(), mapConfig.getMapCentre().getY(), 11);
		map.setZoomContolsVisible(true);
//		MapMarkerCircle alphaLayer = new MapMarkerCircle(
//				new Coordinate(mapConfig.getMapCentre().getX(), mapConfig.getMapCentre().getY()), 1);
//		alphaLayer.setBackColor(new Color(255, 255, 255, 200));
//		alphaLayer.setColor(new Color(255, 255, 255, 200));
//		MapRectangleImpl rectangle = new MapRectangleImpl(new Coordinate(40.426, 115.6860661), new Coordinate(39.4140061, 117.1189678));
//		rectangle.setBackColor(new Color(255, 255, 255, 200));
//		rectangle.setColor(new Color(255, 255, 255, 200));
//		map.addMapRectangle(rectangle);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(900, 900);
		setLocation(100, 100);
		add(map);
	}
	
	public void addData(final HashMap<Way, Integer> roadVisits, int maxValue) {
		List<Way> ordered = new ArrayList<Way>(roadVisits.keySet());
		Collections.sort(ordered, new Comparator<Way>() {

			@Override
			public int compare(Way o1, Way o2) {
				return roadVisits.get(o1) - roadVisits.get(o2);
				
			}
		});
		for (Way way : ordered) {
			int visits = roadVisits.get(way);
			double green = 255 - visits * 255 / maxValue;
			if (green > 200)
				continue;
			Color color = new Color(255, (int) green, 0);
			for (int i = 1; i < way.nodes.size(); i++) {
				Node prev = way.nodes.get(i-1);
				Node crt = way.nodes.get(i);
				Coordinate one = new Coordinate(prev.lat, prev.lon);
				Coordinate two = new Coordinate(crt.lat, crt.lon);
				List<Coordinate> list = new ArrayList<Coordinate>(Arrays.asList(one, two, two));
				MapPolygonImpl line = new MapPolygonImpl(list);
				line.setStroke(new BasicStroke(5));
				line.setColor(color);
				map.addMapPolygon(line);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		Heatmap frame = new Heatmap();
		TreeMap<Long,Way> streetsGraph = EngineUtils.loadGraph("e:/Cursuri/master/cercetare/Sim2Car/trunk/res\\XmlBeijing\\streets_rez_beijing.osm", "e:/Cursuri/master/cercetare/Sim2Car/trunk/res\\XmlBeijing\\streets_graph_beijing.osm");
		HashMap<Long, Integer> streetVisits = ComputeStreetVisits.getStreetVisits("e:/Cursuri/master/cercetare/Sim2Car/trunk/traces\\BeijingRouteTimeData\\road_data_sorted.txt");
		HashMap<Way, Integer> data = new HashMap<Way, Integer>();
		int maxValue = 0;
		for (Long id : streetsGraph.keySet()) {
			Way way = streetsGraph.get(id);
			Integer visits = 0;
			if (streetVisits.containsKey(id))
				visits = streetVisits.get(id);
			data.put(way, visits);
			if (visits > maxValue)
				maxValue = visits;
		}
		frame.setVisible(true);
		frame.addData(data, maxValue);
	}
}