package utils.tracestool.parameters;

public class TraceCorrectionParams {
	/* the number of iteration for the Trace Correction Algorithm */
	public static int max_iter = 10;
	/* The maxim number of unresolved candidates for final phase of Trace Correction Algorithm */
	public static int max_cand_nr = 100;
	public static boolean debug = false;
	/* The maximum distance for candidates of phase3 */
	public static int dist_phase3 = 100;
	/* The maximum angle for candidates of phase4 */
	public static int max_angle = 90;
}
