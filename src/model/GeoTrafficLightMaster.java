package model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import utils.Pair;
import application.Application;
import application.ApplicationType;
import application.multipleIntersections.SynchronizeIntersectionsData;
import application.trafficLight.ApplicationTrafficLightControlData;
import controller.network.NetworkInterface;
import controller.network.NetworkType;
import controller.network.NetworkWiFi;
import gui.TrafficLightView;
import model.OSMgraph.Node;
import model.OSMgraph.Way;
import model.mobility.MobilityEngine;
import model.network.Message;
import model.network.MessageType;
import model.parameters.Globals;

/***
 * The class represents the master traffic light of one intersection.
 * It's job is to receive message from cars that are waiting at one of it's traffic lights for green
 * color and to "put them" to the corresponding waiting queue. 
 * @author Andreea
 *
 */
public class GeoTrafficLightMaster extends Entity{
	
	/** Reference to mobility */
	private MobilityEngine mobility;

	/** If the traffic light is still active */
	private int active = 1;
	
	/** traffic light node center */
	private Node node;
	
	/** traffic light nodes for entities */
	private List<Node> nodes = new ArrayList<Node>();
	
	public List<Node> getNodes() {
		return nodes;
	}

	public void addNode(Node n) {
		this.nodes.add(n);
	}

	/** Number of cars waiting for green for each direction 
	 * key - (way id, direction)*/
	private TreeMap<Pair<Long, Integer>, Integer> noCarsWaiting;
	
	/** Maximum number of cars waiting for green */
	private int maxNoCarsWaiting = 0;

	/** traffic lights view for current intersection */
	private List<TrafficLightView> trafficLightViewList = new ArrayList<TrafficLightView>();
	
	private Integer updateLock = 1;
	
	private long simulationTimeLastChange = -1;
	
	private long timeCurrentPhase = Globals.trafficLightTime;

	/** intersection type */
	private int intersectionType = 3;

	public GeoTrafficLightMaster(long id, Node node, int intersectionType) {
		super(id);
		this.node = node;
		this.intersectionType = intersectionType;
		this.mobility = MobilityEngine.getInstance();
		this.noCarsWaiting = new TreeMap<Pair<Long, Integer>, Integer>();
	}
	
	/** Determine if the traffic lights need to change their color depending on the waiting
	 * queues. */
	public void changeColor() {
		Pair<Long, Integer> maxQueueKey = null;
		
		//System.out.println("change color");
		synchronized (updateLock) {
			/* find the traffic light with the maximum cars waiting */
			for (Pair<Long, Integer> key : noCarsWaiting.keySet()) {
				if (noCarsWaiting.get(key) > maxNoCarsWaiting) {
					maxNoCarsWaiting = noCarsWaiting.get(key);
					maxQueueKey = key;
				}
			}
			
			if (maxNoCarsWaiting > 0) {
				/* change color if this queue has red color*/
				if (getTrafficLightColor(maxQueueKey.getFirst(), maxQueueKey.getSecond())
						== Color.red) {
					
					//System.out.println("Change because is red " +maxNoCarsWaiting + " waiting");
					setTimeZero();
					/* set next phase time */
					timeCurrentPhase = Globals.trafficLightTime;
					/* restart phase */
					maxNoCarsWaiting = 0;
					maxQueueKey = null;
					noCarsWaiting.clear();
				}
				
				if (getTrafficLightColor(maxQueueKey.getFirst(), maxQueueKey.getSecond())
						== Color.green) {
					timeCurrentPhase = Globals.trafficLightTime;
				}
			}
		}
		
	}
	
	public void setTimeZero() {
		simulationTimeLastChange = -1;
	}
	
	public void updateTrafficLightViews(long simulationTime) {
		// time's up
		if (simulationTime - simulationTimeLastChange >= timeCurrentPhase || simulationTimeLastChange == -1) {
			for (TrafficLightView trafficLightView : trafficLightViewList) {
				trafficLightView.updateTrafficLightView();
			}
			simulationTimeLastChange = simulationTime;
		}
	}
	
	/***
	 * The master traffic light will put the car that has sent the message to the corresponding waiting queue.
	 * @param data
	 */
	public void addCarToQueue(ApplicationTrafficLightControlData data) {
		Pair<Long, Integer> key = new Pair<Long, Integer>(data.getWayId(), data.getDirection());
		int noCars = 1;
		
		synchronized (updateLock) {
			if (noCarsWaiting.containsKey(key)) {
				noCars = noCarsWaiting.get(key);
				noCarsWaiting.put(key, noCars + 1);
			}
			else {
				noCarsWaiting.put(key, 1);
			}
			
			sendDataToNeighbors(data.getMapPoint(), data.getWayId(), data.getDirection(), noCarsWaiting.get(key));
			
//			if (noCars > maxNoCarsWaiting) {
//				maxNoCarsWaiting = noCars;
//			}
//				/* change color of traffic light to green */
//				setTimeZero();
//				maxNoCarsWaiting = 0;
//			}
		}
		
	}
	

	/***
	 * Sends data to the closest 3 traffic light masters about the cars waiting
	 * @param mapPoint
	 * @param wayId
	 * @param direction
	 * @param queueSize
	 */
	public void sendDataToNeighbors(MapPoint mapPoint, Long wayId, int direction, int queueSize) {
		NetworkInterface net = this.getNetworkInterface(NetworkType.Net_WiFi);
		List<NetworkInterface> discoveredTrafficLightMasters = ((NetworkWiFi)net).discoverClosestsTrafficLightMasters();

		for (NetworkInterface discoveredTrafficLightMaster : discoveredTrafficLightMasters) {
			
			Message msg = new Message(this.getId(), discoveredTrafficLightMaster.getOwner().getId(), null, 
					MessageType.SYNCHRONIZE_TRAFFIC_LIGHTS, ApplicationType.SYNCHRONIZE_INTERSECTIONS_APP);
			
			SynchronizeIntersectionsData data = new SynchronizeIntersectionsData(
					mapPoint, queueSize, wayId, direction, this.getNode());
			
			msg.setPayload(data);
			net.putMessage(msg);
		}
	
	}
	
