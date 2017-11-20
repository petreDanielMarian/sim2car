package utils.tracestool.parameters;

public class TraceInterpolateParams {
	/* The maxim time interval limit */
	public static int max_limit = 4;
	/* The maxim depth for the finding paths algorithm */
	public static int max_depth = 10;
	public static boolean debug = false;
	/* activate for Rome and Beijing traces the busy attribute for cabs */
	public static boolean taxi_logic_on = true;
	public static boolean busy_on = GenericParams.mapConfig.getCity().contains("rome") || GenericParams.mapConfig.getCity().contains("beijing") ? true : false;
}
