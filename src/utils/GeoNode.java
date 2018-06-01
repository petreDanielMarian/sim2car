package utils;

public class GeoNode implements Node {

	/* The GPS coordinates of a node */
	private double lat;
	private double lon;

	public GeoNode( double lat, double lon )
	{
		this.lat = lat;
		this.lon = lon;
	}

	@Override
	public Object getX() {
		return lon;
	}

	@Override
	public Object getY() {
		return lat;
	}

}
