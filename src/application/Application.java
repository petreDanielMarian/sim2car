package application;

import model.network.Message;

/**
 * Abstracts the behavior of an application running on a mobile device inside the car.
 * Use this class if you want to add any new functionality such as another method of routing etc.
 */
public abstract class Application {
	public long id;
	public abstract boolean getStatus();
	public abstract String run();
	public abstract String stop();
	public abstract String getInfoApp();
	/* Get the data which was send to application */
	public abstract Object getData();
	public abstract ApplicationType getType();
	/* Process the received message and take decisions */
	/* Such as when a car sends a request to the server for a new ROUTE */ 
	public abstract void process(Message m); 
}