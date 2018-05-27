package application.tiles;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import model.GeoCar;
import model.GeoServer;
import model.MapPoint;
import model.network.Message;
import model.network.MessageType;
import model.parameters.Globals;
import model.tiles.ClientTile;
import model.tiles.GenericTile;
import utils.Range;
import application.Application;
import application.ApplicationType;

import com.beust.jcommander.Parameter;

import controller.network.NetworkInterface;
import controller.network.NetworkType;

public class TileApplicationCar extends Application {


	// TODO: Add description
	@Parameter(names = {"--peerProxToServer"}, arity = 1)
	public static boolean peerProxToServer = false;

	// TODO: Add description
	@Parameter(names = {"--peersCommunication"}, arity = 1)
	public static boolean peersComm = true;

	@Parameter(names = {"--wifiOn"}, description = "WiFi communication state on/off", arity = 1)
	public static boolean wifiOn = true;

	@Parameter(names = {"--memorySize"}, description = "Car memory size")
	public static int memorySize = 5120;

	/* The cost for the search over a network */
	public final double searchCost = 50;
	public final double routeReqCost = 16.0 / 1024;
	/* The cost for the transfer of a tile over a network */
	public final double fingerCost = 8.0 / 1024;
	public final double fingerIDCost = 4.0 / 1024;
	/* This field shows the previous state of a taxi */
	public boolean wasOccupied = false;

	/* The type of application */
	private ApplicationType type = ApplicationType.TILES_APP;

	/* Reference to the car object */
	GeoCar car;

	/* The list of IDs (pairs of x, y) for the require tiles */
	public List<Point> requiredTilesIds;

	public HashMap<Point, ClientTile> localStore;

	/* The cost of the traffic on a type of network */
	public double networkTraffic = 0;

	public double wifiTrafficPeersSend = 0;
	public double wifiTrafficPeersRecev = 0;
	public double wifiTrafficServerSend = 0;
	public double wifiTrafficServerRecev = 0;

	/* All memory of the entity */
	public double memory;
	/* The memory used by tiles */
	public double usedMemory;

	/* Direct request to the server for the tiles which aren't in local memory. */
	public int directRequests = 0;

	/* The number of the tiles which are found in the memory. */
	public int cacheHits = 0;

	/* Number of tiles receive from peers */
	public int fromPeers = 0;

	/* Number of tiles send to peers */
	public int toPeers = 0;

	/* Tiles obtained from servers */
	public int fromServers = 0;

	public boolean isActive = false;

	public TileApplicationCar(GeoCar car) {
		this.car = car;
		this.id = car.getId();

		/* the memory available to the car */
		memory = memorySize;
		usedMemory = 0;
		requiredTilesIds = new ArrayList<Point>();

		localStore = new HashMap<Point, ClientTile>();
	}

	public TileApplicationCar(Range memoryRange, GeoCar car) {
		this(car);
		memory = memoryRange.getValue();
	}

	/*
	 * This function analyzes the current situation of car and if it has a
	 * certain destination it requires the ids of tiles to the destination.
	 */
	public void update() {

		MapPoint currentPos = car.getCurrentPos();

		/*
		 * If the car at the anterior positon hasn't a route established and now
		 * it has should get the id-s of the tiles from the local applications
		 * memory.
		 */
		if (!wasOccupied && currentPos.occupied) {
			// System.out.println("Detecting route  " + car.getId());

			//			ArrayList<MapPoint> pointsInAdvance = car.getPointsInAdvance();
			ArrayList<MapPoint> pointsInAdvance = new ArrayList<MapPoint>();
			isActive = true;
			Point lastTileid = currentPos.tile;
			requiredTilesIds.add(currentPos.tile);
			// System.out.println( " POints size "+ pointsInAdvance.size() );
			for (MapPoint pt : pointsInAdvance) {
				if (pt.occupied) {
					if (pt.tile == null)
						continue;
					if (lastTileid == null || (lastTileid != null &&
							(pt.tile.x != lastTileid.x || pt.tile.y != lastTileid.y))) {
						int j;
						for (j = 0; j < requiredTilesIds.size(); j++) {
							Point auxId = requiredTilesIds.get(j);
							if (pt.tile.x == auxId.x && pt.tile.y == auxId.y)
								break;
						}
						/* If the tile ID is on the require list it isn't added on the list. */
						if (j == requiredTilesIds.size()) {
							requiredTilesIds.add(pt.tile);
						}
						lastTileid = pt.tile;
					}
				} else
					break;
			}
		}
		if (wasOccupied && !currentPos.occupied) {
			isActive = false;
			requiredTilesIds.clear();
		}
		wasOccupied = currentPos.occupied;
	}

