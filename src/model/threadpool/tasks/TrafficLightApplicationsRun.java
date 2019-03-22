package model.threadpool.tasks;

import model.GeoTrafficLightMaster;

public class TrafficLightApplicationsRun implements Runnable {

	private GeoTrafficLightMaster trafficLightMaster;
	
	public TrafficLightApplicationsRun(GeoTrafficLightMaster trafficLightMaster) {
		this.trafficLightMaster = trafficLightMaster;
	}
	
	@Override
	public void run() {
		this.trafficLightMaster.runApplications();
	}
}
