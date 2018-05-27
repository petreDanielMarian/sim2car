package controller.engine;

import gui.View;

import java.util.List;

import model.GeoCar;
import model.GeoServer;

/** 
 * The interface used for implementing the simulators core engine
 * @author Alex
 *
 */
public interface EngineInterface {
	public void setUp();
	public void start();
	public void stopSimulation();
	public View getView();
	public List<GeoCar> getPeers();
	public List<GeoServer> getServers();
}