	public void checkStatus(NetworkInterface net_s) {
		final MapPoint currentPos = car.getCurrentPos();
		TileApplicationServer servapp = (TileApplicationServer)
				net_s.getOwner().getApplication(type);

		if (currentPos == null)
			return;

		if (!localStore.containsKey(currentPos.tile)) {
			GenericTile gt = servapp.takeTile(currentPos.tile);
			if (gt == null)
				return;
			freeUpSomeSpace(gt.size, car);

			ClientTile ct = new ClientTile(gt, new Date(), currentPos.tile);
			if (localStore.put(currentPos.tile, ct) != null)
				System.out.println("KKT00");

			usedMemory += gt.size;
			directRequests++;
		} else
			cacheHits++;
	}

	/** Free space when needed */
	protected void freeUpSomeSpace(double size, GeoCar mob) {
		final MapPoint currentPos = mob.getCurrentPos();
		if (memory - usedMemory > size)
			return;

		Set<ClientTile> removable = new HashSet<ClientTile>();
		Comparator<GenericTile> compy = new Comparator<GenericTile>() {
			@Override
			public int compare(GenericTile o1, GenericTile o2) {
				if (o1.id.equals(o2.id))
					return 0;
				if (o1.replicaCount == o2.replicaCount) {
					double dto1 = currentPos.tile.distance(o1.id);
					double dto2 = currentPos.tile.distance(o2.id);
					return (int) (dto1 - dto2);
				}
				return (int) (o1.replicaCount - o2.replicaCount);
			}
		};

		TreeSet<GenericTile> prioriti = new TreeSet<GenericTile>(compy);
		prioriti.addAll(removable);
		/* Remove all tiles from the memory when they're no longer used */
		while (memory - usedMemory < size && !prioriti.isEmpty()) {
			GenericTile gq = prioriti.pollLast();
			if (localStore.remove(gq.id) != null)
				usedMemory -= gq.size;
		}
		/*
		 * If after deleting tiles we no longer need, we still don't have
		 * sufficient memory - we delete data from localstore.
		 */
		if (memory - usedMemory < size) {
			Comparator<ClientTile> compey = new Comparator<ClientTile>() {
				@Override
				public int compare(ClientTile o1, ClientTile o2) {
					if (o1.id.equals(o2.id))
						return 0;
					if (o1.order == o2.order) {
						double dto1 = currentPos.tile.distance(o1.id);
						double dto2 = currentPos.tile.distance(o2.id);
						return (int) (dto1 - dto2);
					}
					return (int) (o1.order - o2.order);
				}
			};

			TreeSet<ClientTile> prioritei = new TreeSet<ClientTile>(compey);
			prioritei.addAll(localStore.values());
			while (memory - usedMemory < size && !prioritei.isEmpty()) {
				GenericTile gq = prioritei.pollLast();
				if (localStore.remove(gq.id) != null)
					usedMemory -= gq.size;
				if (Globals.debug == 1)
					System.out.println("Client " + id + "forced removed " + gq.id);
			}
		}
		if (memory - usedMemory < size) {
			System.err.println("This should not happen.");
		}
	}

	public TreeSet<GenericTile> sortRemovables(Set<GenericTile> removable) {
		final MapPoint currentPos = car.getCurrentPos();
		Comparator<GenericTile> compy = new Comparator<GenericTile>() {

			@Override
			public int compare(GenericTile o1, GenericTile o2) {
				double dto1 = currentPos.tile.distance(o1.id);
				double dto2 = currentPos.tile.distance(o2.id);
				if (dto1 == dto2) {
					if (o1.id.x == o2.id.x) {
						return o1.id.y - o2.id.y;
					} else {
						return o1.id.x - o2.id.x;
					}
				}
				return (int) (dto1 - dto2);
			}
		};
		TreeSet<GenericTile> prioriti = new TreeSet<GenericTile>(compy);
		prioriti.addAll(removable);
		return prioriti;
	}

