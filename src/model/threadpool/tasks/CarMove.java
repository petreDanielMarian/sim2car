package model.threadpool.tasks;

import model.GeoCar;

public class CarMove implements Runnable {

	private GeoCar car;
	
	public CarMove(GeoCar car) {
		
		this.car = car;
	}
	
	@Override
	public void run() {
		this.car.move();
	}
}
