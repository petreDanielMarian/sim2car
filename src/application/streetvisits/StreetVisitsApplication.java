package application.streetvisits;

import model.GeoCar;
import model.MapPoint;
import model.network.Message;
import application.Application;
import application.ApplicationType;

public class StreetVisitsApplication extends Application {
	GeoCar car;
	MapPoint previousPos = null;
	
	public StreetVisitsApplication(GeoCar car) {
		this.car = car;
	}

	@Override
	public boolean getStatus() {
		return true;
	}

	@Override
	public String run() {
		String result = "";
		if (previousPos == null && car.getCurrentPos() != null)
			result = car.getCurrentPos().wayId + "\n";
		if (previousPos != null && car.getCurrentPos() != null &&
				previousPos.wayId != car.getCurrentPos().wayId)
			result = car.getCurrentPos().wayId + "\n";
		previousPos = car.getCurrentPos();
		return result;
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
		return ApplicationType.STREET_VISITS_APP;
	}

	@Override
	public void process(Message m) {}

	@Override
	public String stop() {
		return null;
	}

}
