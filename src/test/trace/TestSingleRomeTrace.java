package test.trace;


import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.io.RandomAccessFile;
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
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

import controller.newengine.EngineUtils;


public class TestSingleRomeTrace extends JFrame {

	private static final long serialVersionUID = 1L;
	private JMapViewer map;
	MapConfig mapConfig;
	
	public TestSingleRomeTrace() {
		super("TestSingleRomeTrace");
		setLayout(new OverlayLayout());
		
		map = new JMapViewer();
		//41.9072957658043 12.4894718722508
		map.setDisplayPositionByLatLon(41.9072957658043, 12.4894718722508, 11);
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
	
	public void addCab(String data) throws Exception {
	
		RandomAccessFile raf = new RandomAccessFile(data, "r");
		String line;
		Color c = new Color(0, 255, 0);
		int count = 0;
		while ((line = raf.readLine()) != null) {
			
			String[] props = line.split(" ");
			
			Double pointX = Double.parseDouble(props[0]);
			Double pointY = Double.parseDouble(props[1]);
			
			
			MapMarkerDot mkd = new MapMarkerDot(pointX, pointY);
			mkd.setBackColor(c);
			mkd.setColor(c);
			map.getMapMarkerList().add(mkd);
			
			

			count++;
			if (count >= 300)
				break;
		}
		
		map.repaint();
		
		raf.close();
	}

	public static void main(String[] args) throws Exception {
		TestSingleRomeTrace frame = new TestSingleRomeTrace();
		frame.setVisible(true);
		frame.addCab("res\\res\\romecabs\\22.txt");
	}
}
