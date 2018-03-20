package gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import model.GeoServer;
import model.MapPoint;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerCircle;

public class ServerView {
	ArrayList<GeoServer> servers;
	JMapViewer map;

	public ServerView(int n, int m, List<GeoServer> s, JMapViewer map) {
		this.servers = (ArrayList<GeoServer>) s;
		this.map = map;
		activateLocationServer();
	}

	public void activateLocationServer() {
		synchronized (servers) {
			for (GeoServer sv : servers) {
				MapPoint pt = sv.getCurrentPos();
				MapMarkerCircle mk = new MapMarkerCircle(pt.lat, pt.lon, 0.05);
				mk.setColor(Color.RED);
				map.addMapMarker(mk);
			}
		}
	}

	public void initLocationServer(ArrayList<GeoServer> sers) {
		synchronized (servers) {
			servers = new ArrayList<GeoServer>(sers);
			for (GeoServer sv : servers) {
				MapPoint pt = sv.getCurrentPos();
				MapMarkerCircle mk = new MapMarkerCircle(pt.lat, pt.lon, 0.05);
				mk.setColor(Color.RED);
				map.addMapMarker(mk);
			}
		}
	}

	public void addnewServerView(GeoServer sv) {
		synchronized (servers) {
			servers.add(sv);
		}
	}
}