	public String run() {
		if (car.getCurrentPos() == null)
			return "";
		String logs = "";
		/* Analysing the current situation of the car */
		update();
		/* Now discovering all peers in his Range */
		if (peersComm && wifiOn) {
			ArrayList<NetworkInterface> peers = car.getNetworkInterface(
					NetworkType.Net_WiFi).discoversPeers();
			for (NetworkInterface net_p : peers) {
				/*
				 * If the current car has a route established it demands from
				 * the other peers the tile for the route if it has some
				 * undiscovered yet.
				 */
				logs += requestPeer2PeerCommunication(net_p);
			}
		}
		ArrayList<NetworkInterface> servers = car.getNetworkInterface(
				NetworkType.Net_WiFi).discoversServers();
		for (NetworkInterface net_s : servers) {
			logs += requestPeer2ServerCommunication(net_s);
		}
		return logs;
	}

	public ArrayList<Point> getUnavailableTile() {
		ArrayList<Point> neededT = new ArrayList<Point>();
		for (int i = 0; i < requiredTilesIds.size(); i++) {
			Point id = requiredTilesIds.get(i);
			/* TODO(Mariana) Does this work? */
			if (!localStore.containsKey(id)) {
				neededT.add(id);
			}
		}
		return neededT;
	}

	/**
	 * TODO(Mariana) Needs a description.
	 * 
	 * @param net_partner  The interface of the peer with whom
	 *                     the current car will communicate.
	 */
	public String requestPeer2PeerCommunication(NetworkInterface net_partner) {
		String log = "";
		NetworkInterface net_c = car.getNetworkInterface(NetworkType.Net_WiFi);

		Object[] data = new Object[2];
		data[0] = type;

		Message m;
		ArrayList<Point> reqTilesids = getUnavailableTile();
		/*
		 * This is an active peer which has an route established request the
		 * needed tiles
		 */
		if (reqTilesids.size() != 0) {
			wifiTrafficPeersSend += reqTilesids.size() * fingerIDCost;
			data[1] = reqTilesids;
			m = new Message(car.getId(), net_partner.getOwner().getId(), data,
					MessageType.REQUEST_ROUTE_TILES_PEER, ApplicationType.TILES_APP);

			net_c.send(m, net_partner);
			GeoCar nextp = (GeoCar) net_partner.getOwner();
			log += "P" + car.getId() + "-P" + nextp.getId() + "("
					+ car.getCurrentPos().distanceTo(nextp.getCurrentPos())
					+ "m)\n";
		}
		/*
		 * If the peer it ask the tiles from his proximity sending his actual
		 * position.
		 */
		if (!isActive) {
			wifiTrafficPeersSend += fingerIDCost;
			data[1] = car.getCurrentPos().tile;
			m = new Message(car.getId(), net_partner.getOwner().getId(), data,
					MessageType.REQUEST_CRTPOS_PROXIMITY_PEER, ApplicationType.TILES_APP);

			net_c.send(m, net_partner);
			GeoCar nextp = (GeoCar) net_partner.getOwner();
			log += "P" + car.getId() + "-P" + nextp.getId() + "("
					+ car.getCurrentPos().distanceTo(nextp.getCurrentPos())
					+ "m)\n";
		}
		return log;
	}

