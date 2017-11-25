package utils.tracestool.parameters;

public class TraceJunctionsDetectionParams {
	/* The maxim distance between 2 trace nodes */
	public static int max_dist = 5000; /* meters */
	/* The maxim depth for the finding paths algorithm */
	public static int max_depth = 12;
	public static boolean debug = false;
	/* Select the traces type used as sources: false - Interpolated traces; true - Corrected traces. */
	public static boolean useCorrectedTrace = false;

	/* Calculate the speed */
	public static boolean speedDetectionOn = true;
	/* MaxSpeed Allowed */
	public static double maxSpeed = 37.5;
}
