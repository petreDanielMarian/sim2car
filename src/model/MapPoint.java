package model;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Date;

import model.OSMgraph.Node;
import model.parameters.Globals;
import utils.SphericalMercator;

/**
 * Position of a car in trace, with the data correctly mapped to the geomap
 */
public class MapPoint implements Serializable {

	private static final long serialVersionUID = -6562197054373098605L;
	/** Grid coordinates of the tile where the car is in */
	public Point tile;
	/** Position on the original map */
	public Point2D location;
	/** Mercator coordinate in pixels */
	public Point2D metricLocation;
	/** Coordinates of the point */
	public double lat, lon;
	/** Flag - occupied or not */
	public boolean occupied;
	/** Timestamp when the information is requested */
	public Date timestamp;
	/** The id of the node if it is available */
	public long nodeId = -1;
	/** The id of the way this point is on */
	public long wayId;
	/**
	 * The direction in which the car is driven across the road 
	 * -1 - reverse (against nodes vector)  // is verified only on not two-way streets
	 *  1 - normal (default)
	 */
	public int direction = 1;
	/** The street's segment number */
	public Integer segmentIndex = -1;
	/** The cell in the segment in which this point is at */
	public Long cellIndex = -1L;

	public MapPoint(PixelLocation position, double lat, double lon,
			boolean occupied, long time) {
		this.tile = position.tile;
		this.metricLocation = position.metricLocation;
		this.location = new Point2D.Double(position.position.x,
				position.position.y);
		this.lat = lat;
		this.lon = lon;
		this.occupied = occupied;
		timestamp = new Date(time);
	}

	static public MapPoint getMapPoint(double lat, double lon, int isOccupied, long wayId) {
		return getMapPoint(lat, lon, ((isOccupied == 0) ? false : true), wayId);
	}

	static public MapPoint getMapPoint(double lat, double lon, int isOccupied, long wayId, long nodeID ) {
		MapPoint mp = getMapPoint(lat, lon, ((isOccupied == 0) ? false : true), wayId);
		mp.nodeId = nodeID;
		return mp;
	}

	static public MapPoint getMapPoint( Node n ) {
		MapPoint mp = getMapPoint(n.lat, n.lon, false, n.wayId);
		mp.nodeId = n.id;
		return mp;
	}


	static public MapPoint getMapPoint(double lat, double lon, boolean isOccupied, long wayId) {
		SphericalMercator sm = new SphericalMercator();
		MapPoint point;
		PixelLocation pix = sm.LatLonToPixelLoc(lat, lon, Globals.zoomLevel);

		/*TODO: This should be updated */
		pix.tile.y -= 6177;
		pix.tile.x -= 13457;

		point = new MapPoint(pix, lat, lon, isOccupied, -1); /* time doesn't matter */
		point.wayId = wayId;
		return point;
	}

	public long distanceTo(MapPoint dest) {
		return (long) this.metricLocation.distance(dest.metricLocation);
	}
	public String toString(){
		return "<MapPoint: (lat, lon): (" + lat + " " + lon + "), wayId = " + wayId + ", segmentIndex = " + segmentIndex + ", cellIndex = " + cellIndex + ">";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MapPoint))
            return false;
        if (obj == this)
            return true;

        MapPoint rhs = (MapPoint) obj;
        double epsilon = 0.00000000001d;
        return Math.abs(this.lat - rhs.lat) < epsilon && 
        		Math.abs(this.lon - rhs.lon) < epsilon;
	}
	
	@Override
	public int hashCode() {
		return new Double(this.lat / this.lon).hashCode();
	}
}