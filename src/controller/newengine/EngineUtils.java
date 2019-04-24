package controller.newengine;

import gui.TrafficLightView;
import gui.Viewer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import application.Application;
import application.ApplicationType;
import application.ApplicationUtils;
import application.routing.RoutingApplicationParameters;
import application.routing.RoutingApplicationServer;
import model.GeoCar;
import model.GeoCarRoute;
import model.GeoServer;
import model.GeoTrafficLightMaster;
import model.MapPoint;
import model.PeanoKey;
import model.PixelLocation;
import model.OSMgraph.Node;
import model.OSMgraph.Way;
import model.mobility.MobilityEngine;
import model.parameters.Globals;
import model.parameters.MapConfig;
import utils.ComputeAverageSpeeds;
import utils.ComputeStreetVisits;
import utils.SphericalMercator;
import utils.tracestool.Utils;
import utils.tracestool.parameters.GenericParams;
import controller.network.NetworkInterface;
import controller.network.NetworkType;
import controller.network.NetworkUtils;
import controller.network.NetworkWiFi;

/**
 * Class used to read the servers and the cars from files
 *
 */
public final class EngineUtils {
	
    /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(EngineUtils.class.getName());
    
	/* for traffic entities id */
	static int count = 0;
		
	/* Server info */
	static double latEdge = 0d, lonEdge = 0d;
	static long rows = 0, cols = 0;
	
	private EngineUtils() {}

