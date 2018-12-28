package application.trafficLight;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import model.GeoTrafficLightMaster;
import model.network.Message;
import model.network.MessageType;
import model.parameters.Globals;
import application.Application;
import application.ApplicationType;
import controller.network.NetworkInterface;
import controller.network.NetworkType;

/**
 * This class is used to simulated the behavior of the traffic light master from the traffic light
 * control app point of view
 * It adds the car (the sender of the message) to the corresponding waiting queue.
 * The car sends a message to the traffic light master only when the traffic light is red.
 * @author Andreea
 *
 */
public class ApplicationTrafficLightControl extends Application {

	private final static Logger logger = Logger.getLogger(ApplicationTrafficLightControl.class.getName());

	/* The type of application  */
	private ApplicationType type = ApplicationType.TRAFFIC_LIGHT_CONTROL_APP;

	/* Reference to the traffic light master object */
	GeoTrafficLightMaster trafficLightMaster;
	
	/* key = trafficLightMasterId value = (avg_waiting_time, avg_queue_length) */
	public static TreeMap<Long, String> queuesStatistics = new TreeMap<Long, String>();

	/* If it demands a route or not */
	public boolean isActive = false;

	public ApplicationTrafficLightControl(GeoTrafficLightMaster trafficLightMaster){
		this.trafficLightMaster = trafficLightMaster;
	}

	@Override
	public boolean getStatus() {
		return isActive;
	}

	@Override
	public String run() {
		String logs = "";

		NetworkInterface net = trafficLightMaster.getNetworkInterface(NetworkType.Net_WiFi);
		net.processOutputQueue();
		
		return logs;
	}

	@Override
	public String getInfoApp() {
		return null;
	}

	@Override
	public Object getData() {
		return null;
	}

	@Override
	public ApplicationType getType() {
		return type;
	}

	/***
	 * Process the message received from a car. Add the car to the corresponding waiting queue
	 */
	@Override
	public void process(Message m) {

		if( m.getType() == MessageType.ADD_WAITING_QUEUE )
		{
			ApplicationTrafficLightControlData data = (ApplicationTrafficLightControlData)m.getPayload();
			 if( data != null )
			 {
					 trafficLightMaster.addCarToQueue(data);
			 }
		}
	}

	@Override
	public String stop() {
		return null;
	}
	
	public static void writeWaitingTimeStatistics() {
		PrintWriter writer = null;
		try {
			if (Globals.useDynamicTrafficLights)
				writer = new PrintWriter("waitingTime&QueueLength_withDynamicTrafficLights.txt", "UTF-8");
			else if (Globals.useTrafficLights)
				writer = new PrintWriter("waitingTime&QueueLength_withTrafficLights.txt", "UTF-8");

			writer.println("trafficLightMasterId avg_waiting_time[sec] avg_queue_length");
			for( Map.Entry<Long, String> entry : queuesStatistics.entrySet() )
			{
				writer.println(entry.getKey() +" "+entry.getValue());	
			}
			writer.close();

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}		
	}
	
	public static void stopGlobalApplicationActions(){
		writeWaitingTimeStatistics();
	}
	
	public static void saveData(long trafficLightId, double avg_waitingTime, double avg_queueLength) {
		queuesStatistics.put(trafficLightId, avg_waitingTime + " " + avg_queueLength);
	}
}
