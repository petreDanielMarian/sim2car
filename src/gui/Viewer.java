package gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.SwingUtilities;

import model.GeoCar;
import model.GeoServer;
import model.GeoTrafficLightMaster;
import model.MapPoint;
import model.OSMgraph.Node;
import model.OSMgraph.Way;
import model.mobility.MobilityEngine;
import model.parameters.Globals;
import model.parameters.MapConfig;
import utils.TraceParsingTool;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;

/**
 * Class to deal with visualising the simulation.
 * TODO: Move all GUI stuff from the EngineSimulation in here.
 *
 */
public class Viewer {
	private View view;
	
	private JMapViewer mapJ;
	/**
	 * The server view for all the geo servers
	 */
	private ServerView serverView;
	private List<CarView> carViewList = new ArrayList<CarView>();
	private TreeMap<Long, GeoTrafficLightMaster> trafficLightMasterList = new TreeMap<Long, GeoTrafficLightMaster>();
	
	private TreeMap<Long, Vector<GeoTrafficLightMaster>> trafficLightMasterByStreets = new TreeMap<Long, Vector<GeoTrafficLightMaster>>();
	
	
	public Viewer(final MapConfig mapConfig) {
		if (Globals.showGUI) {
			mapJ = new JMapViewer();
			mapJ.setDisplayPositionByLatLon(37.79805179347195, -122.27509081363, 11);
			serverView = new ServerView(mapConfig.getN(), mapConfig.getM(), new ArrayList<GeoServer>(), mapJ);
			view = new View(mapConfig.getN(), mapConfig.getM(), mapJ, serverView, carViewList, trafficLightMasterList);
		}
	}

	public void addCar(GeoCar car) {
		if (Globals.showGUI) {
			CarView carView = new CarView(car.getId(), mapJ, car);
			carView.setColor(new Color( (float) Math.random(),
										(float) Math.random(),
										(float) Math.random()) );
			carViewList.add(carView);
		}
	}
	
	public void addServers(ArrayList<GeoServer> servers) {
		if (Globals.showGUI) {
			view.initLocationServer(servers);
		}
	}
	
