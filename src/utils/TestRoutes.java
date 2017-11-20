package utils;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;

import controller.newengine.EngineUtils;

public class TestRoutes extends JFrame {
	private static final long serialVersionUID = 1L;
	private JMapViewer map;
	MapConfig mapConfig;
	
	public TestRoutes() {
		super("Heatmap");
		setLayout(new OverlayLayout());
		mapConfig = MapConfiguration.getInstance("e:/Cursuri/master/cercetare/Sim2Car/trunk/src/rome.properties");
		map = new JMapViewer();
		//map_lat_min 39.4140061 map_lon_min 115.6860661 map_lat_max 40.426 map_lon_max 117.1189678
		map.setDisplayPositionByLatLon(mapConfig.getMapCentre().getY(), mapConfig.getMapCentre().getX(), 10);
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
	
	public void addData( ) {
		
		FileInputStream fstream;
		try {
			//fstream = new FileInputStream( "e:/Cursuri/master/cercetare/Sim2Car/phase11.txt" );
			fstream = new FileInputStream( "e:/Cursuri/master/cercetare/Sim2Car/trunk/traces/CorectedRome/100.txt" );
			
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String line;
			int idx = 0;
			while ( (line = br.readLine()) != null )
			{
				if( line.startsWith("#"))
					continue;
				String [] ws = line.split(" ");
				double lat = Double.parseDouble(ws[1]);
				double lon = Double.parseDouble(ws[2]);
				if( mapConfig.getCity().contains("beijing") )
				{

					lat = Double.parseDouble(ws[1]);
					lon = Double.parseDouble(ws[2]);
					idx = Integer.parseInt(ws[0]);
				}
				else if( mapConfig.getCity().contains("rome") )
					{	
						lat = Double.parseDouble(ws[1]);
						lon = Double.parseDouble(ws[0]);
					}
				
				
		
					MapMarkerDot lastMk = new MapMarkerDot(lat, lon);
					lastMk.setBackColor(Color.BLUE);
					lastMk.setColor(Color.BLUE);
					map.addMapMarker(lastMk);
					
					if( idx++ > 1000 )
						break;

			}
			br.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

public void addOrigData( ) {
		
		FileInputStream fstream;
		try {
			//fstream = new FileInputStream( "e:/Cursuri/master/cercetare/Sim2Car/trunk/res/res/beijingcabs/1.txt" );
			fstream = new FileInputStream( "e:/Cursuri/master/cercetare/Sim2Car/trunk/res/res/romecabs/100.txt" );
			
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String line;
			int idx = 0;
			while ( (line = br.readLine()) != null )
			{

//				if(idx > 3)
//					break;
  			   // if(idx == 3){
					double lat = 0, lon = 0;
					if( mapConfig.getCity().contains("beijing") )
					{
						String [] ws = line.split(",");
						lat = Double.parseDouble(ws[3]);
						lon = Double.parseDouble(ws[2]);
					}
					else if( mapConfig.getCity().contains("rome") )
						{	
							String [] ws = line.split(",");
							lat = Double.parseDouble(ws[1]);
							lon = Double.parseDouble(ws[2]);
						}
  			    	
					MapMarkerDot lastMk = new MapMarkerDot(lat, lon);
					lastMk.setBackColor(Color.RED);
					lastMk.setColor(Color.RED);
					map.addMapMarker(lastMk);
  			  //  }
				
  			    idx++;


			}
			br.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	public static void main(String[] args) throws IOException {
		TestRoutes frame = new TestRoutes();

		frame.setVisible(true);
		//frame.addOrigData();
		frame.addData();
		
	}	
}