	/**
	 * TODO(Mariana) Needs a description.
	 * 
	 * @param net_server  The interface of the server with whom
	 *                    the current car will communicate.
	 */
	public String requestPeer2ServerCommunication(NetworkInterface net_server) {
		String log = "";
		NetworkInterface net_c = car.getNetworkInterface(NetworkType.Net_WiFi);
		Object[] data = new Object[2];
		data[0] = type;
		Message m;
		ArrayList<Point> reqTilesids = getUnavailableTile();

		/*
		 * This is an active peer which has a route established.
		 * Request the needed tiles
		 */
		if (reqTilesids.size() != 0) {
			wifiTrafficServerSend += reqTilesids.size() * fingerIDCost;
			data[1] = reqTilesids;
			m = new Message(car.getId(), net_server.getOwner().getId(), data,
					MessageType.REQUEST_ROUTE_TILES_SERVER, ApplicationType.TILES_APP);

			net_c.send(m, net_server);
			GeoServer nexts = (GeoServer) net_server.getOwner();
			log += "P" + car.getId() + "-S" + nexts.getId() + "("
					+ car.getCurrentPos().distanceTo(nexts.getCurrentPos())
					+ "m)\n";
		} else {
			if (peerProxToServer) {
				ArrayList<Point> neigh = getProximity(car.getCurrentPos().tile);
				neigh.add(car.getCurrentPos().tile);

				data[1] = neigh;

				m = new Message(car.getId(), net_server.getOwner().getId(),
						data, MessageType.REQUEST_ROUTE_TILES_SERVER, ApplicationType.TILES_APP);

				net_c.send(m, net_server);
				GeoServer nexts = (GeoServer) net_server.getOwner();
				log += "P" + car.getId() + "-S" + nexts.getId() + "("
						+ car.getCurrentPos().distanceTo(nexts.getCurrentPos())
						+ "m)\n";
			}
		}
		return log;
	}

	/**
	 * TODO(Mariana) Needs a description.
	 * 
	 * @param dest_id Peer car
	 * @param ids     Needed tiles
	 */
	public void responsePeer2PeerTiles(long dest_id, ArrayList<Point> ids) {

		GeoCar part = car.getPeer(dest_id);
		if (part == null)
			return;

		TileApplicationCar part_app = (TileApplicationCar) part
				.getApplication(type);

		Set<GenericTile> toAdd = new LinkedHashSet<GenericTile>();

		Set<GenericTile> remoteLocalStore = new HashSet<GenericTile>();
		remoteLocalStore.addAll(part_app.localStore.values());

		String reqtiles = "Act Client " + part.getId() + " needs: ";
		/* Take the request tiles available from the peer localmemory */
		for (int i = 0; i < ids.size(); i++) {
			Point id = ids.get(i);
			reqtiles += id + " ";
			GenericTile tile = localStore.get(id);
			if (tile != null) {
				toAdd.add(tile);
			}
		}

		String givtiles = "Act Client " + car.getId() + " provides: ";
		String distiles = "Act Client " + part.getId() + " "
				+ part.getCurrentPos().tile + " discarded: ";

		TreeSet<GenericTile> discardable = sortRemovables(remoteLocalStore);

		while (!(toAdd.isEmpty())) {
			for (Iterator<GenericTile> it = toAdd.iterator(); it.hasNext();) {
				GenericTile gr = it.next();
				if (part_app.localStore.containsKey(gr.id)) {
					it.remove();
				} else {
					if (part_app.usedMemory + gr.size > part_app.memory) {
						break;
					} else {
						if (part_app.localStore.put(gr.id, new ClientTile(gr,
								new Date(), part.getCurrentPos().tile)) != null) {
							System.out.println("KKT11");
						}
						givtiles += gr.id + " ";

						part_app.wifiTrafficPeersRecev += gr.size;
						part_app.fromPeers++;
						toPeers++;
						wifiTrafficPeersSend += gr.size;
						part_app.usedMemory += gr.size;
						it.remove();
					}
				}
			}

			if (toAdd.isEmpty() || discardable.isEmpty()) {
				break;
			}

			// Removes tiles we no longer need
			while (!discardable.isEmpty()) {
				GenericTile gt = discardable.pollLast();
				if (!part_app.requiredTilesIds.contains(gt.id)) {
					if (part_app.localStore.remove(gt.id) != null) {
						part_app.usedMemory -= gt.size;
						distiles += gt.id + " ";
						break;
					}
				}
			}
		}
		if (Globals.debug == 1) {
			System.out.println(reqtiles);
			System.out.println(givtiles);
			System.out.println(distiles);
		}
	}

	/**
	 * Returns the IDs for the possible proximity tiles.
	 * 
	 * @param location The point for which we want tiles in its proximity
	 * @return         The list of surrounding points
	 */
	public ArrayList<Point> getProximity(Point location) {
		ArrayList<Point> res = new ArrayList<Point>();
		int[] proxim = {-1, 0, 1};
		for (int i : proxim) {
			for (int j : proxim) {
				Point t = new Point(location.x + i, location.y + j);
				res.add(t);
			}
		}
		return res;
	}

