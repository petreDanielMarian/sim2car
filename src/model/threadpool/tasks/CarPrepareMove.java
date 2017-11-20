package model.threadpool.tasks;

import model.GeoCar;

public class CarPrepareMove implements Runnable {

	private GeoCar car;
	
	public CarPrepareMove(GeoCar car) {
		
		this.car = car;
	}
	
	@Override
	public void run() {
		this.car.prepareMove();
	}
}
