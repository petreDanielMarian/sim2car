package controller.engine;

import gui.View;

import java.util.List;

import model.GeoCar;
import model.GeoServer;

public interface EngineInterface {

	public void setUp();
	public void start();
	public void stopSimulation();
	public View getView();
	public List<GeoCar> getPeers();
	public List<GeoServer> getServers();
}
