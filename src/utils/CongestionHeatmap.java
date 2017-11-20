package utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import model.OSMgraph.Node;
import model.OSMgraph.Way;
import model.parameters.MapConfig;
import model.parameters.MapConfiguration;

import org.jfree.ui.OverlayLayout;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerCircle;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.MapRectangleImpl;

import sun.util.locale.StringTokenIterator;
import controller.newengine.EngineUtils;

public class CongestionHeatmap extends JFrame {
	private static final long serialVersionUID = 1L;
	private JMapViewer map;
	static MapConfig mapConfig;
	static TreeMap<Long,Way> streetsGraph;
	public static List<Map.Entry<Pair<Long,Long>, Pair<Long,Double>>> readCongestionStatistics( String fname )
	{
		TreeMap<Pair<Long,Long>, Pair<Long,Double>> statistics = new TreeMap<Pair<Long,Long>, Pair<Long,Double>>();
		
		try {
			BufferedReader in = new BufferedReader( new FileReader(fname));
			String line = null;
			while( (line = in.readLine()) != null )
			{
				StringTokenizer st = new StringTokenizer( line, "->(): ");
				long wayId1 = Long.parseLong(st.nextToken());
				long jointId = Long.parseLong(st.nextToken());
				long wayId2 = Long.parseLong(st.nextToken());
				double g = Double.valueOf(st.nextToken());
				
						
				//System.out.println(wayId1 + " "+jointId + " "+ wayId2 +" " +g );
				Way way1 = streetsGraph.get(wayId1);
				Integer idx = way1.getNodeIndex(jointId);
				if( idx == -1 )
				{
					continue;
				}
				for( int i = idx - 1; i >= 0; i-- )
				{
					Node nd = way1.nodes.get(i);
					if( way1.neighs.get( nd.id) != null )
					{
						statistics.put( new Pair<Long,Long>(nd.id, jointId), new Pair<Long, Double>(wayId1, g));
						break;
					}
				}
				
				if( !way1.oneway )
				{
					for( int i = idx + 1; i < way1.nodes.size(); i++ )
					{
						Node nd = way1.nodes.get(i);
						if( way1.neighs.get( nd.id) != null )
						{
							statistics.put( new Pair<Long,Long>(nd.id, jointId), new Pair<Long, Double>(wayId1, g));
							break;
						}
					}
				}
				
			}
		} catch (FileNotFoundException e) {
			System.out.println("File can not be opened " + fname );
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Impossible to read from " + fname );
			e.printStackTrace();
		}
		
		ArrayList<Map.Entry<Pair<Long,Long>, Pair<Long,Double>>> sortedStatistics = new ArrayList<Map.Entry<Pair<Long,Long>, Pair<Long,Double>>>(statistics.entrySet());
		Collections.sort( sortedStatistics, new Comparator<Map.Entry<Pair<Long,Long>, Pair<Long,Double>>>() {

			@Override
			public int compare(Entry<Pair<Long, Long>, Pair<Long, Double>> o1,
					Entry<Pair<Long, Long>, Pair<Long, Double>> o2) {
				double diff = o1.getValue().getSecond() - o2.getValue().getSecond();
				if( diff == 0 || Math.abs(diff) < Math.pow(10, -10))
						return o1.getKey().compareTo(o2.getKey());
				if( diff < 0 )
					return -1;
				else
					return 1;
			}
		});
		return sortedStatistics;
		
	}
	public CongestionHeatmap() {
		super("CongestionHeatmap");
		setLayout(new OverlayLayout());
		mapConfig = MapConfiguration.getInstance("e:/Cursuri/master/cercetare/Sim2Car/trunk/src/configurations/simulator/sanfrancisco.properties");
		map = new JMapViewer();
		//map_lat_min 39.4140061 map_lon_m;;;in 115.6860661 map_lat_max 40.426 map_lon_max 117.1189678
		map.setDisplayPositionByLatLon(mapConfig.getMapCentre().getY(), mapConfig.getMapCentre().getX(), 9);
		map.setZoomContolsVisible(true);
//		MapMarkerCircle alphaLayer = new MapMarkerCircle(
//				new Coordinate(mapConfig.getMapCentre().getX(), mapConfig.getMapCentre().getY()), 1);
//		alphaLayer.setBackColor(new Color(255, 255, 255, 200));
//		alphaLayer.setColor(new Color(255, 255, 255, 200));
//		MapRectangleImpl rectangle = new MapRectangleImpl( new Coordinate(41.9305783,12.4169552), new Coordinate(41.8447224, 12.5515482 ));
//		rectangle.setBackColor(new Color(255, 255, 255, 200));
//		rectangle.setColor(new Color(255, 255, 255, 200));
//		map.addMapRectangle(rectangle);
//		
//		Coordinate one = new Coordinate(37.801127,-122.506656);
//		Coordinate two = new Coordinate(37.7355746, -122.37 );
//		
//
//		MapPolygonImpl line = new MapPolygonImpl(new Vector<Coordinate>(Arrays.asList(one, two, two)));
//		line.setStroke(new BasicStroke(5));
//		line.setColor(new Color(255, 255, 0));
//		
//		map.addMapPolygon(line);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(900, 900);
		setLocation(100, 100);
		add(map);
	}
	
