package model.personality;

public interface Personality {
	/**
	 * Get safety distance between this driver's car and the car in front.
	 * If the distance is too low, the driver will have to break.
	 */
	public double getSafetyDistance(double speed);
	
	/** 
	 * Get the maximum distance between this car and the car in front needed
	 * for this car to be influenced by the other car. If the distance between
	 * the two cars is greater than this, then this car's speed is not
	 * influenced at all (e.g. there's no need to slow down).
	 */
	public double getInfluenceDistance(double speed);
	
	/**
	 * The distance this driver wants between his car and the car in front.
	 * It's less than the influence distance, more than the safety distance.
	 */
	public double getDesiredDistance(double speed);
	
	/** Return the acceleration when there's no obstacle in front of the car. */
	public double getAccelerationFreeDriving();
	
	/** Return the maximum deceleration. */
	public double getAccelerationMaximumBrake();
	
	/** Return the deceleration when approaching intersection. */
	public double getIntersectionDeceleration();
	
	/**
	 * The wanted speed on this road (depends on the road type)
	 * TODO: change roadIndex to a Way object or required speed
	 */
	public double getWantedSpeed(long roadIndex);
	
	/**
	 * The wanted speed on this road (depends on the road type) when approaching
	 * an intersection
	 * TODO: maybe replace distFromInter with a intersection object (with
	 * references to traffic lights etc.
	 */
	public double getWantedSpeedInfluenced(double distanceFromIntersection, double requiredSpeed);
	
	/** Time in seconds for this driver's reaction time */
	public double getReactionTimeFreeDriving();
}