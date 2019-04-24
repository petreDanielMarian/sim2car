package model.parameters;


import java.util.Vector;
import application.ApplicationType;
import application.ApplicationUtils;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import controller.network.NetworkType;
import controller.network.NetworkUtils;

/**
 * Contains command line parameters and static fields that are used in multiple
 * classes and can't be attached by meaning to a single class.
 * e.g.:
 *     --useWifi is specific to the application running on a car, not to the
 * simulator, so we shouldn't add it here.
 *     --maxCarCount is specific to the simulator - the maximum number of cars
 * allowed to be simulated at once, hence its place is here
 * 
 * This class should be instantiated once in the simulation engine and its
 * fields referred statically by the other classes.
 * 
 * @author Mariana
 */
public class Globals {
	
	@Parameter(names = {"--propertiesFile", "-prop"}, description = "The file with the properties for the map to be loaded.", required = true)
    public static String propertiesFile = null;

    @Parameter(names = {"--showGUI", "-gui"}, description = "Display the graphic interface with the application.", arity = 1)
    public static boolean showGUI = true;
	
	@Parameter(names = {"--serversLogging", "-slog"}, description = "Enable logging for servers.", arity = 1)
    public static boolean serversLogging = true;
	
	@Parameter(names = {"--peersLogging", "-plog"}, description = "Enable logging for peers.", arity = 1)
    public static boolean peersLogging = true;
	
	@Parameter(names = {"--serversConsoleLogging", "-sclog"}, description = "Show log messages in console for servers.", arity = 1)
    public static boolean serversConsoleLogging = true;
	
	@Parameter(names = {"--peersConsoleLogging", "-pclog"}, description = "Show log messages in console for peers.", arity = 1)
    public static boolean peersConsoleLogging = true;
    
	@Parameter(names = {"--debug"}, description = "The length of a clock tick.")
	public static int debug = 0;
	
	@Parameter(names = {"--carsCount"}, description = "The number of cars simulated.")
    public static int carsCount = 1300;
	
	@Parameter(names = {"--maxWaitingTime"}, description = "The maximum simulation time a car can wait at a traffic light.")
    public static int maxWaitingTime = 120;

	@Parameter(names = {"--normalTrafficLightTime"}, description = "The normal amount of time a traffic light is green or red.")
    public static int normalTrafficLightTime = 50;
	
	@Parameter(names = {"--minTrafficLightTime"}, description = "The minimum amount of time a traffic light is green or red.")
    public static int minTrafficLightTime = 15;
	
	@Parameter(names = {"--maxTrafficLightTime"}, description = "The maximum amount of time a traffic light can be green or red.")
    public static int maxTrafficLightTime = 90;
	
	@Parameter(names = {"--passIntersectionTime"}, description = "The time needed by a car to pass the intersection.")
    public static int passIntersectionTime = 3;
	
	@Parameter(names = {"--loadGraph"}, description = "Activate the loading graph.", arity = 1)
    public static boolean loadGraph = false;
	
	@Parameter(names = {"--simulationDays"}, description = "Duration of the simulation in days.")
    public static int simulationDays = 7;
	
	@Parameter(names = {"--randomCarsSelect"}, description = "Set the percentage of cars to use in the simulation: 0.0 == none, 1.0 = all.")
    public static double randomCarsSelect = 1.0;
	
	@Parameter(names = {"--timeInterval"}, description = "The distance between two clock ticks in seconds.")
	public static int timePeriod = 1;
	
	@Parameter(names = {"--waitTime"}, description = "How much the simulator should wait between two clock ticks. It is given in milliseconds.")
	public static int waitTime = 100;
	
	@Parameter(names = {"--startTime"}, description = "The starting time moment in the simulation (in seconds from the epoch).")
    public static long startTime = 1211018334;
	
	@Parameter(names = {"--maxSegmentLength", "--maxSegLen"}, description = "The maximum distance (in meters) between consecutive points on a road.")
    public static double maxCellLen = 3.0;
	
	@Parameter(names = {"--threadPoolLogging"}, description = "Enable or disable thread pool logging")
    public static boolean threadPoolLogging = false;
	 
	@Parameter(names = {"--useTreadPool"}, description = "If true, use the treadpool")
	public static boolean useTreadPool = true;
	
	@Parameter(names = {"--useTrafficLights"}, description = "If true, use traffic lights")
	public static boolean useTrafficLights = false;
	
	@Parameter(names = {"--useDynamicTrafficLights"}, description = "If true, use traffic lights")
	public static boolean useDynamicTrafficLights = false;
	
	@Parameter(names = {"--carIdFuelStatistics"}, description = "The car id used to retrieve fuel consumption statistics.")
    public static long carIdFuelStatistics = 10;
	
	/**
	 * Proxy usage from commandline
	 * --useHttpProxy true  --httpProxyHost HOST  --httpProxyPort X  
	 * --httpProxyUser myuser  --httpProxyPassword mypass
	 */
	@Parameter(names = {"--useHttpProxy"})
	public static String useHttpProxy = "false";
	
	@Parameter(names = {"--httpProxyHost"})
	public static String httpProxyHost = null;
	
	@Parameter(names = {"--httpProxyPort"})
	public static String httpProxyPort = null;
	
	@Parameter(names = {"--httpProxyUser"})
	public static String httpProxyUser = null;
	
	@Parameter(names = {"--httpProxyPassword"})
	public static String httpProxyPassword = null;
	 
    public static int zoomLevel = 14;
    
    private static Globals _instance = null;
    private static final Integer _instance_lock = 1;

    
    /* "SPRINT" vs "EPIDEMIC" */
    @Parameter(names = {"--tilesFwAlg"}, description = "the accepted values are EPIDEMIC and SPRINT")
    public static String tilesForwardingAlgorithm = "SPRINT";
	
	
	public static boolean gatherData = true;

	public static long maxTTL = 3;
	
	public static long maxIterations = 2;

	@Parameter(names = {"--activeApps"}, description = "the accepted values ROUTING,TILES,STREET_VISITS,TRAFFIC_LIGHT_CONTROL."
													 + "Please see ApplicationType for more details\n."
													 + "Multiple applications can be passed using --activeApps=app1,app2,app3,..,appn")
	public static String activeApps = "ROUTING,TRAFFIC_LIGHT_CONTROL";
	/* The default application is ROUTING_APP */
	public static Vector<ApplicationType> activeApplications;

	@Parameter(names = {"--activeNets"}, description = "the accepted values WiFi,3G. "
													 + "Please see NetworkType for more details\n."
													 + "Multiple network interfaces can be passed using --activeNets=net1,net2,net3,..,netn")
	public static String activeNets = "WiFi";
	/* The default network interface is Net_WiFi */
	public static Vector<NetworkType> activeNetInterfaces;

    private Globals() {
		_instance = this;
	}

    public static void setUp( String [] args ) {
    	synchronized (_instance_lock) {
			if (_instance == null)
				new Globals();
				parseArgs(_instance, args );
				NetworkUtils.parseNetInterfaces(activeNets);
				ApplicationUtils.parseApplications(activeApps);
		}
    }
    
	public static void parseArgs(Object object, String [] args ) {
		JCommander jArgs = new JCommander(object);
		jArgs.setAcceptUnknownOptions(true);
		jArgs.parse(args);
	}
}
