package model;

public class LocationParse extends Location {

	public int indexNodes;
	public boolean ontheStreetFirst = false;

	public LocationParse(double lat, double lon, int o, long timestamp) {
		super(lat, lon, o, timestamp);
		indexNodes = -1;
	}

	public void setIdStreet(Long id) {
		super.setIdStreet(id);
	}

	public void setIndexNodes(int idx) {
		indexNodes = idx;
	}

	public String toString() {
		return "lat:" + lat + " long:" + lon + "timestamp " + timestamp
				+ "busy " + occupied + " street_id" + wid + "\n";
	}
}