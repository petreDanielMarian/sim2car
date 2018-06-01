package application.trafficLight;

import java.util.logging.Logger;

import model.GeoTrafficLightMaster;
import model.network.Message;
import model.network.MessageType;
import application.Application;
import application.ApplicationType;
import controller.network.NetworkInterface;
import controller.network.NetworkType;

public class ApplicationTrafficLightControl extends Application {

	private final static Logger logger = Logger.getLogger(ApplicationTrafficLightControl.class.getName());

	/* The type of application  */
	private ApplicationType type = ApplicationType.TRAFFIC_LIGHT_CONTROL_APP;

	/* Reference to the traffic light master object */
	GeoTrafficLightMaster trafficLightMaster;

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

	@Override
	public void process(Message m) {
		//System.out.println("The message received type is " + m.getType());

		if( m.getType() == MessageType.ADD_WAITING_QUEUE )
		{
			ApplicationTrafficLightControlData data = (ApplicationTrafficLightControlData)m.getPayload();
			 if( data != null )
			 {
					 //System.out.println("Add car " + data.getCarId() + "to traffic light " + trafficLightMaster.getId() + " waiting queue");
					 trafficLightMaster.addCarToQueue(data);
			 }
		}
	}

	@Override
	public String stop() {
		return null;
	}
}
