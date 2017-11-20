package model.threadpool.tasks;

import model.GeoServer;

public class ServerApplicationsRun implements Runnable {

	private GeoServer server;
	
	public ServerApplicationsRun(GeoServer server) {
		
		this.server = server;
	}
	
	@Override
	public void run() {
		this.server.runApplications();
	}
}