	public void synchronizeWithNeighbors(SynchronizeIntersectionsData data) {
		Pair<Long, Integer> key = new Pair<Long, Integer>(data.getWayId(), data.getDirection());
		
		Node fromMaster = data.getFromNode();
		
		long wayCar = data.getWayId();
		long wayFromMaster = data.getFromNode().wayId;
		long wayMaster = this.node.wayId;
		//Get all ways from this intersection (where this master traffic light is situated)
		List<Long> thisIntersection = mobility.streetsGraph.get(wayMaster).neighs.get(this.node.id);
		
		long link = 0;
		//Get all ways from the first intersection (traffic light)
		for (long wayFromFirstIntersection : mobility.streetsGraph.get(wayFromMaster).neighs.get(data.getFromNode().id)) {
			if (thisIntersection.contains(wayFromFirstIntersection)) {
				link = wayFromFirstIntersection;
				break;
			}
		}

		if (noCarsWaiting.containsKey(key)) {
//			int noCars = noCarsWaiting.get(key);
//			noCarsWaiting.put(key, noCars + data.getQueueSize());
//			System.out.println(this.getId() + " synchronize with neighbour no cars:" + noCars);
		}
		else {
//			System.out.println(mobility.streetsGraph.get(data.getWayId()).min_lat + " " +
//					mobility.streetsGraph.get(data.getWayId()).max_lat + " / " +
//					mobility.streetsGraph.get(data.getWayId()).min_long + " " +
//					mobility.streetsGraph.get(data.getWayId()).max_long);
//			
//			Way way = mobility.streetsGraph.get(data.getWayId());
//			Vector<Long> wayNeighs = way.neighs.get(this.getNode().id);
//			
//			for (Long w : wayNeighs) {
//				System.out.println("Neigh: " + mobility.streetsGraph.get(w).min_lat + " " +
//						mobility.streetsGraph.get(w).max_lat + " / " +
//						mobility.streetsGraph.get(w).min_long + " " +
//						mobility.streetsGraph.get(w).max_long);
//			}
			
			//System.out.println("Data nodes" + mobility.streetsGraph.get(data.getWayId()).nodes);
			for (TrafficLightView k : trafficLightViewList) {
	
//				System.out.println(mobility.streetsGraph.get(k.getWayId()).getAllNeighbors().contains(data.getWayId()));
//				//System.out.println(mobility.streetsGraph.get(k.getWayId()).nodes);
//				System.out.println(k.getWayId());
//				if (mobility.streetsGraph.get(k.getWayId()).getAllNeighbors().contains(data.getWayId()) == true) {
//				key = new Pair<Long, Integer>(k.getWayId(), data.getDirection());
//				//int noCars = noCarsWaiting.get(key);
//				//System.out.println("No cars " + noCars);
//				System.out.println("put " + k.getDirection() + " " + data.getDirection());
//				noCarsWaiting.put(key, data.getQueueSize());
//				}
				if (link == k.getWayId()) {
//					System.out.println("Found traffic light view: " + k.getWayId() + " " + k.getDirection());
					key = new Pair<Long, Integer>(k.getWayId(), k.getDirection());
					//System.out.println("Change because of neighbour");
					if (noCarsWaiting.containsKey(key)) {
						int noCars = noCarsWaiting.get(key);
						noCarsWaiting.put(key, noCars + 3);
					}
					else {
						noCarsWaiting.put(key, 2);
					}
				}
//				System.out.println(k.getWayId());
			}
//			System.out.println("not contain key");
		}
	}
	
	public void init() {
		mobility.addTrafficLight(this);
	}
	
	/**
	 * First initialization of the GeoTrafficLight Object
	 */
	public void start() {
		init();
	}	
	
	public int getActive() {
		return this.active;
	}

	
	public int getIntersectionType() {
		return intersectionType;
	}

	public void setIntersectionType(int intersectionType) {
		this.intersectionType = intersectionType;
	}
	
	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}
		
	public List<TrafficLightView> getTrafficLights() {
		return trafficLightViewList;
	}

	public void setTrafficLights(List<TrafficLightView> trafficLightViewList) {
		this.trafficLightViewList = trafficLightViewList;
	}
	
	public void addTrafficLightView(TrafficLightView trafficLightView) {
		this.trafficLightViewList.add(trafficLightView);
	}
	
	public boolean containsTrafficLightByWay(long wayId) {
		for (TrafficLightView trafficLightView : trafficLightViewList) {
			if (trafficLightView.getWayId() == wayId) 
				return true;
		}
		return false;
	}
	
	public Color getTrafficLightColor(long wayId, int direction) {
		for (TrafficLightView trafficLightView : trafficLightViewList) {
			if (trafficLightView.getWayId() == wayId && trafficLightView.getDirection() == direction) 
				return trafficLightView.getColor();
		}
		return Color.green;
	}
	
	public String runApplications() {
		String result = "";
		for (Application application : this.applications) {
			result += application.run();
		}
		return result;
	}

	public String stopApplications() {
		String result = "";
		for (Application application : this.applications) {
			result += application.stop();
		}
		return result;
	}

	
}
