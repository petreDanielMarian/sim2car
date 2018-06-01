package controller.engine;

import gui.CarView;
import gui.ServerView;
import gui.View;

import java.awt.Color;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import model.Entity;
import model.GeoCar;
import model.GeoServer;
import model.MapPoint;
import model.PixelLocation;
import model.OSMgraph.Way;
import model.parameters.Globals;
import model.parameters.MapConfig;
import model.parameters.MapConfiguration;
import model.tiles.ClientTile;
import model.tiles.GenericTile;

import org.openstreetmap.gui.jmapviewer.JMapViewer;

import utils.SphericalMercator;
import utils.TraceParsingTool;
import application.ApplicationType;
import application.tiles.GeoMap;
import application.tiles.TileApplicationCar;
import application.tiles.TileApplicationServer;
import controller.network.NetworkWiFi;

public class EngineSimulation implements EngineInterface {

	/* Start moment */
	private Date now;
	private boolean run = true;
	private Runnable animate;
	private JMapViewer mapJ;

	private List<GeoCar> cars;
	private List<GeoServer> servs;
	private List<CarView> carsView;
	private List<ServerView> servsView;
	private Vector<Vector<Integer>> areas;
	private TreeMap<Long, Way> graph;
	private BufferedWriter serversLog = null;
	private BufferedWriter peersLog = null;
	private View view;
	
	private MapConfig mapConfig;

	/* The map represents the container with tiles */
	GeoMap map;
	
	private static EngineSimulation _engine = null;

	public static EngineSimulation getInstance() {
		if (_engine == null) {
			new EngineSimulation();
		}
		return _engine;
	}

	private EngineSimulation() {
		mapConfig = MapConfiguration.getInstance(Globals.propertiesFile);
		EngineSimulation._engine = this;
		Globals.zoomLevel += mapConfig.getQuot();
	}

	@Override
	public void setUp() {
		now = new Date(Globals.startTime);
		if (Globals.loadGraph) {
			graph = TraceParsingTool.load_graph(mapConfig.getStreetsFilename(),
					mapConfig.getPartialGraphFilename());
			areas = TraceParsingTool.load_areas(mapConfig.getIndexTableFilename());
		}
		if (Globals.peersLogging || Globals.serversLogging) {
			File log_dir = new File("logs");
			if (!log_dir.exists()) {
				boolean result = log_dir.mkdir();
				if (!result) {
					System.err
							.println(" Error: Logging directory can't be created");
				}
			}
		}
		cars = new ArrayList<GeoCar>();
		servs = new ArrayList<GeoServer>();
		carsView = new ArrayList<CarView>();
		servsView = new ArrayList<ServerView>();
//		carsMobility = new ArrayList<CarMobility>();
		File f = new File("logs");
		if (f.exists() && !f.isDirectory()) {
			f.delete();
			f.mkdir();
		}
		if (Globals.peersLogging) {
			try {
				peersLog = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream("logs\\logpeers_"
								+ System.currentTimeMillis() / 1000 + ".txt")));
			} catch (FileNotFoundException e) {
				Logger.getLogger(EngineSimulation.class.getName()).log(
						Level.SEVERE, null,
						"Eroare Deschidere log peers \n" + e);
				e.printStackTrace();
			}
		}
		if (Globals.serversLogging) {
			try {
				serversLog = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream("logs\\logservers_"
								+ System.currentTimeMillis() / 1000)));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				Logger.getLogger(EngineSimulation.class.getName()).log(
						Level.SEVERE, null,
						"Eroare Deschidere log severs \n" + e);
				e.printStackTrace();
			}
		}
		Integer sync = new Integer(0);
		/* Create grid with N * M tiles */
		map = new GeoMap(mapConfig.getN(), mapConfig.getM(), sync);

		mapJ = new JMapViewer();
		mapJ.setDisplayPositionByLatLon(mapConfig.getMapCentre().getX(),
				mapConfig.getMapCentre().getY(), 11);

		carsView = new ArrayList<CarView>();

		/* Create map of servers */
