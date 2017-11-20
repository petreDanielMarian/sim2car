package model;

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * Graphical pixel.
 */
public class PixelLocation {

	/* Tile coordinates in the grid */
	public Point tile;
	/* Position on the original map */
	public Point position;
	/* Mercator coordinate in pixels */
	public Point2D metricLocation;

	@Override
	public String toString() {
		return "in tile " + tile.x + " / " + tile.y + ", at pos (" + position.x
				+ "," + position.y + ")";
	}
}
