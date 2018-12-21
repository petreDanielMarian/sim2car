package controller.network;

import java.util.ArrayList;
import java.util.List;

import model.Entity;
import model.GeoCar;
import model.GeoServer;
import model.GeoTrafficLightMaster;
import model.network.Message;
import utils.tracestool.Utils;

import com.beust.jcommander.Parameter;
import application.routing.RoutingApplicationParameters;

import controller.engine.EngineInterface;
import controller.engine.EngineSimulation;
import controller.newengine.SimulationEngine;

public class NetworkWiFi extends NetworkInterface {
	
    @Parameter(names = {"--maxWifiRange"}, description = "The maximum range of the WiFi interface.")
    public static int maxWifiRange = 10000000;


	public NetworkWiFi(Entity owner) {
		super(NetworkType.Net_WiFi);
		setOwner(owner);
	}

	@Override
	public ArrayList<NetworkInterface> discoversPeers() {
		Entity owner = getOwner();
		ArrayList<NetworkInterface> peersInRange = new ArrayList<NetworkInterface>();
		ArrayList<GeoCar> peers = (ArrayList<GeoCar>) owner.getPeers();
		long dist = 0;

		for (int i = 0; i < peers.size(); i++) {
			GeoCar p = peers.get(i);
			if (p.getCurrentPos() == null /* || p.getNextPos() == null */
					|| owner.getId() == p.getId() || p.getActive() == 0) {
				continue;
			}
			dist = owner.getCurrentPos().distanceTo(p.getCurrentPos());
			if (dist < NetworkWiFi.maxWifiRange) {
				NetworkInterface net = p.getNetworkInterface(this.getType());
				if (net != null)
					peersInRange.add(net);
			}
		}
		return peersInRange;
	}

	@Override
	public ArrayList<NetworkInterface> discoversServers() {
		Entity owner = getOwner();
		ArrayList<NetworkInterface> serversInRange = new ArrayList<NetworkInterface>();
		ArrayList<GeoServer> servers = (ArrayList<GeoServer>) owner.getServers();
		long dist = 0;

		for (int i = 0; i < servers.size(); i++) {
			GeoServer s = servers.get(i);
			dist = owner.getCurrentPos().distanceTo(s.getCurrentPos());
			if (dist < NetworkWiFi.maxWifiRange) {
				NetworkInterface net = s.getNetworkInterface(this.getType());
				if (net != null) {
					serversInRange.add(net);
				}
			}
		}
		return serversInRange;
	}
	
	/* Discover the closest server to the entity */
	@Override
	public NetworkInterface discoverClosestServer() {
		Entity owner = getOwner();
		ArrayList<GeoServer> servers = (ArrayList<GeoServer>) owner.getServers();
		
		GeoServer serverInRange = servers.get(0);
		double maxDist = Utils.distance(owner.getCurrentPos().lat, owner.getCurrentPos().lon,
				serverInRange.getCurrentPos().lat, serverInRange.getCurrentPos().lon);
		
		double dist = 0;

		for (int i = 0; i < servers.size(); i++) {
			GeoServer s = servers.get(i);
			dist = Utils.distance(owner.getCurrentPos().lat, owner.getCurrentPos().lon,
					s.getCurrentPos().lat, s.getCurrentPos().lon);
			if (dist < RoutingApplicationParameters.distMax) {
				if (dist < maxDist) {
					maxDist = dist;
					serverInRange = s;
				}

			}
		}
		
		return serverInRange.getNetworkInterface(this.getType());
	}
	