//		cars = getAllCarData(mapConfig.getTracesListFilename(), sync, carsView,
//				carsMobility);
		cars = getAllCarData(mapConfig.getTracesListFilename(), sync, carsView);
		servs = getServersData(mapConfig.getAccessPointsFilename());

		if (Globals.showGUI) { // we don't really need GUI
			ServerView servView = new ServerView(mapConfig.getN(),
					mapConfig.getM(), servs, mapJ);
			view = new View(mapConfig.getN(), mapConfig.getM(), mapJ,
					servView, carsView, null);
		}

		animate = new Animation(this);
	}

	@Override
	public void start() {
		if (Globals.showGUI)
			view.showView();
		new Thread(animate).start();

	}

	/**
	 * Returns a random number of traces
	 * 
	 * @param route_index
	 *            - Path to the Calea catre fisierul care indica traceurile
	 *            taxiurilor.
	 * @param sync
	 *            - ?
	 * @param serv
	 *            - Serverul
	 * @return Lista Clientilor posibili
	 */
	List<GeoCar> getAllCarData(String route_index, Integer sync,
			List<CarView> carsViews) {
		FileInputStream fstream = null;
		/* List of clients */
		List<GeoCar> rutes = new ArrayList<GeoCar>();
		try {
			fstream = new FileInputStream(route_index);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					fstream));
			String strLine;
			// Read File Line By Line
			/*
			 * Count is used to count the number of taxi cabs and use as id in
			 * GeoClient
			 */
			int count = 0;
			/* Read data about traces */
			while ((strLine = br.readLine()) != null) {
				if (Globals.randomCarsSelect == 0.0) {
					if (Math.random() > 0.1) {
						continue;
					}
				}
				if (count == Globals.carsCount)
					break;
				System.out.println(" We opened " + count + ". " + strLine);
				StringTokenizer st = new StringTokenizer(strLine, " ", false);
				st.nextToken();
				String srcId = st.nextToken();
				srcId = srcId.substring(4, srcId.length() - 1);

				/* Create client */
				GeoCar car = new GeoCar(count);
				car.addApplication(new TileApplicationCar(car));
				car.addNetworkInterface(new NetworkWiFi(car));

				String path = mapConfig.getTracesPath() + srcId + ".txt";

//				CarMobility carM = new CarMobility(count, path, car);
				if (Globals.showGUI) {
					CarView carV = new CarView(count, mapJ, car);
					/* set color of client */
					Color c = new Color((float) Math.random(),
							(float) Math.random(), (float) Math.random());
					carV.setColor(c);
					carsViews.add(carV);
				}
				/*
				 * If current time is too high, we take the earliest moment
				 * before that.
				 */
				if (car.getCurrentPos() != null) {
					if (car.getCurrentPos().timestamp.before(now)) {
						now.setTime(car.getCurrentPos().timestamp.getTime());
					}
				}
//				carsMob.add(carM);
				rutes.add(car);
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
		return rutes;

	}

	List<GeoServer> getServersData(String route_index) {
		FileInputStream fstream = null;
		SphericalMercator mercat = new SphericalMercator();
		List<GeoServer> servers = new ArrayList<GeoServer>();
		try {
			System.out.println("Path received " + route_index);
			fstream = new FileInputStream(route_index);
			BufferedReader br = new BufferedReader(
					new InputStreamReader(fstream));
			String strLine;
			int count = 0;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				if (strLine.startsWith("#"))
					continue;
				StringTokenizer st = new StringTokenizer(strLine, " ", false);

				double lat = Double.parseDouble(st.nextToken());
				double lon = Double.parseDouble(st.nextToken());

				PixelLocation pix = mercat.LatLonToPixelLoc(lat, lon, Globals.zoomLevel);

				pix.tile.y -= mapConfig.getBaseRow();
				pix.tile.x -= mapConfig.getBaseColumn();

				/* If not on map, we don't consider it */
				count++;
				MapPoint mp = new MapPoint(pix, lat, lon, false, 0);
				GeoServer s = new GeoServer(mapConfig.getN(), mapConfig.getM(), count, mp);
				s.addApplication(new TileApplicationServer(s));
				s.addNetworkInterface(new NetworkWiFi(s));
				servers.add(s);
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
		return servers;
	}

	void showChart() {
		int cnt = 0;
		ArrayList<GeoCar> cs = new ArrayList<GeoCar>();
		for (int i = 0; i < cars.size(); i++) {
			TileApplicationCar c_app = (TileApplicationCar) cars.get(i)
					.getApplication(ApplicationType.TILES_APP);
			if (cnt == 50)
				break;
			if (c_app.toPeers != 0 || c_app.fromPeers != 0) {
				cs.add(cars.get(i));
				cnt++;
			}
		}
		view.showChart(1, "Tiles Exchances", cs);
	}

	void printClientsStatus(List<GeoCar> rutes) {
		String header = printHeaderClients();
		if (Globals.peersConsoleLogging)
			System.out.println(header);

		if (Globals.peersLogging && peersLog != null) {
			try {
				peersLog.write(header + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			for (int ji = 0; ji < rutes.size(); ji++) {
				GeoCar gc = rutes.get(ji);
				// if ( gc.getCurrentPos() != null ) {
				String log = gc.getApplication(ApplicationType.TILES_APP).getInfoApp();
				if (Globals.peersConsoleLogging)
					System.out.println(log);
				if (Globals.peersLogging && peersLog != null) {
					peersLog.write(log + "\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void printServersStatus(List<GeoServer> servers) {
		String header = printHeaderServers();
		if (Globals.serversConsoleLogging)
			System.out.println(header);
		if (Globals.serversLogging && serversLog != null) {
			try {
				serversLog.write(header + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			for (int ji = 0; ji < servers.size(); ji++) {
				GeoServer gs = servers.get(ji);
				String log = gs.getApplication(ApplicationType.TILES_APP).getInfoApp();
				if (Globals.serversConsoleLogging)
					System.out.println(log);

				if (Globals.serversLogging && serversLog != null) {
					serversLog.write(log + "\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	void updateGlobalStatistics(GeoMap map, List<GeoCar> clients) {

		for (GenericTile gt : map.getTileList()) {
			gt.etheric = 0;
			gt.dmed = 0;
			int cnt = 0;
			for (GeoCar gc : clients) {
				HashMap<Point, ClientTile> localStore =
						(HashMap<Point, ClientTile>) gc.getApplication(ApplicationType.TILES_APP).getData();
				if (localStore.containsKey(gt.id)) {
					gt.etheric++;
					cnt++;
					gt.dmed += gt.id.distance(gc.getCurrentPos().tile);
				}
			}
			if (cnt > 0) {
				gt.dmed /= cnt;
			} else {
				gt.dmed = 999;
			}
		}
	}

	String printHeaderClients() {
		String header = new Date(now.getTime() * 1000).toString() + "\n";
		header += String.format(
				"%5s %15s %15s %15s %15s %7s %15s %15s %15s %15s %15s", "Taxi",
				"WiFi-TX To PEERS", "WiFi-RX From PEERS", "WiFi-TX To SERVERS",
				"WiFi-RX From SERVERS", "Tiles", "TilesTX2PEERS",
				"Tiles-RX-F-PEERS", "Tiles-RX-F-SERVS", "Mem", "Used");
		return header;
	}

	String printHeaderServers() {
		String header = new Date(now.getTime() * 1000).toString() + "\n";
		header += String.format("%8s %15s %15s %15s", "Server",
				"WiFi-TX To PEERS", "WiFi-RX From PEERS", "TilesTX2PEERS");
		return header;
	}

	void printFooter() {
		System.out.println("--------------------------------------------------"
				+ "----------------------------------");
	}

	void printClientsSimpleStatus(List<GeoCar> rutes) {
		for (int ji = 0; ji < rutes.size(); ji++) {
			GeoCar gc = rutes.get(ji);
			if (gc.getCurrentPos() != null) {
				System.out.println(gc.getApplication(ApplicationType.TILES_APP).getInfoApp());
			}
		}
		printFooter();
	}

	@Override
	public View getView() {
		return view;
	}

	public List<GeoCar> getPeers() {
		return cars;
	}

	public List<GeoServer> getServers() {
		return servs;
	}

	public TreeMap<Long, Way> getGraph() {
		return graph;
	}

	public Vector<Vector<Integer>> getAreas() {
		return areas;
	}

	public void stopSimulation() {
		run = false;
	}

	public Date getNow() {
		return now;
	}

	public boolean isRunning() {
		return run;
	}

	public List<GeoCar> getCars() {
		return cars;
	}

	public List<CarView> getCarsView() {
		return carsView;
	}

	public GeoMap getMap() {
		return map;
	}

	public JMapViewer getMapJ() {
		return mapJ;
	}

	public BufferedWriter getServersLog() {
		return serversLog;
	}

	public BufferedWriter getPeersLog() {
		return peersLog;
	}

	public Entity getCarById(int index) {
		return this.cars.get(index);
	}
	public MapConfig getMapConfig()
	{
		return mapConfig;
	}
}