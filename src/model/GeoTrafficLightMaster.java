package model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import utils.Pair;
import application.Application;
import application.ApplicationType;
import application.multipleIntersections.SynchronizeIntersectionsData;
import application.trafficLight.ApplicationTrafficLightControl;
import application.trafficLight.ApplicationTrafficLightControlData;
import controller.network.NetworkInterface;
import controller.network.NetworkType;
import controller.network.NetworkWiFi;
import controller.newengine.SimulationEngine;
import gui.TrafficLightView;
import model.OSMgraph.Node;
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
	
	private long sumWaitingTime = 0;
	private long sumQueueLenth = 0;
	private long noWaits = 0;
	
	public List<Node> getNodes() {
		return nodes;
	}

	public void addNode(Node n) {
		this.nodes.add(n);
	}

	/** Number of cars waiting for green for each direction 
	 *  Waiting time for the first car
	 * key - (way id, direction)  value - (firstCarWaitingTime, noCarsWaiting)*/
	private TreeMap<Pair<Long, Integer>, Pair<Long, Integer>> waitingQueue;
	
	/** Maximum number of cars waiting for green */
	private int maxNoCarsWaiting = 0;

	/** traffic lights view for current intersection */
	private List<TrafficLightView> trafficLightViewList = new ArrayList<TrafficLightView>();
	
	private Integer updateLock = 1;
	
	private long simulationTimeLastChange = 0;
	private boolean changeColor = false;
	
	private long timeCurrentPhase = Globals.normalTrafficLightTime;

	public long getTimeCurrentPhase() {
		return timeCurrentPhase;
	}

	public void setTimeCurrentPhase(long timeCurrentPhase) {
		this.timeCurrentPhase = timeCurrentPhase;
	}

	/** intersection type */
	private int intersectionType = 3;

	public GeoTrafficLightMaster(long id, Node node, int intersectionType) {
		super(id);
		this.node = node;
		this.intersectionType = intersectionType;
		this.mobility = MobilityEngine.getInstance();
		this.waitingQueue = new TreeMap<Pair<Long, Integer>, Pair<Long,Integer>>();
	}
	
	public void collectWaitingQueueStatistics(long stopTime, long noCarsWaiting) {
//		System.out.println("collecting data: " + stopTime + " " + noCarsWaiting);
		/* Waiting queue statistics */
		sumWaitingTime += (SimulationEngine.getInstance().getSimulationTime() - stopTime);
		sumQueueLenth += noCarsWaiting;
		noWaits++;
	}
	
	/** Determine if the traffic lights need to change their color depending on the waiting
	 * queues. */
	public void changeColor() {
		Pair<Long, Integer> maxQueueKey = null;	
	
		if (Globals.useTrafficLights && timeExpired()) {
			setChangeColor();
			return;
		}
		
		if (Globals.useDynamicTrafficLights) {
			if (waitingQueue.size() == 1) {
				/* Set green for this queue */
				/* set next phase time */
				maxNoCarsWaiting  = waitingQueue.get(waitingQueue.keySet().iterator().next()).getSecond();
				timeCurrentPhase = (maxNoCarsWaiting + 1) * Globals.passIntersectionTime > Globals.maxTrafficLightTime ?
						Globals.maxTrafficLightTime : maxNoCarsWaiting * Globals.passIntersectionTime;
				
				if (timeCurrentPhase < Globals.minTrafficLightTime)
					timeCurrentPhase = Globals.minTrafficLightTime;
				
				if (getTrafficLightColor(waitingQueue.keySet().iterator().next().getFirst(), waitingQueue.keySet().iterator().next().getSecond())
						== Color.red) {
					setChangeColor();
				}
				
				/* Waiting queue statistics */
				collectWaitingQueueStatistics(waitingQueue.get(waitingQueue.keySet().iterator().next()).getFirst(), 
										maxNoCarsWaiting);

				
				/* restart phase */
				maxNoCarsWaiting = 0;
				maxQueueKey = null;
				waitingQueue.clear();
				return;
			}
			
			if (timeExpired()) {
				synchronized (updateLock) {
					/* Find the traffic light with the maximum cars waiting */
					for (Pair<Long, Integer> key : waitingQueue.keySet()) {
						if (waitingQueue.get(key).getFirst() > Globals.maxWaitingTime) {
							/* There is a car that has been waiting for too long 
							 * (avoid infinite waiting) */
							maxNoCarsWaiting = waitingQueue.get(key).getSecond();
							maxQueueKey = key;
							break;
						}
						if (waitingQueue.get(key).getSecond() > maxNoCarsWaiting) {
							maxNoCarsWaiting = waitingQueue.get(key).getSecond();
							maxQueueKey = key;
						}
					}
					
					if (maxNoCarsWaiting > 0) {
							/* change color if this queue has red color*/
							
							/* set next phase time */
							timeCurrentPhase = (maxNoCarsWaiting + 1) * Globals.passIntersectionTime > Globals.maxTrafficLightTime ?
									Globals.maxTrafficLightTime : maxNoCarsWaiting * Globals.passIntersectionTime;
							
							if (timeCurrentPhase < Globals.minTrafficLightTime)
								timeCurrentPhase = Globals.minTrafficLightTime;
							
							if (getTrafficLightColor(maxQueueKey.getFirst(), maxQueueKey.getSecond())
									== Color.red) {
								setChangeColor();
							}
							
							/* Waiting queue statistics */
							collectWaitingQueueStatistics(waitingQueue.get(maxQueueKey).getFirst(), 
									waitingQueue.get(maxQueueKey).getSecond());
							
							/* restart phase */
							maxNoCarsWaiting = 0;
							maxQueueKey = null;
							waitingQueue.clear();
							return;	
					}
		//				
		//				if (getTrafficLightColor(maxQueueKey.getFirst(), maxQueueKey.getSecond())
		//						== Color.green) {
		//					timeCurrentPhase = Globals.maxTrafficLightTime;
		//				}
	
						timeCurrentPhase = Globals.normalTrafficLightTime;
						setChangeColor();
						/* restart phase */
						maxNoCarsWaiting = 0;
						maxQueueKey = null;
						waitingQueue.clear();
				}
			}
		}
		
	}
	
	public void setChangeColor() {
		simulationTimeLastChange = SimulationEngine.getInstance().getSimulationTime();
		changeColor = true;
	}
	
	public void updateTrafficLightViews(long simulationTime) {
			for (TrafficLightView trafficLightView : trafficLightViewList) {
				trafficLightView.updateTrafficLightView();
			}
			changeColor = false;
	}
	
	public boolean timeExpired() {
		if (SimulationEngine.getInstance().getSimulationTime() - simulationTimeLastChange >= timeCurrentPhase) {
			//System.out.println("time expired " + SimulationEngine.getInstance().getSimulationTime());
			return true;
		}
		return false;
	}
	
	public boolean needsColorsUpdate(long simulationTime) {
		return changeColor;
	}
	/***
	 * The master traffic light will put the car that has sent the message to the corresponding waiting queue.
	 * @param data
	 */
	public void addCarToQueue(ApplicationTrafficLightControlData data) {
		Pair<Long, Integer> key = new Pair<Long, Integer>(data.getWayId(), data.getDirection());
		
		synchronized (updateLock) {
			if (waitingQueue.containsKey(key)) {
				Pair<Long, Integer> value = new Pair<Long, Integer>
					(waitingQueue.get(key).getFirst(), waitingQueue.get(key).getSecond() + 1);

				waitingQueue.put(key, value);
			}
			else {
				Pair<Long, Integer> value = new Pair<Long, Integer>
					(data.getTimeStop(), 1);
				
				waitingQueue.put(key, value);
			}
			
			sendDataToNeighbors(data.getMapPoint(), 
					data.getWayId(), data.getDirection(), 
					waitingQueue.get(key).getSecond());
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

		if (waitingQueue.containsKey(key)) {
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
					if (getId() == 706 || getId() == 705)
						System.out.println("Change because of neighbour");
					if (waitingQueue.containsKey(key)) {
						Pair<Long, Integer> value = new Pair<Long, Integer>
							(waitingQueue.get(key).getFirst(), waitingQueue.get(key).getSecond() + 3);
						waitingQueue.put(key, value);
					}
					else {
						Pair<Long, Integer> value = new Pair<Long, Integer>
							(SimulationEngine.getInstance().getSimulationTime(), 3);
						waitingQueue.put(key, value);
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
	
	public void sendStatistics() {
		if (noWaits == 0)
			return;
		
		double avg_waitingTime = sumWaitingTime / noWaits;
		double avg_queueLength = sumQueueLenth / noWaits;
		ApplicationTrafficLightControl.saveData(this.getId(), avg_waitingTime, avg_queueLength);
	}

	public String stopApplications() {
		/* Send statistics */
		sendStatistics();
		
		String result = "";
		for (Application application : this.applications) {
			result += application.stop();
		}
		return result;
	}
	
	public String stopNetwork() {
		String result = "";
		for (NetworkInterface net : this.netInterfaces) {
			result += net.stop();
		}
		return result;
	}

	
}
