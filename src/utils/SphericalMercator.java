package utils;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import model.PixelLocation;

/**
 * Utility class for Mercator.
 */
public class SphericalMercator {
	private int tileSize;
	private final static double initialResolution = 2 * Math.PI * 6378137;
	private final static double originShift = Math.PI * 6378137;

	private class SimpleRectangle {
		double minx;
		double maxx;
		double miny;
		double maxy;

		public SimpleRectangle(double minx, double miny, double maxx,
				double maxy) {
			this.minx = minx;
			this.maxx = maxx;
			this.miny = miny;
			this.maxy = maxy;

		};
	};

	public SphericalMercator() {
		this(256);
	}

	private SphericalMercator(int tileSize) {
		this.tileSize = tileSize;
	}

	/**
	 * Converts given lat/lon in WGS84 Datum to XY in Spherical Mercator EPSG:900913.
	 */
	public Point2D.Double LatLonToMeters(double lat, double lon) {
		double mx = lon * originShift / 180.0;
		double my = Math.log(Math.tan((90 + lat) * Math.PI / 360.0))
				/ (Math.PI / 180.0);
		my = my * originShift / 180.0;
		return new Point2D.Double(mx, my);
	}

	/**
	 * Converts XY point from Spherical Mercator EPSG:900913 to lat/lon in WGS84 Datum.
	 */
	public Point2D.Double MetersToLatLon(double mx, double my) {
		double lon = (mx / originShift) * 180.0;
		double lat = (my / originShift) * 180.0;

		lat = 180 / Math.PI
				* (2 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0);
		return new Point2D.Double(lat, lon);
	}

	/**
	 * Converts pixel coordinates in given zoom level of pyramid to EPSG:900913.
	 */
	public Point2D.Double PixelsToMeters(double px, double py, int zoom) {
		double res = this.Resolution(zoom);
		double mx = px * res - originShift;
		double my = py * res - originShift;
		return new Point2D.Double(mx, my);
	}

	/**
	 * Converts EPSG:900913 to pyramid pixel coordinates in given zoom level.
	 */
	public Point2D.Double MetersToPixels(double mx, double my, int zoom) {
		double res = this.Resolution(zoom);
		double px = (originShift + mx) / res * tileSize;
		double py = (originShift - my) / res * tileSize;
		return new Point2D.Double(px, py);
	}

	/**
	 * Returns a tile covering region in given pixel coordinates.
	 */
	public Point PixelsToTile(double px, double py) {
		int tx = (int) (Math.ceil(px / (float) (this.tileSize)) - 1);
		int ty = (int) (Math.ceil(py / (float) (this.tileSize)) - 1);
		return new Point(tx, ty);
	}

	/**
	 * Move the origin of pixel coordinates to top-left corner.
	 */
	public Point2D.Double PixelsToRaster(double px, double py, int zoom) {
		int mapSize = this.tileSize << zoom;
		return new Point2D.Double(px, mapSize - py);
	}

	/**
	 * Returns tile for given mercator coordinates.
	 */
	public Point MetersToTile(double mx, double my, int zoom) {
		Point2D.Double p = this.MetersToPixels(mx, my, zoom);
		return this.PixelsToTile(p.x, p.y);
	}

	/**
	 * Returns tile for given mercator coordinates.
	 */
	public Point LatLonToTile(double lat, double lon, int zoom) {
		Point2D.Double met = LatLonToMeters(lat, lon);
		Point2D.Double p = this.MetersToPixels(met.x, met.y, zoom);
		return this.PixelsToTile(p.x, p.y);
	}

	/**
	 * Returns tile for given mercator coordinates.
	 */
	public PixelLocation LatLonToPixelLoc(double lat, double lon, int zoom) {
		Point2D.Double met = LatLonToMeters(lat, lon);
		Point2D.Double p = this.MetersToPixels(met.x, met.y, zoom);
		Point tile = this.PixelsToTile(p.x, p.y);
		Point location = new Point(0, 0);
		location.x = (int) (p.x - tile.x * tileSize);
		location.y = (int) (p.y - tile.y * tileSize);
		PixelLocation pl = new PixelLocation();
		pl.tile = tile;
		pl.position = location;
		pl.metricLocation = p;
		return pl;
	}

	/**
	 * Returns bounds of the given tile in EPSG:900913 coordinates.
	 */
	private SimpleRectangle TileBounds(int tx, int ty, int zoom) {
		Point2D.Double min = this.PixelsToMeters(tx * this.tileSize, ty
				* this.tileSize, zoom);
		Point2D.Double max = this.PixelsToMeters((tx + 1) * this.tileSize,
				(ty + 1) * this.tileSize, zoom);
		return new SimpleRectangle(min.x, min.y, max.x, max.y);
	}

	/**
	 * Returns bounds of the given tile in latitude/longitude using WGS84 datum.
	 */
	public Rectangle2D.Double TileLatLonBounds(int tx, int ty, int zoom) {
		SimpleRectangle bounds = this.TileBounds(tx, ty, zoom);
		Point2D.Double min = this.MetersToLatLon(bounds.minx, bounds.miny);
		Point2D.Double max = this.MetersToLatLon(bounds.maxx, bounds.maxy);
		// TODO ???
		// return ( minLat, minLon, maxLat, maxLon );
		return null;
	}

	/**
	 * Resolution (meters/pixel) for given zoom level (measured at Equator).
	 */
	public double Resolution(int zoom) {
		return initialResolution / Math.pow(2, zoom);
	}

	/**
	 * Maximal scale down zoom of the pyramid closest to the pixelSize.
	 */
	public int ZoomForPixelSize(int pixelSize) {
		for (int i = 0; i < 30; i++)
			if (pixelSize > this.Resolution(i))
				return (i != 0) ? i - 1 : 0; // We don't want to scale up
		return 0;
	}

	/**
	 * Converts TMS tile coordinates to Google Tile coordinates.
	 * Coordinate origin is moved from bottom-left to top-left corner of
	 * the extent.
	 */
	public Point GoogleTile(int tx, int ty, int zoom) {
		return new Point(tx, (int) (Math.pow(2, zoom) - 1 - ty));
	}

	/**
	 * Compute the distance between 2 Mercator points.
	 * The returned distance is in meters.
	 */
	public double distance(double lat1, double lon1, double lat2, double lon2) {
		double earthRadius = 6378137;
		double rad_lat1 = lat1 * Math.PI / 180.0;
		double rad_lat2 = lat2 * Math.PI / 180.0;
		double delta_lat = (lat2 - lat1) * Math.PI / 180.0;
		double delta_lon = (lon2 - lon1) * Math.PI / 180.0;
		
		double a = Math.sin(delta_lat / 2.0) * Math.sin(delta_lat / 2.0) + 
				   Math.cos(rad_lat1) * Math.cos(rad_lat2) *
				   Math.sin(delta_lon / 2.0) * Math.sin(delta_lon / 2.0);
		
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return earthRadius * c;
	}
}