	/**
	 * TODO(Mariana) Needs a description.
	 * 
	 * @param dest_id
	 * @param crtPos
	 */
	public void responsePeer2PeerProximity(long dest_id, Point crtPos) {
		GeoCar part = car.getPeer(dest_id);
		if (part == null)
			return;

		TileApplicationCar part_app = (TileApplicationCar) part
				.getApplication(type);
		Set<GenericTile> toAdd = new LinkedHashSet<GenericTile>();
		Set<GenericTile> remoteLocalStore = new HashSet<GenericTile>();
		remoteLocalStore.addAll(part_app.localStore.values());
		ArrayList<Point> proximities = getProximity(crtPos);
		String reqTiles = "Act Client " + part.getId() + " needs: ";

		/* Take the request tiles available from the peer localmemory */
		for (int i = 0; i < proximities.size(); i++) {
			Point id = proximities.get(i);
			reqTiles += id + " ";
			GenericTile tile = localStore.get(id);
			if (tile != null) {
				toAdd.add(tile);
			}
		}

		String givTiles = "Act Client " + car.getId() + " provides: ";
		String disTiles = "Act Client " + part.getId() + " "
				+ car.getCurrentPos().tile + " discarded: ";

		TreeSet<GenericTile> discardable = sortRemovables(remoteLocalStore);

		while (!(toAdd.isEmpty())) {
			for (Iterator<GenericTile> it = toAdd.iterator(); it.hasNext();) {
				GenericTile gr = it.next();

				if (part_app.localStore.containsKey(gr.id)) {
					it.remove();
				} else {
					if (part_app.usedMemory + gr.size > part_app.memory) {
						break;
					} else {
						if (part_app.localStore.put(gr.id, new ClientTile(gr,
								new Date(), part.getCurrentPos().tile)) != null) {
							System.out.println("KKT11");
						}
						givTiles += gr.id + " ";

						part_app.wifiTrafficPeersRecev += gr.size;
						part_app.fromPeers++;
						toPeers++;
						wifiTrafficPeersSend += gr.size;
						part_app.usedMemory += gr.size;
						it.remove();
					}
				}
			}

			if (toAdd.isEmpty() || discardable.isEmpty()) {
				break;
			}

			// Remove tiles no longer needed
			while (!discardable.isEmpty()) {
				GenericTile gt = discardable.pollLast();
				if (!part_app.requiredTilesIds.contains(gt.id))
					if (part_app.localStore.remove(gt.id) != null) {
						part_app.usedMemory -= gt.size;
						disTiles += gt.id + " ";
						break;
					}
			}
		}
		if (Globals.debug == 1) {
			System.out.println(reqTiles);
			System.out.println(givTiles);
			System.out.println(disTiles);
		}
	}

	@Override
	public boolean getStatus() {
		// TODO Auto-generated method stub
		return isActive;
	}

	public String getInfoApp() {
		return String.format(
				"%5s %15f %15f %15f %15f %15d %15d %15d %15d %20f %20f", "Taxi"
						+ car.getId(), (wifiTrafficPeersSend),
						(wifiTrafficServerSend), (wifiTrafficServerSend),
						(wifiTrafficServerRecev), localStore.size(), toPeers,
						fromPeers, fromServers, memory, usedMemory);
	}

	public Object getData() {
		return localStore;
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void process(Message m) {
		Object[] data = (Object[]) m.getData();
		/* If the data is destined to other type of application is dropped */
		if (data[0] != type || m.getDestId() != car.getId())
			return;

		switch (m.getType()) {
		case REQUEST_ROUTE_TILES_PEER:
			responsePeer2PeerTiles(m.getSourceId(), (ArrayList<Point>) data[1]);
			break;
		case REQUEST_CRTPOS_PROXIMITY_PEER:
			responsePeer2PeerProximity(m.getSourceId(), (Point) data[1]);
			break;
		default:
			break;
		}
	}

	public ApplicationType getType() {
		return type;
	}

	@Override
	public String stop() {
		return null;
	}
}