	public List<NetworkInterface> discoverClosestsTrafficLightMasters() {
		Entity owner = getOwner();
		ArrayList<GeoTrafficLightMaster> mtl = (ArrayList<GeoTrafficLightMaster>) owner.getMasterTrafficLights();
		
		GeoTrafficLightMaster mtlInRange1 = mtl.get(0);
		GeoTrafficLightMaster mtlInRange2 = mtl.get(1);;
		GeoTrafficLightMaster mtlInRange3 = mtl.get(2);;
		
		double maxDist1 = Utils.distance(owner.getCurrentPos().lat, owner.getCurrentPos().lon,
				mtlInRange1.getCurrentPos().lat, mtlInRange1.getCurrentPos().lon);
		
		double maxDist2 = Double.MAX_VALUE;
		double maxDist3 = Double.MAX_VALUE;
		double dist = 0;

		for (int i = 0; i < mtl.size(); i++) {
			GeoTrafficLightMaster s = mtl.get(i);
			dist = Utils.distance(owner.getCurrentPos().lat, owner.getCurrentPos().lon,
					s.getCurrentPos().lat, s.getCurrentPos().lon);
			if (dist < RoutingApplicationParameters.distMax && s.getId() != owner.getId()) {
				if (dist < maxDist1) {
					maxDist3 = maxDist2;
					maxDist2 = maxDist1;
					maxDist1 = dist;
					mtlInRange3 = mtlInRange2;
					mtlInRange2 = mtlInRange1;
					mtlInRange1 = s;
					continue;
				}
				
				if (dist < maxDist2) {
					maxDist3 = maxDist2;
					maxDist2 = dist;
					mtlInRange3 = mtlInRange2;
					mtlInRange2 = s;
					continue;
				}
				
				if (dist < maxDist3) {
					maxDist3 = dist;
					mtlInRange3 = s;
				}

			}
		}
		
		List<NetworkInterface> ret = new ArrayList<NetworkInterface>();
		
		ret.add(mtlInRange3.getNetworkInterface(this.getType()));
		ret.add(mtlInRange2.getNetworkInterface(this.getType()));
		ret.add(mtlInRange1.getNetworkInterface(this.getType()));

		return ret;
	}
	
	
	
	/* Discover the closest 3 traffic light masters */
	public NetworkInterface discoverClosestTrafficLightMaster() {
		Entity owner = getOwner();
		ArrayList<GeoTrafficLightMaster> mtl = (ArrayList<GeoTrafficLightMaster>) owner.getMasterTrafficLights();
		
		GeoTrafficLightMaster mtlInRange = mtl.get(0);
		
		double maxDist = Utils.distance(owner.getCurrentPos().lat, owner.getCurrentPos().lon,
				mtlInRange.getCurrentPos().lat, mtlInRange.getCurrentPos().lon);

		double dist = 0;

		for (int i = 0; i < mtl.size(); i++) {
			GeoTrafficLightMaster s = mtl.get(i);
			dist = Utils.distance(owner.getCurrentPos().lat, owner.getCurrentPos().lon,
					s.getCurrentPos().lat, s.getCurrentPos().lon);
			if (dist < RoutingApplicationParameters.distMax && s.getId() != owner.getId()) {
				
				if (dist < maxDist) {
					maxDist = dist;
					mtlInRange = s;
				}

			}
		}

		return mtlInRange.getNetworkInterface(this.getType());
	}
	
	public NetworkInterface discoverTrafficLight(GeoTrafficLightMaster trafficLightMaster) {
		Entity owner = getOwner();
		long dist = 0;

		dist = owner.getCurrentPos().distanceTo(trafficLightMaster.getCurrentPos());
		if (dist < NetworkWiFi.maxWifiRange) {
			NetworkInterface net = trafficLightMaster.getNetworkInterface(this.getType());
			if (net != null) {
				return net;
			}
		}
		return null;
	}
	
	 public Message getNextInputMessage() {
		return this.getInputQueue().remove(0);
	}
	 
	 public void processOutputQueue() {
			
			while(!this.getOutputQueue().isEmpty()) {
				
				Message msg = this.getOutputQueue().remove(0);
				/* maybe another thread is processing this output queue too */
				if (msg == null)
					break;
				
				/* get destination Entity */
				EngineInterface engine = SimulationEngine.getInstance();
				/* Maintain backward compatibility with old simulator */
				Entity destEntity = engine != null ? ((SimulationEngine)engine).getEntityById(msg.getDestId()): 
									EngineSimulation.getInstance().getCarById((int)msg.getDestId());
				/* get destination entity WIFI network */
				NetworkInterface destNetIface = destEntity.getNetworkInterface(NetworkType.Net_WiFi);
				/* send message to entity */
				this.getOwner().getNetworkInterface(NetworkType.Net_WiFi).send(msg, destNetIface);
			}
		}
}