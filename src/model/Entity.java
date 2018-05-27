package model;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import model.OSMgraph.Way;
import model.network.Message;
import application.Application;
import application.ApplicationType;
import controller.engine.EngineInterface;
import controller.engine.EngineSimulation;
import controller.network.NetworkInterface;
import controller.network.NetworkType;
import controller.newengine.SimulationEngine;

/**
 * This class represents the main structure of all the simulator components such as
 * cars and servers.
 * @author Alex
 *
 */
public class Entity {

	/** The ID of this entity. */
	private long id;

	/** The current position on the map. */
	private MapPoint currentPos = null;

	/** List of applications running on top of this entity. */
	protected List<Application> applications;

	/** List of network interfaces available for this entity. */
	protected List<NetworkInterface> netInterfaces;
	
	
	private final Logger logger;

	public Entity(long id) {
		this.id = id;
		this.applications = new LinkedList<Application>();
		this.netInterfaces = new LinkedList<NetworkInterface>();
		this.logger = Logger.getLogger(Entity.class.getName());
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public void setCurrentPos(MapPoint currentPos) {
		this.currentPos = currentPos;
	}

	public MapPoint getCurrentPos() {
		return currentPos;
	}

	public List<GeoCar> getPeers() {
		// TODO EngineSimulation
		return null;
	}
	
	/* Returns all the servers in the simulator */ 
	public List<GeoServer> getServers() {
		EngineInterface engine = SimulationEngine.getInstance();
		if( engine == null )
		{	
			/* Use the old Engine of Simulator */
			return EngineSimulation.getInstance().getServers();
		}
		else
		{
			return engine.getServers();
		}
	}

	public GeoCar getPeer(long id) {
		// TODO EngineSimulation
		return null;
	}

	public GeoServer getServer(long id) {
		// TODO EngineSimulation
		return null;
	}

	/**
	 * Process a message.
	 * Send that message for processing to all applications running.
	 */
	public void process(Message m) {
		
		if (m == null) {
			logger.log(Level.INFO, "Entity id: " + id + " finished all input messages for the tick");
			return;
		}
		
		Application app = this.getApplication(m.getAppType());
		/* if this entity can process message m internally */
		if (app != null) {
			app.process(m);
		}
	}
	
	/**
	 * Adds the given network interface to the list of available interfaces on
	 * this vehicle. It ensures that there is only one network interface of the
	 * new type. If there is already an interface of the same type, that
	 * interface is replaced by the given network interface.
	 *   
	 * @param networkInterface The network interface to be added
	 */
	public void addNetworkInterface(NetworkInterface networkInterface) {
		Iterator<NetworkInterface> it = this.netInterfaces.iterator();
		while(it.hasNext()) {
			NetworkInterface netInterface = it.next();
			if (netInterface.getType() == networkInterface.getType())
				it.remove();
		}
		this.netInterfaces.add(networkInterface);
	}
	
	/**
	 * Remove the network interface of the given type from the available
	 * interfaces. There should be just one interface per type. If there are
	 * more than one with the same type, both will be removed.
	 * 
	 * @param type The type of the interface to be removed
	 */
	public void removeNetworkInterface(NetworkType type) {
		Iterator<NetworkInterface> it = this.netInterfaces.iterator();
		while(it.hasNext()) {
			NetworkInterface netInterface = it.next();
			if (netInterface.getType() == type)
				it.remove();
		}
	}

	/**
	 * Return the network interface with the specified type.
	 * 
	 * @param type The type of the network interface requested.
	 * @return     The network interface or null if this Entity doesn't have it. 
	 */
	public NetworkInterface getNetworkInterface(NetworkType type) {
		for (NetworkInterface net : this.netInterfaces) {
			if (net.getType() == type)
				return net;
		}
		return null;
	}

	/**
	 * Adds the given application to the list of running apps on this vehicle.
	 * It ensures that there is only one application of the new type. If there
	 * is already an application of the same type, that application is
	 * replaced by the given application.
	 * 
	 * @param application  The application to be added to the internal list
	 */
	public void addApplication(Application application) {
		Iterator<Application> it = this.applications.iterator();
		while(it.hasNext()) {
			Application app = it.next();
			if (app.getType() == application.getType())
				it.remove();
		}
		this.applications.add(application);
	}
	
	/**
	 * Remove the application of the given type from the running applications.
	 * If there are more than one with the same type, both will be removed.
	 * 
	 * @param type  The type of the application that will be removed
	 */
	public void removeApplication(ApplicationType type) {
		Iterator<Application> it = this.applications.iterator();
		while(it.hasNext()) {
			Application app = it.next();
			if (app.getType() == type)
				it.remove();
		}
	}

	/**
	 * Return the application with the specified type.
	 * 
	 * @param type The type of the network interface requested.
	 * @return     The network interface or null if this Entity doesn't have it. 
	 */
	public Application getApplication(ApplicationType type) {
		for (Application app : this.applications) {
			if (app.getType() == type)
				return app;
		}
		return null;
	}

	public TreeMap<Long, Way> getStreetGraph() {
		// TODO EngineSimulation
		return null;
	}

	public Vector<Vector<Integer>> getStreetAreas() {
		// TODO EngineSimulation
		return null;
	}


	/**
	 * 
	 * @return - list of this entity network interfaces
	 */
	public List<NetworkInterface> getNetworkInterfaces() {
		// TODO Auto-generated method stub
		return this.netInterfaces;
	}
}