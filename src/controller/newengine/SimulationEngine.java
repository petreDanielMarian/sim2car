package controller.newengine;

import gui.View;
import gui.Viewer;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

import application.ApplicationType;
import application.routing.RoutingApplicationCar;
import application.routing.RoutingApplicationParameters;
import model.Entity;
import model.GeoCar;
import model.GeoServer;
import model.mobility.MobilityEngine;
import model.parameters.Globals;
import model.parameters.MapConfig;
import model.parameters.MapConfiguration;
import model.threadpool.ThreadPool;
import model.threadpool.tasks.CarApplicationsRun;
import model.threadpool.tasks.CarMove;
import model.threadpool.tasks.CarPrepareMove;
import model.threadpool.tasks.ServerApplicationsRun;
import controller.engine.EngineInterface;
import controller.network.NetworkType;

/**
 * Class used to represent the brain of the simulator.
 * It reads the data for cars and servers applies the designated applications to be run on them.
 * Runs the simulation steps for each time frame (see the Runnable hidden object)
 * @author Alex
 *
 */
public class SimulationEngine implements EngineInterface {
	
	private final Logger logger = Logger.getLogger(SimulationEngine.class.getName());
	
	/** The thread that will keep the clock of the simulation */
	Runnable simulation;
	
	/** Map configuration read from a properties file */
	private MapConfig mapConfig;
	
	/** The Mobility engine that keeps track of traffic related problems */
	private MobilityEngine mobilityEngine;
	
	/** List of traffic entities */
	public TreeMap<Long,Entity> entities;
	
	/** Graphic interface visualizer */
	private Viewer viewer;
	
	/** Thread Pool reference */
	private ThreadPool threadPool;
	
	/** Control the simulation */
	boolean run = true;

	/** Simulation time */
	private long time = 0;

	private static final SimulationEngine _instance = new SimulationEngine();
	private static Object lock = null;

	/**
	 * Constructor is called just once, so all initializations are safe to be done here...
	 */
	private SimulationEngine() {
	}
	
	/**
	 * Loads the main parameters for the simulation
	 */
	private void load() {
		mapConfig = MapConfiguration.getInstance(Globals.propertiesFile);
		mobilityEngine = MobilityEngine.getInstance();
		viewer = new Viewer(mapConfig);
		entities = new TreeMap<Long,Entity>();
		threadPool = ThreadPool.getInstance();
		//EngineUtils.enhanceStreetGraph(mobilityEngine);
	}

	public static SimulationEngine getInstance() {
		synchronized (SimulationEngine.class) {
			if (lock == null) {
				lock = new Object();
				_instance.load();
			}
		}
		return _instance;
	}

	public void setUp() {
		
		entities.putAll(EngineUtils.getCars(getMapConfig().getTracesListFilename(), viewer, mobilityEngine) );
		entities.putAll(EngineUtils.getServers(getMapConfig().getAccessPointsFilename(), viewer, mobilityEngine) );
		
		for (Entity e : entities.values()) {
			if (e instanceof GeoServer) {
				EngineUtils.addApplicationToServer((GeoServer) e);
			}
		}
		
		simulation = new Runnable() {
			
			@Override
			public void run() {
				
				StringBuffer streetData = new StringBuffer();
				
				for (Entity e : entities.values()) {
					if (e instanceof GeoCar) {
						GeoCar car = (GeoCar) e;
						if (car.getActive() == 1) {
							car.start();
						}
					}
				}
				
				viewer.updateCarPositions();
				
				while (run) {
					time++;
					
					for (Entity e : entities.values()) {
						if (e instanceof GeoServer) {
							//streetData.append(((GeoCar) e).runApplications());
							threadPool.submit(new ServerApplicationsRun((GeoServer) e));
						}
						if (e instanceof GeoCar && ((GeoCar) e).getActive() == 1) {
							threadPool.submit(new CarApplicationsRun((GeoCar) e));
						}
						
					}

					threadPool.waitForThreadPoolProcessing();

					for (Entity e : entities.values()) {
						if (e instanceof GeoCar && ((GeoCar) e).getActive() == 1) {
//							((GeoCar) e).prepareMove();
							threadPool.submit(new CarPrepareMove((GeoCar) e));
						}
					}
					
					threadPool.waitForThreadPoolProcessing();
					
					int activeCars = 0;
					for (Entity e : entities.values()) {
						if (e instanceof GeoCar && ((GeoCar) e).getActive() == 1) {
//							((GeoCar) e).move();
							threadPool.submit(new CarMove((GeoCar) e));
							activeCars++;
						}
					}
					threadPool.waitForThreadPoolProcessing();

					viewer.updateCarPositions();
					viewer.setTime("" + time);
					
					if( (time + 2)% RoutingApplicationParameters.SamplingInterval == 0 )
					{
						for (Entity e : entities.values()) {
							if (e instanceof GeoCar && ((GeoCar) e).getActive() == 1) {
								((GeoCar) e).printRouteData(mapConfig.getCity() + "/" + time + "_routes_time_" + ((GeoCar) e).getId() + ".txt");
							}
						}

					}
					
					try {
						Thread.sleep(Globals.waitTime);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					if (activeCars == 0) {
						run = false;
					}
				}
				logger.info(""+time);
				System.out.println(""+time);
				PrintWriter writer = null;
				try {
					writer = new PrintWriter("road_data.txt", "UTF-8");
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
				if (writer != null) {
					writer.print(streetData.toString());
					writer.close();
				}
				logger.info("done!");
			}
		};
	}

	public void start() {
		viewer.showView();
		new Thread(simulation).start();
	}

	@Override
	public View getView() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<GeoCar> getPeers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<GeoServer> getServers() {
		ArrayList<GeoServer> servers = new ArrayList<GeoServer>();
		for (Entity e : entities.values()) {
			if (e instanceof GeoServer) {
				servers.add((GeoServer)e);
			}
		}
		return servers;
	}

	public MapConfig getMapConfig() {
		return mapConfig;
	}

	@Override
	public void stopSimulation() {
		this.run = false;
		stopActions();
	}

	public Entity getEntityById(long trafficEntityId) {
		return this.entities.get(trafficEntityId);
	}

	public void stopActions()
	{
		/* do the finalization actions for each entities*/
		for (Entity e : entities.values()) {
			if (e instanceof GeoCar && ((GeoCar) e).getActive() == 1) {
				((GeoCar) e).stopApplications();
				((GeoCar) e).stopNetwork();
			}
		}
		for( ApplicationType type : Globals.activeApplications )
		{
			stopGlobalApplicationActions(type);
		}
		for( NetworkType type : Globals.activeNetInterfaces )
		{
			stopGlobalNetworkActions(type);
		}

	}
	public void stopGlobalApplicationActions(ApplicationType type)
	{
		switch(type)
		{
			case TILES_APP:
				break;
			case ROUTING_APP:
				RoutingApplicationCar.stopGlobalApplicationActions();
				break;
			case STREET_VISITS_APP:
				break;
		}
	}
	public void stopGlobalNetworkActions( NetworkType type )
	{

	}
	public long getSimulationTime()
	{
		return time;
	}
}
