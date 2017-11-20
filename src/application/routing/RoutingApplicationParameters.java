package application.routing;

import application.routing.RoutingApplicationData.RoutingApplicationState;
public class RoutingApplicationParameters {

	/** RoutingApplication in debug mode */
	public static boolean routingapp_debug = true;
	/** The simulation interval (seconds) for which are kept the road costs are kept */
	public static long SamplingInterval = 1800;

	/** Searching algorithm maximum path */
	public static int maxdepth = 10; 

	/** The maximum distance that can accept intersections from the one Area (meters) */
	public static double distMax = 500;

	/** Routing Application states */
	public static RoutingApplicationState state = /*RoutingApplicationState.COST_COLLECTING*/RoutingApplicationState.RUN_USING_ONLY_PAGERANK;

}
