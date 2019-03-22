package model.threadpool.tasks;

import model.GeoTrafficLightMaster;

public class TrafficLightChangeColor implements Runnable{

	private GeoTrafficLightMaster trafficLight;
	
	public TrafficLightChangeColor(GeoTrafficLightMaster trafficLight) {
		
		this.trafficLight = trafficLight;
	}
	
	@Override
	public void run() {
		this.trafficLight.changeColor();
	}
}
