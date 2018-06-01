package test.model;

import gui.CarView;
import gui.ServerView;
import gui.View;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import main.Main;
import model.GeoCar;
import model.GeoServer;
import model.OSMgraph.Way;
import model.parameters.Globals;
import model.parameters.MapConfig;
import model.parameters.MapConfiguration;

import org.openstreetmap.gui.jmapviewer.JMapViewer;

import controller.newengine.EngineUtils;

public class TestGeoCar {
	
	public static void main(String[] args) {
		Main.args = args;
		
		Globals.setUp(args);
		
		TreeMap<Long, Way> graph = EngineUtils.loadGraph(
				"res\\Xml\\streets_rez_san-francisco.osm",
				"res\\Xml\\streets_graph_san-francisco.osm");
		
		MapConfig mapConfig = MapConfiguration.getInstance(Globals.propertiesFile);
		Globals.zoomLevel += mapConfig.getQuot();

		JMapViewer mapJ = new JMapViewer();
		mapJ.setDisplayPositionByLatLon(mapConfig.getMapCentre().getX(),
				mapConfig.getMapCentre().getY(), 11);
		
		final GeoCar car = new GeoCar(0);
		
		final CarView carView = new CarView(car.getId(), mapJ, car);
		Color c = new Color((float) Math.random(),
				(float) Math.random(), (float) Math.random());
		carView.setColor(c);
		List<CarView> carsView = new ArrayList<CarView>();
		carsView.add(carView);
		
		final View view = new View(mapConfig.getN(), mapConfig.getM(), mapJ,
							 new ServerView(
									 mapConfig.getN(), mapConfig.getM(),
									 new ArrayList<GeoServer>(), mapJ),
							carsView);

		final Date now = new Date(Globals.startTime);
		
		Runnable animation = new Runnable() {
			
			@Override
			public void run() {
				long time = now.getTime();
				while (true) {
					time += Globals.timePeriod;

					now.setTime(time);
					view.setTimer(new Date(time * 1000).toString());

					car.prepareMove();
					System.out.println(car.getCurrentPos().lat + " " + car.getCurrentPos().lon);
//					if (car.getNextPos() == null)
//						continue;
					carView.updateCarView();

					try {
						Thread.sleep(1000);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}

				}
			}
		};
		
		view.showView();
		animation.run();

	}

}
