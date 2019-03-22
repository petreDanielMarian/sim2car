package application.multipleIntersections;

import application.Application;
import application.ApplicationType;
import application.trafficLight.ApplicationTrafficLightControlData;
import controller.network.NetworkInterface;
import controller.network.NetworkType;
import model.GeoTrafficLightMaster;
import model.network.Message;
import model.network.MessageType;

public class SynchronizeIntersectionsApplication extends Application{

	/* The type of application  */
	private ApplicationType type = ApplicationType.SYNCHRONIZE_INTERSECTIONS_APP;

	/* Reference to the traffic light master object */
	GeoTrafficLightMaster trafficLightMaster;
	
	public boolean isActive = false;
	
	
	public SynchronizeIntersectionsApplication(GeoTrafficLightMaster trafficLightMaster){
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
	public String stop() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getInfoApp() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ApplicationType getType() {
		return type;
	}

	@Override
	public void process(Message m) {

		if( m.getType() == MessageType.SYNCHRONIZE_TRAFFIC_LIGHTS )
		{
			SynchronizeIntersectionsData data = (SynchronizeIntersectionsData)m.getPayload();
			 if( data != null )
			 {
				 trafficLightMaster.synchronizeWithNeighbors(data);
			 }
		}		
	}

}
