package model.threadpool.tasks;

import model.GeoCar;

public class CarApplicationsRun implements Runnable {

	private GeoCar car;
	
	public CarApplicationsRun(GeoCar car) {
		
		this.car = car;
	}
	
	@Override
	public void run() {
		this.car.runApplications();
	}
}
