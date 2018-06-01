package utils;

import java.util.Random;

public class Range {

	double base;
	double variance;
	private Random r;

	public Range(double low, double high) {
		r = new Random();
		base = low;
		variance = high - low;
	}

	public double getValue() {
		double fact = r.nextDouble();
		return base + fact * variance;
	}
}