	public void addData(final List<Map.Entry<Pair<Long,Long>, Pair<Long,Double>>> data ) {

		double r=255;
		for (Map.Entry<Pair<Long,Long>, Pair<Long,Double>> entry : data) {
			Pair<Long,Long> key = entry.getKey();
			Pair<Long,Double> value = entry.getValue();
			double g = value.getSecond();

			if( g == 0 )
				continue;
			double green = 255 - g * 255;
			if (green > 230)
			{
				r = 0;
			}
			else
			{
				r = 255;
			}
			Color color = new Color((int)r, (int) green, 0);
			Way w = streetsGraph.get(value.getFirst());
			int idx1 = w.getNodeIndex(key.getFirst()), idx2 = w.getNodeIndex(key.getSecond());

			for( int i = Math.min(idx1, idx2); i <  Math.max(idx1, idx2); i++)
			{
				Node crt = w.nodes.get(i);
				Node next = w.nodes.get(i+1);
				Coordinate one = new Coordinate(crt.lat, crt.lon);
				Coordinate two = new Coordinate(next.lat, next.lon);

				MapPolygonImpl line = new MapPolygonImpl(new Vector<Coordinate>(Arrays.asList(one, two, two)));
				line.setStroke(new BasicStroke(5));
				line.setColor(color);

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						map.addMapPolygon(line);
					}
				});
			

			}
			
		}
	}

	public static void main(String[] args) throws IOException {
		CongestionHeatmap frame = new CongestionHeatmap();
		streetsGraph = EngineUtils.loadGraph(mapConfig.getStreetsFilename(),
					 											mapConfig.getPartialGraphFilename());
		//List<Map.Entry<Pair<Long,Long>, Pair<Long,Double>>> sortedStatistics = readCongestionStatistics( "e:/Cursuri/master/cercetare/Sim2Car/RoutingCongestionStatistics/rome/second_injected/streetcostGraph_617_7198.txt" );
		//List<Map.Entry<Pair<Long,Long>, Pair<Long,Double>>> sortedStatistics = readCongestionStatistics( "e:/Cursuri/master/cercetare/Sim2Car/RoutingCongestionStatistics/rome/congpr/streetcostGraph_617_7198.txt" );
		//List<Map.Entry<Pair<Long,Long>, Pair<Long,Double>>> sortedStatistics = readCongestionStatistics( "e:/Cursuri/master/cercetare/Sim2Car/RoutingCongestionStatistics/rome/onlypr/streetcostGraph_617_7198.txt" );
		List<Map.Entry<Pair<Long,Long>, Pair<Long,Double>>> sortedStatistics = readCongestionStatistics( "e:/Cursuri/master/cercetare/Sim2Car/RoutingCongestionStatistics/sanfrancisco/second_injected/streetcostGraph_837_7198.txt" );
		//List<Map.Entry<Pair<Long,Long>, Pair<Long,Double>>> sortedStatistics = readCongestionStatistics( "e:/Cursuri/master/cercetare/Sim2Car/RoutingCongestionStatistics/sanfrancisco/congpr/streetcostGraph_837_7198.txt" );
		//List<Map.Entry<Pair<Long,Long>, Pair<Long,Double>>> sortedStatistics = readCongestionStatistics( "e:/Cursuri/master/cercetare/Sim2Car/RoutingCongestionStatistics/sanfrancisco/congprrd/streetcostGraph_837_7198.txt" );
		frame.setVisible(true);
		frame.addData(sortedStatistics);
	}
}