package gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

import main.Main;
import model.GeoCar;
import model.MapPoint;
import model.parameters.Globals;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;

import com.beust.jcommander.Parameter;

public class CarView {

	@Parameter(names = {"--trackerLine"}, description = "Show line tracking car movements on the map.", arity = 1)
	public static boolean trackerLine = false;
    
	private Color carColor;
	private GeoCar car;
	private boolean keepDrawing = true;
	MapMarkerDot lastMk = null;
	private Integer mkLock = 1;
	private MapPoint prevPoint = null;
	private MapPoint currentPoint = null;
	private long id;
	private List<MapPolygonImpl> lines = new ArrayList<MapPolygonImpl>();
	private List<MapPolygonImpl> routeLines = new ArrayList<MapPolygonImpl>();
	JMapViewer map;
	private Integer updateLock = 1;

	public CarView(long id, JMapViewer map, GeoCar car) {
		Globals.parseArgs(this, Main.args );
		this.id = id;
		this.map = map;
		this.carColor = Color.RED;
		this.car = car;
	}

	public long getId() {
		return id;
	}

	public void updateCarView() {
		synchronized (updateLock) {
			if ((car.isBeginNewRoute() || car.getActive() == 0) && CarView.trackerLine)
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						synchronized (routeLines) {
							Iterator<MapPolygonImpl> it = routeLines.iterator();
							while(it.hasNext()) {
								map.removeMapPolygon(it.next());
								it.remove();
							}
						}
					}
				});

			if (car.getActive() == 0 && lastMk != null) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						synchronized (mkLock) {
							if (car.getActive() == 0 && lastMk != null) {
								map.removeMapMarker(lastMk);
								lastMk = null;
							}
						}
					}
				});
				return;
			}
			
			if (car.getCurrentPos() == null)
				return;
			if (car.isBeginNewRoute())
				prevPoint = null;
			else
				prevPoint = currentPoint;
			currentPoint = car.getCurrentPos();
			if (!keepDrawing) {
				return;
			}
			
			if (CarView.trackerLine && prevPoint != null) {
				Coordinate one = new Coordinate(prevPoint.lat, prevPoint.lon);
				Coordinate two = new Coordinate(currentPoint.lat, currentPoint.lon);
				List<Coordinate> route = new ArrayList<Coordinate>(Arrays.asList(one, two, two));
				Color color = Color.RED;
				if ((!prevPoint.occupied) || (!currentPoint.occupied))
					color = Color.GREEN;
				MapPolygonImpl line = new MapPolygonImpl(route);
				line.setColor(color);
				synchronized (lines) {
					lines.add(line);
				}

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						synchronized (lines) {
							if (CarView.trackerLine && prevPoint != null) {
								Iterator<MapPolygonImpl> it = lines.iterator();
								while (it.hasNext()) {
									MapPolygonImpl line = it.next();
									map.addMapPolygon(line);
									synchronized (routeLines) {
										routeLines.add(line);
									}
									it.remove();
								}
							}
						}
					}
				});
			}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					synchronized (mkLock) {
						if (lastMk != null) {
							map.removeMapMarker(lastMk);
						}
						if (currentPoint == null)
							return;
						lastMk = new MapMarkerDot(currentPoint.lat, currentPoint.lon);
						lastMk.setBackColor(carColor);
						lastMk.setColor(carColor);
						map.addMapMarker(lastMk);
					}
				}
			});

		}
	}

	public void disableDrawingCar() {
		keepDrawing = false;
		prevPoint = currentPoint = null;
	}

	public void setColor(Color x) {
		carColor = x;
		System.out.println("=== car " + id + " has color: " + x);
	}
}