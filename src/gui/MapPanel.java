package gui;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;

import javax.swing.JPanel;

import controller.newengine.SimulationEngine;

/**
 * The panel that displays the map.
 * Operations implemented:
 *   - zoom in/out
 *   - move
 */
public class MapPanel extends JPanel{
	private static final long serialVersionUID = 1L;
	
	/* How much can the map move on X and Y, between minimum and maximum limits. */
	protected int startX;
	protected int stopX;
	protected int startY;
	protected int stopY;

	/* Number of columns and lines for the grid. */
	protected static int ROWS = 10;
	protected static int COLS = 10;

	/* Map dimensions */ 
	public int w;
	public int h;

	/* Steps for applying a graphical transformation in increments. */   
	public double xInc, yInc;

	/* Graphical transforms in 2D: translation, scale, flip, rotations, and shears. */
	public AffineTransform trans;

	public MapPanel(int n, int m) {
		ROWS = n;
		COLS = m;

		/* Get panel dimension */
		w = getWidth();
		h = getHeight();
		
		startX = (int)(2 * Math.pow(2, SimulationEngine.getInstance().getMapConfig().getQuot()));
		startY = (int)(10 * Math.pow(2, SimulationEngine.getInstance().getMapConfig().getQuot()));
		stopX = (int)(12 * Math.pow(2, SimulationEngine.getInstance().getMapConfig().getQuot()));
		stopY = (int)(22 * Math.pow(2, SimulationEngine.getInstance().getMapConfig().getQuot()));

		/*
		 * xInc - step on x -- width
		 * yInc - step on y -- height
		 */
		xInc = (double) (w) / (stopY - startY);
		yInc = (double) (h) / (stopX - startX);

		trans = new AffineTransform();
		trans.setToScale(xInc / 256.0, yInc / 256.0);
	}

	/**
	 * Recompute incrementing steps and rescale.
	 */
	public void repairView() {
		xInc = (double) (w) / (stopX - startX);
		yInc = (double) (h) / (stopY - startY);
		trans.setToScale(xInc / 256.0, yInc / 256.0);
	}

	/**
	 * Move panel up. Stop at the first row in the grid.
	 */
	public boolean moveUp() {
		if (startY > 0) {
			startY--;
			stopY--;
			repairView();
			repaint();
			return true;
		}
		return false;
	}
	
	/**
	 * Move panel down. Stop at the last row in grid.
	 */
	public boolean moveDown() {
		if (stopY < ROWS) {
			startY++;
			stopY++;
			repairView();
			repaint();
			return true;
		}
		return false;
	}
	
	/**
	 * Move panel towards the left.
	 */
	public boolean moveLeft() {
		if (startX > 0) {
			startX--;
			stopX--;
			repairView();
			repaint();
			return true;
		}
		return false;
	}

	/**
	 * Move panel towards the right.
	 */
	public boolean moveRight() {
		if (stopY < COLS) {
			startX++;
			stopX++;
			repairView();
			repaint();
			return true;
		}
		return false;
	}
	
	/**
	 * Zoom into the current view.
	 */
	public boolean zoomIn() {
		boolean changed = false;
		/* Zoom on x. */
		if (stopX - startX - 2 > 0) {
			stopX--;
			startX++;
			changed = true;
		}
		/* Zoom on y. */    
		if (stopY - startY - 2 > 0) {
			stopY--;
			startY++;
			changed = true;
		}
		if (changed) {
			repairView();
			repaint();
		}
		return changed;
	}

	/**
	 * Zoom out of the current view.
	 */
	public boolean zoomOut() {
		boolean changed = false;
		/* Zoom on x. */
		if (stopX < COLS) {
			stopX++;
			changed = true;
		}
		/* Zoom on y. */
		if (stopY < ROWS) {
			stopY++;
			changed = true;
		}
		if (startX > 0) {
			startX--;
			changed = true;
		}
		if (startY > 0) {
			startY--;
			changed = true;
		}
		if(changed) {
			repairView();
			repaint();
		} 
		return changed;
	}

	/** 
	 * Listener for resize
	 */
	public ComponentListener cl = new ComponentAdapter() {

		@Override
		public void componentResized(ComponentEvent e) {

			w = getWidth();
			h = getHeight();
			repairView();
			repaint();
		}
	};

}
