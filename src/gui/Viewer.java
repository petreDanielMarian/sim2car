package gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import model.GeoCar;
import model.parameters.Globals;
import model.parameters.MapConfig;

import org.openstreetmap.gui.jmapviewer.JMapViewer;

/**
 * Class to deal with visualising the simulation.
 * TODO: Move all GUI stuff from the EngineSimulation in here.
 *
 */
public class Viewer {
	private View view;
	
	private JMapViewer mapJ;
	
	private List<CarView> carViewList = new ArrayList<CarView>();
	
	public Viewer(final MapConfig mapConfig) {
		if (Globals.showGUI) {
			mapJ = new JMapViewer();
			mapJ.setDisplayPositionByLatLon(mapConfig.getMapCentre().getY(), mapConfig.getMapCentre().getX(), 11);
			view = new View(mapConfig.getN(), mapConfig.getM(), mapJ, null, carViewList);
		}
	}

	public void addCar(GeoCar car) {
		if (Globals.showGUI) {
			CarView carView = new CarView(car.getId(), mapJ, car);
			carView.setColor(new Color( (float) Math.random(),
										(float) Math.random(),
										(float) Math.random()) );
			carViewList.add(carView);
		}
	}

	public void showView() {
		if (Globals.showGUI) {
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					view.showView();
				}
			});
		}
	}
	
	public void setTime(final String time) {
		if (Globals.showGUI) {
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					view.setTimer(time);
				}
			});
		}
	}

	public void updateCarPositions() {
		if (Globals.showGUI) {
			for (CarView car : carViewList) {
				car.updateCarView();
			}
			view.repaint();
		}
	}
}
