package application.tiles;

import gui.MapPanel;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import model.tiles.MapTile;

/**
 * Class to manage the graphical map. 
 */
public class GeoMap extends MapPanel {
	private static final long serialVersionUID = 1L;
	public boolean ok;
	/* Map tiles */
	public MapTile[][] squares;
	public boolean interactive = true;
	/* TODO(Mariana) What is this? */
	public Integer sync;

	/**
	 * GeoMap is the map being displayed.
	 * 
	 * @param n Number of lines
	 * @param m Number of columns
	 * @param sync
	 */
	public GeoMap(int n, int m, Integer sync) {
		super(n,m);
		setFocusable(true);
		this.sync = sync;
		/* grid initialisation */ 
		while(!ok) {
			initSquares();
		}
	}

	@Override
	public void repairView() {
		xInc = (double) (w) / (stopX - startX);
		yInc = (double) (h) / (stopY - startY);

		/* Update squares showing up on the map */
		for (int i = startY; i < stopY; i++) {
			double y = (i - startY) * yInc;
			for (int j = startX; j < stopX; j++) {
				double x = (j - startX) * xInc;
				squares[i][j].rect = new Rectangle2D.Double(x, y, xInc, yInc);
				squares[i][j].transf.setToTranslation(x, y);
				squares[i][j].transf.scale(xInc / 256.0, yInc / 256.0);
			}
		}
	}

	/**
	 * Returns the tile at at the given row and column.
	 * 
	 * @param col Column coordinates
	 * @param row Row coordinates
	 * @return    The tile at the requested position
	 */
	public MapTile getTile(int col, int row) {
		if(col < 0 || col >= COLS || row < 0 || row >= ROWS) {
			return null;
		}
		return squares[row][col];
	}

	/**
	 * Build and return a list out of the tiles that form the map grid.
	 * 
	 * @return The list of map tiles
	 */
	public List<MapTile> getTileList() {
		List<MapTile> tileList= new ArrayList<MapTile>();
		for (int i = 0; i < ROWS; i++) {
			for (int j = 0; j < COLS; j++) {
				tileList.add(squares[i][j]);
			}
		}
		return tileList;
	}

	/**
	 * Initialise map grid.
	 */
	private void initSquares() {
		w = getWidth();
		h = getHeight();
		xInc = (double) (w) / COLS;
		yInc = (double) (h) / ROWS;
		squares = new MapTile[ROWS][COLS];
		for (int i = 0; i < ROWS; i++) {
			double y = i * yInc;
			for (int j = 0; j < COLS; j++) {
				double x = j * xInc;
				Rectangle2D.Double r = new Rectangle2D.Double(x, y, xInc, yInc);
				squares[i][j] = new MapTile(i, j,  r );
			}
		}
		ok=true;
	}

	/** The current status */
	public boolean isInteractive() {
		return interactive;
	}
} // end GeoMap

