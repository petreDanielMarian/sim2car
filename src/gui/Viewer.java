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
import utils.tracestool.Utils;

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
	/**
	 * The master traffic lights
	 */
	private TreeMap<Long, GeoTrafficLightMaster> trafficLightMasterList = new TreeMap<Long, GeoTrafficLightMaster>();	
	
	public Viewer(final MapConfig mapConfig) {
		if (Globals.showGUI) {
			mapJ = new JMapViewer();
			mapJ.setDisplayPositionByLatLon(mapConfig.getMapCentre().getX(),
					mapConfig.getMapCentre().getY(), 11);
			serverView = new ServerView(mapConfig.getN(), mapConfig.getM(), new ArrayList<GeoServer>(), mapJ);
			view = new View(mapConfig.getN(), mapConfig.getM(), mapJ, serverView, carViewList, trafficLightMasterList);
		}
	}
	
	public JMapViewer getMapJ() {
		return this.mapJ;
	}

	public void addCar(GeoCar car) {
		//if (Globals.showGUI && car.getId() == 148) {
			CarView carView = new CarView(car.getId(), mapJ, car);
			carView.setColor(new Color( (float) Math.random(),
										(float) Math.random(),
										(float) Math.random()) );
			carViewList.add(carView);
		//}
	}
	
	public void addServers(ArrayList<GeoServer> servers) {
		if (Globals.showGUI) {
			//view.initLocationServer(servers);
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
		
		if (trafficLightMaster.getId()== 893) {
			System.out.println("here");
		}
		
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
		{
			return;
		}
		
		
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
			if (trafficLightMaster.getId()== 893) {
				System.out.println("Node index" + nodeIndex);
			}
			node = centerNode;
			prevNode = way.getNodeByIndex(nodeIndex + 1);
			newPoint = MapPoint.getMapPoint(prevNode);
			
			node = prevNode;
			prevNode = centerNode;
			distanceBetweenNodes = TraceParsingTool.distance(prevNode.lat, prevNode.lon, node.lat, node.lon);
			newPoint.lat = (node.lat - prevNode.lat) * distanceFromCenter / distanceBetweenNodes + prevNode.lat;
			newPoint.lon = (node.lon - prevNode.lon) * distanceFromCenter / distanceBetweenNodes + prevNode.lon;
			
			newPoint = MapPoint.getMapPoint(newPoint.lat, newPoint.lon, true, prevNode.wayId);
			addMapMarker(newPoint.lat, newPoint.lon, Color.yellow);
			
			direction = - direction;
			
			trafficLightView = new TrafficLightView(mapJ, newPoint, wayId, direction);
			trafficLightView.setColor(color);
			trafficLightMaster.addTrafficLightView(trafficLightView);
		}
		
	}
	
	/***
	 * Adds a map marker to the map
	 * @param lat
	 * @param lon
	 * @param color
	 */
	public void addMapMarker(double lat, double lon, Color color) {
		MapMarkerDot lastMk = new MapMarkerDot(lat, lon);
		lastMk.setBackColor(color);
		lastMk.setColor(color);
		mapJ.addMapMarker(lastMk);
	}

	/***
	 * Adds a traffic light view on the map for each way in the intersection
	 * specified by the master traffic light
	 * If the intersection already has a master traffic light, it updates that initial master
	 * with the traffic light view for each way intersecting the current traffic light master
	 * @param currentTrafficLightMaster
	 * @return true - add new master traffic light; false - update an existing master traffic
	 * light
	 */
	public boolean addTrafficLightViews(GeoTrafficLightMaster currentTrafficLightMaster) {
		Way currentWay, wayNeigh;
		Node currentNode, nodeNeigh;
		int nodeNeighIndex;
		Color trafficLightColor = Color.red;
		
		if (Globals.showGUI) {
			
			currentWay = MobilityEngine.getInstance().getWay(currentTrafficLightMaster.getNode().wayId);
			currentNode = currentTrafficLightMaster.getNode();
			
			/* Check if the intersection has double traffic light control */
			boolean doubleIntersection = false;
			GeoTrafficLightMaster masterTrafficLightInitial = null;
			
			/* Find if another traffic light is very close */
			for (GeoTrafficLightMaster masterToCheck : trafficLightMasterList.values()) {
				double distance = Utils.distance(masterToCheck.getNode().lat, masterToCheck.getNode().lon, 
												currentNode.lat, currentNode.lon);
				if (distance < 20) {
					doubleIntersection = true;
					masterTrafficLightInitial = trafficLightMasterList.get(masterToCheck.getId());
					break;
				}
			}
			
			/* If current intersection has already a master traffic light associated */
			if (doubleIntersection && masterTrafficLightInitial != null) {
				
				/* Check if neighbor ways from current master traffic light have already
				 * a traffic light view associated */
				Vector<Long> currentWayNeighs = currentWay.neighs.get(currentNode.id);
				if (currentWayNeighs != null) {
					for (Long currentWayNeighId : currentWayNeighs) {
						
						if (!masterTrafficLightInitial.containsTrafficLightByWay(currentWayNeighId)) {
							
							/* Add a traffic light view for this neighbor way */
							wayNeigh =  MobilityEngine.getInstance().getWay(currentWayNeighId);
							nodeNeighIndex = wayNeigh.getNodeIndex(currentNode.id);
							nodeNeigh = wayNeigh.getNodeByIndex(nodeNeighIndex);
							
							/* Create a traffic light view for this neighbor way */
							masterTrafficLightInitial.addNode(currentNode);
							masterTrafficLightInitial.addNode(nodeNeigh);					
							createTrafficLightView(masterTrafficLightInitial, nodeNeigh, currentWayNeighId, trafficLightColor);
						}
					}
				}
				/* Don't add the current traffic light because another one already exists
				 * and it was updated with current neighbor ways
				 */
				return false;
			}
			
			/* ----------------------------------------------------------
			 * This intersection does't have a master traffic light yet */
			
			/* Add a traffic light view for each neighbor way that intersects the currentNode */
			Vector<Long> wayNeighs = currentWay.neighs.get(currentNode.id);
			if (wayNeighs == null || wayNeighs.size() <= 0) {
				return false;
			}
			
			/* set color for the new traffic light */
			trafficLightColor = getNextColor(trafficLightColor);
			currentTrafficLightMaster.addNode(currentNode);
			createTrafficLightView(currentTrafficLightMaster, currentNode, currentWay.id, trafficLightColor);
			
			/* Take every neighbor street and add a traffic light view for it*/
			for (Long wayIdNeigh : wayNeighs) {
				wayNeigh =  MobilityEngine.getInstance().getWay(wayIdNeigh);
				nodeNeighIndex = wayNeigh.getNodeIndex(currentTrafficLightMaster.getNode().id);
				nodeNeigh = wayNeigh.getNodeByIndex(nodeNeighIndex);
				
				/* set color for the new traffic light */
				trafficLightColor = getNextColor(trafficLightColor);
				currentTrafficLightMaster.addNode(nodeNeigh);
				createTrafficLightView(currentTrafficLightMaster, nodeNeigh, wayIdNeigh, trafficLightColor);
			}
			
			currentTrafficLightMaster.setIntersectionType(currentTrafficLightMaster.getTrafficLights().size());
			addTrafficLightMaster(currentTrafficLightMaster);
		}
		return true;
	}
	
	/***
	 * Adds the traffic light master to the viewer list
	 * @param trafficLightMaster
	 */
	public void addTrafficLightMaster(GeoTrafficLightMaster trafficLightMaster) {
		trafficLightMasterList.put(trafficLightMaster.getId(), trafficLightMaster);
	}
	
	/***
	 * Gets the next color
	 * Cycle: red -> green -> red
	 * @param trafficLightColor
	 * @return next color
	 */
	private Color getNextColor(Color trafficLightColor) {
		if (trafficLightColor == Color.red)
			return Color.green;
		
		return Color.red;		
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
				if (trafficLightMaster.needsColorsUpdate(simulationTime))
					trafficLightMaster.updateTrafficLightViews(simulationTime);
			}
			view.repaint();
		}
	}
}