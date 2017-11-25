package utils.tracestool.parameters;

public class OSM {

	public final static double SQUARE_L = 0.0011357 * 5; /* an edge of 500 m */
	public final static double DIF = SQUARE_L;
	public final static int SQUARE_LM = 100;
	public final static double EarthRadius = 6380;
	public final static String [] streets_type  = new String[] {
															"motorway", "motorway_link", "trunk", "trunk_link", "primary",
															"primary_link", "secondary", "secondary_link", "tertiary", "tertiary_link",
															"living_street", "residential", "unclassified", "service", "track", "road"
														};
	public final static String [] restricted_streets_type  = new String[] {
															"tram", "footway" 
	};
	public final static int GenModPartialFile = 1;
	public final static int GenRawPartialFile = 0;

}
