package model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import utils.Pair;
import application.Application;
import application.trafficLight.ApplicationTrafficLightControlData;
import gui.TrafficLightView;
import model.OSMgraph.Node;
import model.mobility.MobilityEngine;
import model.parameters.Globals;

public class GeoTrafficLightMaster extends Entity{
	
	/** Reference to mobility */
	private MobilityEngine mobility;

	/** If the traffic light is still active */
	private int active = 1;
	
	/** traffic light node */
	private Node node;
	
	/** Number of cars waiting for green for each direction */
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
	
	/** Change the color of this traffic light if the time is 0 */
	public void changeColor() {	
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
			
			if (noCars > maxNoCarsWaiting) {
				maxNoCarsWaiting = noCars;
				/* change color of traffic light to green */
				setTimeZero();
				maxNoCarsWaiting = 0;
			}
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
