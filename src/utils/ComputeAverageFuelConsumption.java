package utils;

public class ComputeAverageFuelConsumption {
	

	/***
	 * Compute the average fuel consumption
	 * @param totalFuel
	 * @param totalTime
	 * @return [Liter]
	 */
	public static double computeAverageFuelConsumption(double totalFuel, long totalTime) {
		return (totalFuel/totalTime)*3.6;
	}
	
	/***
	 * Return the fuel consumption rate in mL/s
	 * @param speed (m/s)
	 * @param acceleration (m/s2)
	 * @param deltaTime (seconds)
	 * @return
	 */
	public static double computeFuel(double speed, double acceleration) {
		double f_idle = 0.375; // [mL/s]
		double Rt = 1400 * acceleration + 2060.1 + 0.4 * Math.pow(speed, 2);
		double x1 = 0.00009 * Rt * speed; // [mL/s]
		double x2 = 0.042 * Math.pow(acceleration, 2) * speed;
		
		return (f_idle + x1 + x2);
	}

}
