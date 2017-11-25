package application;

import java.util.Vector;

import controller.engine.EngineSimulation;
import controller.newengine.SimulationEngine;
import application.routing.RoutingApplicationCar;
import application.routing.RoutingApplicationServer;
import application.streetvisits.StreetVisitsApplication;
import application.tiles.TileApplicationCar;
import application.tiles.TileApplicationServer;
import model.GeoCar;
import model.GeoServer;
import model.parameters.Globals;

public class ApplicationUtils {

	/**
	 * Create the application for current car object
	 * @param type - application type
	 * @param car - Car object
	 * @return
	 */
	public static Application activateApplicationCar( ApplicationType type, GeoCar car)
	{
		switch (type) {
			case ROUTING_APP:
				return SimulationEngine.getInstance() != null ? new RoutingApplicationCar(car) : null;
			case TILES_APP:
				/* this application can be run only using the old simulator */
				return EngineSimulation.getInstance() != null ? new TileApplicationCar(car) : null;
			case STREET_VISITS_APP:
				return SimulationEngine.getInstance() != null ? new StreetVisitsApplication(car) : null;
		default:
			return null;
		}
	}

	/**
	 * Create the application for current Server object
	 * @param type - Application type
	 * @param server - Server object
	 * @return
	 */
	public static Application activateApplicationServer( ApplicationType type, GeoServer server)
	{
		switch (type) {
			case ROUTING_APP:
				return SimulationEngine.getInstance() != null ? new RoutingApplicationServer(server) : null;
			case TILES_APP:
				/* this application can be run only using the old simulator */
				return  EngineSimulation.getInstance() != null ? new TileApplicationServer(server) : null;
		default:
			return null;
		}
	}

	/**
	 * @param netInterfaces - String with active Applications
	 */
	public static void parseApplications(String activeApplications)
	{
		String activeApps[] = activeApplications.split(",");
		Globals.activeApplications = new Vector<ApplicationType>();
		for( String app : activeApps )
		{
			Globals.activeApplications.add( ApplicationType.valueOf(app+"_APP") );
		}
	}
}
