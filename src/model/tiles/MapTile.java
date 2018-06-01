package model.tiles;

import java.awt.Image;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * MapTile.
 */
public class MapTile extends GenericTile {
	/* Coordinates of the rectangle bounding the tile */
	public Rectangle2D.Double rect;
	public AffineTransform transf = new AffineTransform();

	/* TODO ??? */
	public int carrierAccess = 0;

	/* Images being stored in the current tile */
	Image img;

	/**
	 * Constructor
	 * 
	 * @param x    The line that the tile is in the grid
	 * @param y    The column that the tile is in the grid
	 * @param rect Coordinates of bounding rectangle for tile
	 * @param fileLocation The location where the tile's image is located
	 */
	public MapTile(int x, int y, Rectangle2D.Double rect) {
		id = new Point(y, x);
		replicaCount = 0;
		this.rect = rect;
		/* Initial size of the tile */
		size = 200;
	}
}
