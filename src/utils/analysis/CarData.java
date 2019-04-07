package utils.analysis;

/***
 * 
 * @author Andreea
 *
 */
public class CarData {
	private TrafficData fuel = new TrafficData();
	private TrafficData speed = new TrafficData();
	private TrafficData time = new TrafficData();
	
	private int noRoutesTL = 0;
	private int noRoutesDTL = 0;
	

	public int getNoRoutesTL() {
		return noRoutesTL;
	}

	public void setNoRoutesTL(int noRoutesTL) {
		this.noRoutesTL = noRoutesTL;
	}

	public int getNoRoutesDTL() {
		return noRoutesDTL;
	}

	public void setNoRoutesDTL(int noRoutesDTL) {
		this.noRoutesDTL = noRoutesDTL;
	}

	public TrafficData getFuel() {
		return fuel;
	}

	public void setFuel(DataType dataType, double fuel) {
		switch (dataType) {
		case NoTrafficLights:
			this.fuel.setAvgNA(fuel);
			break;
		case WithTrafficLights:
			this.fuel.setAvgTL(fuel);
			break;
		case WithDynamicTrafficLights:
			this.fuel.setAvgDTL(fuel);
			break;
		default:
			break;
		}
	}
	

	public TrafficData getSpeed() {
		return speed;
	}

	public void setSpeed(DataType dataType, Double speed) {
		switch (dataType) {
		case NoTrafficLights:
			this.speed.setAvgNA(speed);
			break;
		case WithTrafficLights:
			this.speed.setAvgTL(speed);
			break;
		case WithDynamicTrafficLights:
			this.speed.setAvgDTL(speed);
			break;
		default:
			break;
		}
	}

	public TrafficData getTime() {
		return time;
	}

	public void setTime(DataType dataType, double time) {
		switch (dataType) {
		case NoTrafficLights:
			this.time.setAvgNA(time);
			break;
		case WithTrafficLights:
			this.time.setAvgTL(time);
			break;
		case WithDynamicTrafficLights:
			this.time.setAvgDTL(time);
			break;
		default:
			break;
		}
	}

	@Override
	public String toString() {
		return noRoutesTL + " " + noRoutesDTL + " " + time.getData() + " " + speed.getData() + " " + fuel.getData();
	}

	public enum DataType {
		NoTrafficLights,
		WithTrafficLights,
		WithDynamicTrafficLights
	}
}
