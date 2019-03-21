package model;

/**
 * The class used to keep data about a point in the trace at a certain time
 **/
public class Location {

	/** position */
	public double lat, lon;

	/** timestamp */
	public long timestamp;

	/** flag indicating occupancy */
	public int occupied;

	public long wid;

	public Location(double lat, double lon, int o, long timestamp) {
		this.lat = lat;
		this.lon = lon;
		this.timestamp = timestamp;
		occupied = o;
	}

	public void setIdStreet(Long id) {
		wid = id;
	}

	public String toString() {
		return "lat:" + lat + " long:" + lon + "timestamp " + timestamp
				+ "ocupat " + occupied + " Id_strada" + wid + "\n";
	}
}