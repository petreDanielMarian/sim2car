package controller.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import model.Entity;
import model.network.Message;

public abstract class NetworkInterface {
	private Entity owner;
	
	private List<Message> inputQueue;
	private List<Message> outputQueue;
	
	private NetworkType type;
	
	/**
	 * 
	 * @return the next Message from the recv queue
	 */
	public abstract Message getNextInputMessage();
	/**
	 * tries to send all the messages it has to send
	 */
	public abstract void processOutputQueue();
	public abstract ArrayList<NetworkInterface> discoversServers();
	public abstract ArrayList<NetworkInterface> discoversPeers();
	

	public NetworkInterface(NetworkType type) {
		/** using synchronizedList for multithreading, a car can receive another message while
		 * processing the current messages queue, same for output queue
		 */
		this.type = type;
		this.inputQueue = Collections.synchronizedList(new LinkedList<Message>());
		this.outputQueue = Collections.synchronizedList(new LinkedList<Message>());
	}
	
	

	/**
	 * 
	 * @param o - object placed to be sent, it will be
	 * serialized when we will send it. This gives the
	 * liberty to look at the message header without
	 * serializing it again
	 */
	public void putMessage(Message m) {
		
		outputQueue.add(m);
	}
	
	public Entity getOwner() {
		return owner;
	}

	public void setOwner(Entity o) {
		owner = o;
	}

	public void send(Message m, NetworkInterface dest) {
		dest.receive(m);
	}

	public void receive(Message m) {
		owner.process(m);
	}
	
	/**
	 * 
	 * @return list with the messages it received so far
	 */
	public List<Message> getInputQueue() {
		
		return this.inputQueue;
	}
	
	/**
	 * 
	 * @return list with the messages it has to send
	 */
	public List<Message> getOutputQueue() {
		
		return this.outputQueue;
	}
	/**
	 * 
	 * @return the subclass network type
	 */
	public NetworkType getType() {
		
		return this.type;
	}
	public String stop()
	{
		String result = "";
		return result;
	}
}