	/** Creates a traffic light view on the map for the given node*/
	public void createTrafficLightView(GeoTrafficLightMaster trafficLightMaster, Node node, Long wayId, Color color) {
		TrafficLightView trafficLightView;
		MobilityEngine mobilityEngine;
		MapPoint newPoint;
		Node prevNode, centerNode;
		Way way;
		boolean oneway;
		int nodeIndex;
		int direction;
		double distanceFromCenter, distanceBetweenNodes;
		
		/* save a copy of the intersection node */
		centerNode = node;
		distanceFromCenter = 14;
		mobilityEngine = MobilityEngine.getInstance();
		way = mobilityEngine.getWay(wayId);
		newPoint = MapPoint.getMapPoint(node);
		
		nodeIndex = mobilityEngine.getSegmentNumber(node);
		newPoint.segmentIndex = nodeIndex;
		
		/* If it is the beginning of the street, a traffic light is useless */
		if (newPoint.segmentIndex > 0)
			prevNode = way.getNodeByIndex(newPoint.segmentIndex - 1);
		else
			return;
		
		
		/* Add a traffic light between the center of the intersection and the previous node 
		 * distance - distance from the center of the intersection to previous node*/
		Node aux = prevNode;
		prevNode = node;
		node = aux;
		
		distanceBetweenNodes = TraceParsingTool.distance(prevNode.lat, prevNode.lon, node.lat, node.lon);
		newPoint.lat = (node.lat - prevNode.lat) * distanceFromCenter / distanceBetweenNodes + prevNode.lat;
		newPoint.lon = (node.lon - prevNode.lon) * distanceFromCenter / distanceBetweenNodes + prevNode.lon;
		
		newPoint = MapPoint.getMapPoint(newPoint.lat, newPoint.lon, true, prevNode.wayId);
		newPoint.segmentIndex = mobilityEngine.getSegmentNumber(prevNode);
		newPoint.cellIndex = mobilityEngine.getCellIndex(node, node, newPoint);
		
		direction = mobilityEngine.getDirection(prevNode, node);
				
		trafficLightView = new TrafficLightView(mapJ, newPoint, wayId, direction);
		trafficLightView.setColor(color);
		
		trafficLightMaster.addTrafficLightView(trafficLightView);
		
		
		/* If the street has 2 ways then add a traffic light on the opposite lane of the street */
		oneway = way.getDirection();

		if (!oneway && nodeIndex < way.nodes.size() - 1) {
			node = centerNode;
			prevNode = way.getNodeByIndex(nodeIndex + 1);
			newPoint = MapPoint.getMapPoint(prevNode);
			
			node = prevNode;
			prevNode = centerNode;
			distanceBetweenNodes = TraceParsingTool.distance(prevNode.lat, prevNode.lon, node.lat, node.lon);
			newPoint.lat = (node.lat - prevNode.lat) * distanceFromCenter / distanceBetweenNodes + prevNode.lat;
			newPoint.lon = (node.lon - prevNode.lon) * distanceFromCenter / distanceBetweenNodes + prevNode.lon;
			
			newPoint = MapPoint.getMapPoint(newPoint.lat, newPoint.lon, true, prevNode.wayId);
			MapMarkerDot lastMk = new MapMarkerDot(newPoint.lat, newPoint.lon);
			lastMk.setBackColor(Color.yellow);
			lastMk.setColor(Color.yellow);
			mapJ.addMapMarker(lastMk);
			
			direction = - direction;
			
			trafficLightView = new TrafficLightView(mapJ, newPoint, wayId, direction);
			trafficLightView.setColor(color);
			trafficLightMaster.addTrafficLightView(trafficLightView);
		}
		
	}
	
	
	/** Adds traffic light views on the map for each road on intersection */
	public void addTrafficLightViews(GeoTrafficLightMaster trafficLightMaster) {
		MobilityEngine mobilityEngine;
		Way way, wayNeigh;
		Node node, nodeNeigh;
		int nodeIndex, nodeNeighIndex;
		Color trafficLightColor = Color.red;
		
		if (Globals.showGUI) {
			
			mobilityEngine = MobilityEngine.getInstance();
			way = mobilityEngine.getWay(trafficLightMaster.getNode().wayId);
			nodeIndex = way.getNodeIndex(trafficLightMaster.getNode().id);
			node = trafficLightMaster.getNode();
			
			/* Check if the intersection has double traffic light control */
			boolean doubleIntersection = false;
			GeoTrafficLightMaster masterTrafficLightDoubleOriginal = null;
			
			if (trafficLightMasterByStreets.get(way.id) != null) {
				for (GeoTrafficLightMaster master : trafficLightMasterByStreets.get(way.id)) {
					double distance = TraceParsingTool.distance(master.getNode().lat, master.getNode().lon, node.lat, node.lon);
					if (distance < 20) {
						doubleIntersection = true;
						masterTrafficLightDoubleOriginal = trafficLightMasterList.get(master.getId());
						break;
					}
				}
			}
			
			/* Check if current node has already a traffic light view associated */
			boolean notDoubled = true;
			if (doubleIntersection && masterTrafficLightDoubleOriginal != null) {
				for (TrafficLightView trafficLightViewOriginal : masterTrafficLightDoubleOriginal.getTrafficLights()) {
					if (trafficLightViewOriginal.getWayId() == trafficLightMaster.getNode().getWayId()) {
						notDoubled = false;
						break;
					}
				}
			}
			/* Add a traffic light view */
			if (doubleIntersection) {
				if (notDoubled)
					createTrafficLightView(masterTrafficLightDoubleOriginal, way.getNodeByIndex(nodeIndex), 
							trafficLightMaster.getNode().getWayId(), trafficLightColor);
			}
			else {
				createTrafficLightView(trafficLightMaster, way.getNodeByIndex(nodeIndex), 
						trafficLightMaster.getNode().getWayId(), trafficLightColor);				
			}
					
			
			Vector<Long> wayNeighs = way.neighs.get(trafficLightMaster.getNode().id);
			if (wayNeighs == null)
				return;
			
			/* Take every neighbor street and add a traffic light view for it for the current intersection*/
			for (Long wayIdNeigh : wayNeighs) {
				wayNeigh =  mobilityEngine.getWay(wayIdNeigh);
				nodeNeighIndex = wayNeigh.getNodeIndex(trafficLightMaster.getNode().id);
				nodeNeigh = wayNeigh.getNodeByIndex(nodeNeighIndex);
				
				/* Check if current node has already a traffic light view associated */
				notDoubled = true;
				if (doubleIntersection && masterTrafficLightDoubleOriginal != null) {
					for (TrafficLightView trafficLightViewOriginal : masterTrafficLightDoubleOriginal.getTrafficLights()) {
						if (trafficLightViewOriginal.getWayId() == wayIdNeigh) {
							notDoubled = false;
							break;
						}
					}
				}
				
				/* set color for the new traffic light */
				if (trafficLightColor == Color.red)
					trafficLightColor = Color.green;
				else
					trafficLightColor = Color.red;
				
				/* Add a traffic light view */
				if (doubleIntersection) {
					if (notDoubled)
						createTrafficLightView(masterTrafficLightDoubleOriginal, nodeNeigh, wayIdNeigh, trafficLightColor);
				}
				else {
					createTrafficLightView(trafficLightMaster, nodeNeigh, wayIdNeigh, trafficLightColor);
				}
			}
			if (trafficLightMasterByStreets.get(way.id) == null) {
				trafficLightMasterByStreets.put(way.id, new Vector<>());
			}
			if (!doubleIntersection) {
				trafficLightMasterByStreets.get(way.id).add(trafficLightMaster);
				trafficLightMaster.setIntersectionType(trafficLightMaster.getTrafficLights().size());
				trafficLightMasterList.put(trafficLightMaster.getId(), trafficLightMaster);
				//setTrafficLightColors(trafficLightMaster);
			}
		}
	}
	public void showView() {
		if (Globals.showGUI) {
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					view.showView();
				}
			});
		}
	}
	
	public void setTime(final String time) {
		if (Globals.showGUI) {
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					view.setTimer(time);
				}
			});
		}
	}

	public void updateCarPositions() {
		if (Globals.showGUI) {
			for (CarView car : carViewList) {
				car.updateCarView();
			}
			view.repaint();
		}
	}
	
	public void updateTrafficLightsColors() {
		if (Globals.showGUI) {
			for (GeoTrafficLightMaster trafficLightMaster : trafficLightMasterList.values()) {
				long simulationTime = view.getTimer().equals("") ? 0 : Long.parseLong(view.getTimer());
				trafficLightMaster.updateTrafficLightViews(simulationTime);
			}
			view.repaint();
		}
	}
}