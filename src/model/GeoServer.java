package model;

import application.Application;


/**
 * A WiFi Access Point having also a server role.
 */
public class GeoServer extends Entity {

	public GeoServer(int n, int m, int id, MapPoint location) {
		super(id);
		setCurrentPos(location);
	}

	public String runApplications() {
		String result = "";
		for (Application application : this.applications) {
			result += application.run();
		}
		return result;
	}

}