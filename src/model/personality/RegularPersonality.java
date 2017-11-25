package model.personality;

import model.OSMgraph.Way;
import model.mobility.MobilityEngine;

public class RegularPersonality implements Personality {

	private double coef2, coef3, coef4, coef5, coef6, coef7, coef8,
			coef9, coef10;

	public RegularPersonality() {
		coef2 = 1.0 - Math.random() * 2;
		coef3 = 0.1 - Math.random() * 0.2;
		coef4 = 5.0 - Math.random() * 10;
		coef5 = 0.1 - Math.random() * 0.2;
		coef6 = 1.0 - Math.random() * 2;
		coef7 = 1.0 - Math.random() * 2;
		coef8 = 1.0 - Math.random() * 2;
		coef9 = 1.0 - Math.random() * 2;
		coef10 = 0.5 - Math.random();
	}

	@Override
	public double getSafetyDistance(double speed) {
		if (speed < 10)
			return 2.0 * speed + coef2;
		return 2.0 * speed + coef4;
	}

	@Override
	public double getInfluenceDistance(double speed) {
		return (5.7 + coef3) * speed + (40.0 + coef4);
	}

	@Override
	public double getDesiredDistance(double speed) {
		if (speed < 10) {
			return 5.0;
		}
		return (0.4 + coef5) * 3.6 * speed + (10.0 + coef6);
	}

	@Override
	public double getAccelerationFreeDriving() {
		return (1.5 + coef7);
	}

	@Override
	public double getAccelerationMaximumBrake() {
		return (-4.0 + coef8);
	}

	@Override
	public double getIntersectionDeceleration() {
		return (-3.0 + coef7);
	}

	@Override
	public double getWantedSpeed(long id) {
		Way way = MobilityEngine.getInstance().getWay(id);
		return way.getMaximumSpeed() + coef9;
	}

	@Override
	public double getWantedSpeedInfluenced(double distanceFromIntersection,
			double requiredSpeed) {
		if (distanceFromIntersection > 90)
			return 100; // speed in km here, should be meters
		double ret = requiredSpeed
				+ (-1.0 / 90.0 * distanceFromIntersection * distanceFromIntersection + 19 / 9 * distanceFromIntersection);

		return ret;
	}

	@Override
	public double getReactionTimeFreeDriving() {
		return (1.6 + coef10);
	}
}
