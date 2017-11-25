package application;

import model.network.Message;

/**
 * Abstracts the behaviour of an application running on a mobile device inside the car.
 */
public abstract class Application {
	
	public long id;
	public abstract boolean getStatus();
	public abstract String run();
	public abstract String stop();
	public abstract String getInfoApp();
	public abstract Object getData();
	public abstract ApplicationType getType();
	public abstract void process(Message m); 
}