	/**
	 * Return a list of cars from car traces.
	 * 
	 * @param carListFilename	the file with the list of car IDs
	 * @param viewer			the GUI viewer
	 * @param mobilityEngine	the traffic engine to which we must add
	 * 								the cars we read for this simulation
	 */
	static TreeMap<Long,GeoCar> getCars(String carListFilename, Viewer viewer, MobilityEngine mobilityEngine) {
		FileInputStream fstream = null;
		TreeMap<Long,GeoCar> cars = new TreeMap<Long,GeoCar>();

		try {
			fstream = new FileInputStream(carListFilename);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String line;

			/* Read data about traces */
			while ((line = br.readLine()) != null) {
				/* If the simulation requires just a fraction of the cars
				 * to be simulated, choose them randomly */
				if (Math.random() > Globals.randomCarsSelect)
					continue;
				/* The number of cars in the simulation can be limited */
				if (count == Globals.carsCount)
					break;

				if (count == 183)
					System.out.println("read ");
				logger.info(" We opened " + count + ". " + line);
				StringTokenizer st = new StringTokenizer(line, " ", false);
				st.nextToken(); /* <cab */
				String srcId = st.nextToken(); /* id="XXXXX" */
				srcId = srcId.substring(4, srcId.length() - 1); /* extract just the number */
				String path = SimulationEngine.getInstance().getMapConfig().getTracesPath() + "joints_" + srcId + ".txt";
				List<GeoCarRoute> routes = Utils.readCarTraces(path);
				
				GeoCar car = new GeoCar(count);
				car.setRoutes(routes);
				if (count == 183)
					System.out.println("read +");

				/* Create each network interface which is defined */
				for( NetworkType type : Globals.activeNetInterfaces )
				{
					NetworkInterface netInterface = NetworkUtils.activateNetworkInterface(type, car);
					car.addNetworkInterface(netInterface);
				}
				
				/* Create each application which is defined */
				for( ApplicationType type : Globals.activeApplications )
				{
					Application app = ApplicationUtils.activateApplicationCar(type, car);
					if( app == null )
					{
						logger.info(" Failed to create application with type " + type);
						continue;
					}
					car.addApplication( app );
				}

				viewer.addCar(car);
				cars.put(car.getId(), car);
				count++;
			}
			br.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				fstream.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("cars" + cars.size());
		return cars;
	}
	
	/**
	 * Return a list of servers from car traces.
	 * 
	 * @param serverListFilename	the file with the list of car IDs
	 * @param viewer				the GUI viewer
	 * @param mobilityEngine		the traffic engine to which we must add
	 * 									the servers we read for this simulation
	 * 
	 */
	static TreeMap<Long,GeoServer> getServers(String serverListFilename, Viewer viewer, MobilityEngine mobilityEngine) {
		MapConfig mapConfig = SimulationEngine.getInstance().getMapConfig();
		FileInputStream fstream = null;
		SphericalMercator mercator = new SphericalMercator();
		TreeMap<Long,GeoServer> servers = new TreeMap<Long,GeoServer>();
		ArrayList<GeoServer> serversList = new ArrayList<>();
		
		try {
			logger.info("Path received " + serverListFilename);
			fstream = new FileInputStream(serverListFilename);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String line;


			while ((line = br.readLine()) != null) {
				if (line.startsWith("#"))
					continue;
				StringTokenizer st = new StringTokenizer(line, " ", false);
				if (line.startsWith("size"))
				{
					/* ignore size word */
					st.nextToken();
					rows = Long.parseLong(st.nextToken());
					cols = Long.parseLong(st.nextToken());
					continue;
				}
				

				double lat = Double.parseDouble(st.nextToken());
				double lon = Double.parseDouble(st.nextToken());

				if( st.hasMoreTokens() )
				{
					/* 
					 * The server is enclosed by a rectangle, so should be detected
					 * it's edges.
					 */
					latEdge = Double.parseDouble(st.nextToken());
					lonEdge = Double.parseDouble(st.nextToken());
				}

				PixelLocation pix = mercator.LatLonToPixelLoc(lat, lon, Globals.zoomLevel);

				pix.tile.y -= mapConfig.getBaseRow();
				pix.tile.x -= mapConfig.getBaseColumn();

				count++;
				MapPoint mp = new MapPoint(pix, lat, lon, false, 0);

				GeoServer s = new GeoServer(mapConfig.getN(), mapConfig.getM(), count, mp);
				//System.out.println(s.getId());
				
				addApplicationToServer(s);

				servers.put(s.getId(), s);
				serversList.add(s);
			}
			
			br.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				fstream.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		/* Add other servers to the map */
		computeServersPositions(serversList, servers);
		viewer.addServers(serversList);
		System.out.println(servers.size() + " " + count);
		/* Compute the neighbor servers of each server */
		computeServerNeighbors(servers);

		return servers;
	}
	
	
	/* Finds the neighbors of all servers */
	public static void computeServerNeighbors(TreeMap<Long, GeoServer> serversMap) {
		for (Map.Entry<Long, GeoServer> entry1 : serversMap.entrySet()) {
			for (Map.Entry<Long, GeoServer> entry2 : serversMap.entrySet()) {
				GeoServer s1 = entry1.getValue();
				GeoServer s2 = entry2.getValue();
				
				if (Utils.distance(s1.getCurrentPos().lat, s1.getCurrentPos().lon,
						s2.getCurrentPos().lat, s2.getCurrentPos().lon) < RoutingApplicationParameters.regionDistance +
						RoutingApplicationParameters.distMax && s1.getId() != s2.getId()) {
					s1.neighServers.add(s2.getId());
				}
			}
		}
	}
	
	public static void addApplicationToServer(GeoServer s) {
		s.addNetworkInterface(new NetworkWiFi(s));
		
		/* Create each application which is defined */
		for( ApplicationType type : Globals.activeApplications )
		{
			Application app = ApplicationUtils.activateApplicationServer(type, s);
			if( app == null )
			{
				logger.info(" Failed to create application with type " + type);
				continue;
			}
			if( app.getType() == ApplicationType.ROUTING_APP )
			{
				((RoutingApplicationServer)app).setGridSize(rows, cols);
				((RoutingApplicationServer)app).setServerAreaSizes(latEdge, lonEdge);
			}
			s.addApplication(app);
		}
	}
	
	/* Computes the positions of the servers on the map */
	private static void computeServersPositions(ArrayList<GeoServer> servers, TreeMap<Long, GeoServer> serversMap) {
		Set<Entry<Long,Way>> ways = MobilityEngine.getInstance().streetsGraph.entrySet();
		
		for (Entry<Long,Way> entry : ways) {
			Way auxWay = entry.getValue();
			
			// Check if the ways has any nodes in it
			if (auxWay.nodes != null && auxWay.nodes.size() > 0) {
				// We place our server on the first node of the way
				Node auxNode = auxWay.nodes.get(0);
				SphericalMercator mercator = new SphericalMercator();
				MapConfig mapConfig = SimulationEngine.getInstance().getMapConfig();
				
				PixelLocation pix = mercator.LatLonToPixelLoc(auxNode.lat, auxNode.lon, Globals.zoomLevel);

				pix.tile.y -= mapConfig.getBaseRow();
				pix.tile.x -= mapConfig.getBaseColumn();
				
				MapPoint mp = new MapPoint(pix, auxNode.lat, auxNode.lon, false, 0);

				GeoServer s = new GeoServer(mapConfig.getN(), mapConfig.getM(), count, mp);
				boolean control = true;
				
				for (GeoServer s1 : servers) {
					if (Utils.distance(s.getCurrentPos().lat, s.getCurrentPos().lon,
							s1.getCurrentPos().lat, s1.getCurrentPos().lon) < RoutingApplicationParameters.regionDistance) {
						control = false;
						break;
					}
				}
				
				if (control) {
					count++;
					s.setId(count);
					servers.add(s);
					serversMap.put(s.getId(), s);
				}
				
			}
		}
		
		System.out.println(servers.size());
	}
	
	private static void addTrafficLightApps(GeoTrafficLightMaster master) {
		/* Create each network interface which is defined */
		for( NetworkType type : Globals.activeNetInterfaces )
		{
			NetworkInterface netInterface = NetworkUtils.activateNetworkInterface(type, master);
			master.addNetworkInterface(netInterface);
		}
		
		/* Create each application which is defined for traffic light*/
		ApplicationType type = ApplicationType.TRAFFIC_LIGHT_CONTROL_APP;
		Application app = ApplicationUtils.activateApplicationTrafficLight(type, master);
		if( app == null )
		{
			logger.info(" Failed to create application with type " + type);
		}
		else
			master.addApplication( app );
		
		if (Globals.useDynamicTrafficLights) {
			/* Create each application which is defined for traffic light*/
			type = ApplicationType.SYNCHRONIZE_INTERSECTIONS_APP;
			app = ApplicationUtils.activateApplicationSynchronizeTrafficLight(type, master);
			if( app == null )
			{
				logger.info(" Failed to create application with type " + type);
			}
			else
				master.addApplication( app );
		}
	}
	
	/***
	 * Read existing traffic lights
	 * @param file
	 * @return
	 */
	private static TreeMap<Long,GeoTrafficLightMaster> readExistingTrafficLights(
			String file, Viewer viewer, MobilityEngine mobilityEngine) {
		
		FileInputStream fstream = null;
		TreeMap<Long,GeoTrafficLightMaster> trafficLights = new TreeMap<Long,GeoTrafficLightMaster>();
		
		try {
			fstream = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String line;
			
			//Format:
			//		  master id idNode wayidNode latNote lonNode
			// 		  wayId direction lat lon segmentIndex cellIndex color
			
			/* Read data about traffic lights */
			GeoTrafficLightMaster master = null;
			while ((line = br.readLine()) != null) {
				
				StringTokenizer st = new StringTokenizer(line, " ", false);
				String keyword = st.nextToken(); 					/* master keyword */
				if (keyword.equals("master")) {
					
					if (master != null) {
						trafficLights.put(master.getId(), master);
						viewer.addTrafficLightMaster(master);
					}
					
					// traffic light master line
					count++;
					Long masterId = (long) count;
					st.nextToken(); 	/* traffic light master id */
					Long nodeId = Long.parseLong(st.nextToken()); 		/* node id */
					Long wayId = Long.parseLong(st.nextToken()); 		/* way id" */
					
					Node node = mobilityEngine.streetsGraph.get(wayId).getNode(nodeId);
					node.setWayId(wayId);
					master = new GeoTrafficLightMaster(masterId, node, 3);
					MapPoint mapPoint = MapPoint.getMapPoint(node);
					master.setCurrentPos(mapPoint);
					
					if (Globals.useTrafficLights || Globals.useDynamicTrafficLights)
						addTrafficLightApps(master);
					continue;
				}
				if (keyword.equals("nodes")) {
					while(st.hasMoreTokens()) {
						Long nodeId = Long.parseLong(st.nextToken());
						Long wayId = Long.parseLong(st.nextToken());
						Node node = mobilityEngine.streetsGraph.get(wayId).getNode(nodeId);
						master.addNode(node);
					}
				}
				else {
					if (st.countTokens() != 6)
						continue;
					// traffic light view line
					Long wayId = Long.parseLong(keyword); 					/* way id */
					Integer direction = Integer.parseInt(st.nextToken()); 	/* direction */
					Double lat = Double.parseDouble(st.nextToken()); 		/* lat */
					Double lon = Double.parseDouble(st.nextToken()); 		/* lon */
					Integer seg = Integer.parseInt(st.nextToken()); 		/* segmentIndex */
					Long cell = Long.parseLong(st.nextToken()); 			/* cellIndex */
					String color = st.nextToken(); 							/* color */
					MapPoint currentPoint = MapPoint.getMapPoint(lat, lon, 1, wayId);
					currentPoint.segmentIndex = seg;
					currentPoint.cellIndex = cell;
					TrafficLightView view = new TrafficLightView(viewer.getMapJ(), currentPoint, wayId, direction);
					view.setColor(color);
					viewer.addMapMarker(currentPoint.lat, currentPoint.lon, view.getColor());
					master.addTrafficLightView(view);
				}
			}
			br.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				fstream.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		System.out.println(trafficLights.size());
		return trafficLights;
	}
	
	/***
	 * 
	 * @param inFile
	 * @param outFile
	 * @param viewer
	 * @param mobilityEngine
	 * @return
	 */
	private static TreeMap<Long,GeoTrafficLightMaster> readTrafficLights(
			String inFile, String outFile, Viewer viewer, MobilityEngine mobilityEngine) {
		
		FileInputStream fstream = null;
		TreeMap<Long,GeoTrafficLightMaster> trafficLights = new TreeMap<Long,GeoTrafficLightMaster>();
		//if traffic lights were not loaded before, read them from the config
		try {
			fstream = new FileInputStream(inFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String line;

			/* read info data */
			br.readLine();
			/* Read data about traffic lights */
			while ((line = br.readLine()) != null) {
				logger.info(" We opened " + count + ". " + line);
				
				StringTokenizer st = new StringTokenizer(line, " ", false);
				Long nodeId = Long.parseLong(st.nextToken()); /* node id */
				Long wayId = Long.parseLong(st.nextToken()); /* way id" */
				
				Node node = mobilityEngine.streetsGraph.get(wayId).getNode(nodeId);
				node.setWayId(wayId);
				
				Way way = mobilityEngine.getWay(wayId);
				int noWaysNeigh = 0;
				if (way.neighs.containsKey(node.id)) {
					noWaysNeigh = way.neighs.get(node.id).size();
				}
				count++;
				GeoTrafficLightMaster trafficLight = new GeoTrafficLightMaster(count, node, noWaysNeigh);
				
				MapPoint mapPoint = MapPoint.getMapPoint(node);
				mapPoint.segmentIndex = mobilityEngine.getSegmentNumber(node);
				
				if (mapPoint.segmentIndex < mobilityEngine.getWay(wayId).nodes.size() - 1)
					mapPoint.cellIndex = mobilityEngine.getCellIndex(node, mobilityEngine.streetsGraph.get(wayId).getNodeByIndex(mapPoint.segmentIndex + 1), mapPoint);
				else {
					if (mapPoint.segmentIndex <= 0)
						mapPoint.segmentIndex = 1;
					mapPoint.cellIndex = mobilityEngine.getCellIndex(mobilityEngine.streetsGraph.get(wayId).getNodeByIndex(mapPoint.segmentIndex - 1), node, mapPoint);
				}
				trafficLight.setCurrentPos(mapPoint);
				trafficLights.put(trafficLight.getId(), trafficLight);
				
				
				/* Create each network interface which is defined */
				for( NetworkType type : Globals.activeNetInterfaces )
				{
					NetworkInterface netInterface = NetworkUtils.activateNetworkInterface(type, trafficLight);
					trafficLight.addNetworkInterface(netInterface);
				}
				
				/* Create each application which is defined for traffic light*/
				if (Globals.useTrafficLights || Globals.useDynamicTrafficLights)
					addTrafficLightApps(trafficLight);

				//if another master traffic light exists
				if (!viewer.addTrafficLightViews(trafficLight)) {
					trafficLights.remove(trafficLight.getId());
				}
			}
			br.close();
			
			//write the traffic lights to file for further runs
			writeToFileTrafficLights(outFile, trafficLights);
			
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				fstream.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return trafficLights;
	}
	
	/***
	 * Return a list of master traffic lights from traces file or from previous runs.
	 * @param trafficLightListFilename - traffic lights from traces and osm map
	 * @param trafficLightsLoaded - traffic lights from previous runs
	 * @param viewer
	 * @param mobilityEngine
	 * @return
	 */
	static TreeMap<Long,GeoTrafficLightMaster> getTrafficLights(String trafficLightListFilename,
			String trafficLightsLoaded, Viewer viewer, MobilityEngine mobilityEngine) {

		/* check if file with traffic lights exists */
		File file = new File(trafficLightsLoaded);
		if (file.exists() && !file.isDirectory()) {
			/* read the traffic lights configuration from here */
			return readExistingTrafficLights(trafficLightsLoaded, viewer, mobilityEngine);
		}
		return readTrafficLights(trafficLightListFilename, trafficLightsLoaded, viewer, mobilityEngine);
	}
	
	
	/***
	 * Write traffic lights configuration to output file for further runs
	 * @param trafficLightListFilename
	 * @param trafficLights
	 */
	private static void writeToFileTrafficLights(
			String trafficLightListFilename, TreeMap<Long,GeoTrafficLightMaster> trafficLights) {
		
		FileOutputStream fstream = null;
		try {
			fstream = new FileOutputStream(trafficLightListFilename);
			Writer br = new BufferedWriter(new OutputStreamWriter(fstream));
			
			//Format: master id idNode wayidNode latNote lonNode \n";
			// 		  wayId direction lat lon segmentIndex cellIndex color
			
			String line = "";
			for (GeoTrafficLightMaster tlm : trafficLights.values()) {
				line += "master " + tlm.getId() + " " + tlm.getNode().id + " " + tlm.getNode().wayId + " " 
							+ tlm.getNode().lat + " " + tlm.getNode().lon;
				br.write(line);
				br.write(System.lineSeparator());
				line = "nodes";
				for (Node node : tlm.getNodes()) {
					line += " " + node.id + " " + node.wayId;
				}
				br.write(line);
				br.write(System.lineSeparator());
				line = "";
				for (TrafficLightView tlv : tlm.getTrafficLights()) {
					line += tlv.getWayId() + " " + tlv.getDirection() + " "
							+ tlv.getCurrentPoint().lat + " " + tlv.getCurrentPoint().lon + " " 
							+ tlv.getCurrentPoint().segmentIndex + " " + tlv.getCurrentPoint().cellIndex + " "
							+ tlv.getColorString();
					br.write(line);
					br.write(System.lineSeparator());
					line = "";
				}
			}
			br.close();	
		}
		catch (IOException ex){}
		finally {
				try {
					fstream.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
	}
	
	
	/**
	 * Load the street graph in memory.
	 * 
	 * @param streetsFilename		This file stores data about each street:
	 * 									* way_id, node_ids, oneway-ness
	 * @param partialGraphFilename	This file stores data about the road graph:
	 * 									* way_id, box coordinates and join_ids
	 * @return  A TreeMap encoding the street graph, where the way_id is key,
	 * 				and the Way is the value
	 */
	public static TreeMap<Long, Way> loadGraph(String streetsFilename,
										String partialGraphFilename) {
		FileInputStream streetsFIS = null, graphFIS = null;
		BufferedReader streetsBR = null, graphBR = null;
		TreeMap<Long, Way> graph = new TreeMap<Long, Way>();
		
		try {
			String line;
			streetsFIS = new FileInputStream(streetsFilename);
			graphFIS = new FileInputStream(partialGraphFilename);
			streetsBR = new BufferedReader(new InputStreamReader(streetsFIS));
			graphBR = new BufferedReader(new InputStreamReader(graphFIS));

			/* Read all streets file before reading the graph file, as there are
			 * streets that don't have any intersections in the latter, hence no
			 * entry, and we don't want to ignore those streets. */
			Way way = null;
			line = streetsBR.readLine();
			if (line == null)
				return null;
			if (line.contains("map_lat_min")) {}  // ignore first line

			while ((line = streetsBR.readLine()) != null) {
				String[] ws = line.split(" ");
				/* way_id 4035215 */
				if (ws[0].equals("way_id")) {
					way = new Way(Long.parseLong(ws[1]));
					if (way != null) graph.put(way.id, way);
				}
				/* oneway true */
				if (ws[0].equals("oneway")) {
					way.setDirection(Boolean.parseBoolean(ws[1]));
				}
				/* nod_id 1636835245 lat 37.8111372 long -122.3633334 */
				if (ws[0].equals("node_id")) {
					double lat = Double.parseDouble(ws[3]);
					double lon = Double.parseDouble(ws[5]);
					Node n = new Node(Long.parseLong(ws[1]), lat, lon);
					n.setWayId(way.id);
					if (way.id == 0) {
						System.err.println("node id: " + n.id);
					}
					way.addNode(n);
				}
			}

			/* Read now the information in the graph file and update the ways
			 * in the graph with their intersections. */
			line = graphBR.readLine();
			if (line == null) {
				return null;
			}

			do {
				String [] ws = line.split(" ");
				if (ws[0].compareTo("way_id") == 0) {
					way = graph.get(Long.parseLong(ws[1]));
					if (way == null) {
						logger.warning( "[loadGraph] Way not found: " + ws[1]);
					}
					while ((line = graphBR.readLine()) != null) {
						if (line.contains("join_id") == false)
							break;
						ws = line.split(" ");

						Vector<Long> av = way.neighs.get(Long.parseLong(ws[1]));
						if (av == null)
							av = new Vector<Long>();
						for (int h = 2; h < ws.length; h++) {
							av.add(Long.parseLong(ws[h]));
						}
						way.neighs.put(Long.parseLong(ws[1]), av);
					}
				}
			} while (line != null);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (streetsBR != null)
					streetsBR.close();
				if (streetsFIS != null)
					streetsFIS.close();
				if (graphBR != null)
					graphBR.close();
				if (graphFIS != null)
					graphFIS.close();
			} catch (IOException e) {}
		}
		return graph;
	}
	
	public static void enhanceStreetGraph(MobilityEngine mobility) {
		try {
			TreeMap<Long, Way> graph = mobility.streetsGraph;
//			HashMap<Long, Integer> streetVisits = ComputeStreetVisits.getStreetVisits("trunk\\traces\\BeijingRouteTimeData\\road_data_sorted.txt");
//			HashMap<Long, Double> streetSpeeds = ComputeAverageSpeeds.getStreetSpeeds("trunk\\traces\\BeijingRouteTimeData\\road_speeds_sorted.txt");

			HashMap<Long, Integer> streetVisits = ComputeStreetVisits.getStreetVisits("trunk\\traces\\SanFranciscoRouteTimeData\\road_data_sorted.txt");
			HashMap<Long, Double> streetSpeeds = ComputeAverageSpeeds.getStreetSpeeds("trunk\\traces\\SanFranciscoTimeData\\road_speeds_sorted.txt");
			
			for (Long way : streetVisits.keySet())
				graph.get(way).setVisits(streetVisits.get(way));
			for (Long way : streetSpeeds.keySet())
				graph.get(way).setMaximumSpeed(streetSpeeds.get(way));
		} catch (IOException e) {
			e.printStackTrace();
			logger.severe("Unable to add extra info to the street graph. Exiting.");
			System.exit(-1);
		}
	}
	
	/* build the graph based on the street visits */
	public static TreeMap<Long, Double> loadPRGraph()
	{
		/* Should get the ways weights from the logs. */
		BufferedReader reader = null;
		TreeMap<Long,Double> PRGraph = new TreeMap<Long,Double>();
		try {
			String line = null;
			reader = new BufferedReader(new FileReader(GenericParams.mapConfig.getPRGraphFilename()));
			while( (line = reader.readLine()) != null )
			{
				if( line.startsWith("#") )
					continue;
				String ws[] = line.split(" ");
				Long streetID = Long.parseLong(ws[0]);
				Double pr = Double.parseDouble(ws[1]);
				PRGraph.put(streetID, pr);
			}
			reader.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return PRGraph;
	}

	/* Discover the closest 3 traffic light masters */
	public static GeoTrafficLightMaster discoverClosestTrafficLightMaster(double lat, double lon) {

		ArrayList<GeoTrafficLightMaster> mtl = (ArrayList<GeoTrafficLightMaster>) 
				SimulationEngine.getInstance().getMasterTrafficLights();
		
		GeoTrafficLightMaster mtlInRange = mtl.get(0);
		
		double maxDist = Utils.distance(lat, lon,
				mtlInRange.getCurrentPos().lat, mtlInRange.getCurrentPos().lon);

		double dist = 0;

		for (int i = 0; i < mtl.size(); i++) {
			GeoTrafficLightMaster s = mtl.get(i);
			dist = Utils.distance(lat, lon,
					s.getCurrentPos().lat, s.getCurrentPos().lon);
			if (dist < RoutingApplicationParameters.distMax && dist < maxDist) {
					maxDist = dist;
					mtlInRange = s;
			}
		}

		return mtlInRange;
	}
	/**
	 * Just for testing...
	 * @param args
	 * @throws InterruptedException
	 */
//	public static void main(String[] args) throws InterruptedException {
//		long microseconds, end, start = System.nanoTime();
//		
////		TreeMap<Long, Way> graph = EngineUtils.loadGraph(
////				"res\\Xml\\streets_rez_san-francisco.osm",
////				"res\\Xml\\streets_graph_san-francisco.osm");
//		TreeMap<Long, Way> graph = EngineUtils.loadGraph(
//				"res\\XmlBeijing\\streets_rez_beijing.osm",
//				"res\\XmlBeijing\\streets_graph_beijing.osm");
//		end = System.nanoTime();
//		microseconds = (end - start) / 1000;
//		logger.info("graph loaded in " + microseconds / 1000.0 + " milliseconds");
//		Thread.sleep(2000);
//		
//		start = System.nanoTime();
//		/* Generate peano keys for each road */
//		ArrayList<PeanoKey> peanoKeys = new ArrayList<PeanoKey>();
//		SphericalMercator mercator = new SphericalMercator();
//		for (Way w : graph.values()) {
//			peanoKeys.addAll(w.buildPeanoKeys(mercator));
//		}
//		end = System.nanoTime();
//		microseconds = (end - start) / 1000;
//		logger.info("PeanoKeys built in " + microseconds / 1000.0 + " milliseconds");
//		Thread.sleep(2000);
//		
//		
//		start = System.nanoTime();
//		/* sort the PeanoKey vector */
//		Collections.sort(peanoKeys);
//		end = System.nanoTime();
//		microseconds = (end - start) / 1000;
//		logger.info("PeanoKeys sorted in " + microseconds / 1000.0 + " milliseconds");
//		Thread.sleep(2000);
//		
//		
//		logger.info(peanoKeys.size() + " PeanoKeys generated");
//	}
}