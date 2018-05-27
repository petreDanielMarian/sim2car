package application.tiles;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import controller.engine.EngineInterface;
import controller.engine.EngineSimulation;
import model.Entity;
import model.GeoCar;
import model.network.Message;
import model.parameters.Globals;
import model.tiles.ClientTile;
import model.tiles.GenericTile;
import model.tiles.MapTile;
import application.Application;
import application.ApplicationType;

public class TileApplicationServer extends Application {

	private final Logger logger = Logger.getLogger(TileApplicationServer.class.getName());

	public final double fingerCost = 8.0 / 1024;
	public final double replicaUpdateCost = 8.0 / 1024;
	public final double fingerIDCost = 4.0 / 1024;

	public float wifiTrafficPeersSend = 0;
	public float wifiTrafficPeersRecev = 0;
	int toPeers = 0;
	public boolean isActive = true;

	ApplicationType type = ApplicationType.TILES_APP;

	/** Sorted after their availability, and coordinates. */
	public TreeSet<GenericTile> prioritet;

	/* Number of columns and lines for grid */
	protected static int ROWS = 10;
	protected static int COLS = 10;
	GeoMap map;
	Entity serv;

	public TileApplicationServer(Entity serv) {
		EngineInterface engine = EngineSimulation.getInstance();
		if( engine == null )
		{
			logger.info("Impossible to create the Tile Application for server "+ serv.getId());
			return;
		}
		ROWS = ((EngineSimulation)engine).getMapConfig().getN();
		COLS = ((EngineSimulation)engine).getMapConfig().getM();
		this.serv = serv;
		this.map = EngineSimulation.getInstance().getMap();
		this.id = serv.getId();
		prioritet = new TreeSet<GenericTile>(new Comparator<GenericTile>() {
			@Override
			public int compare(GenericTile o1, GenericTile o2) {
				if (o1.id.equals(o2.id))
					return 0;
				if (o1.replicaCount == o2.replicaCount) {
					if (o1.id.x == o2.id.x)
						return o1.id.y - o2.id.y;
					else
						return o1.id.x - o2.id.x;
				}

				return (int) (o1.replicaCount - o2.replicaCount);
			}
		});
	}

	public GenericTile reqTile(Point iD) {
		MapTile m = map.getTile(iD.x, iD.y);
		m.replicaCount -= 2;
		prioritet.remove(m);
		prioritet.add(m);
		return m;
	}

	public GenericTile getTileFromMap(int x, int y) {
		return map.getTile(x, y);
	}

	/* Upadte tile and a new client will carry it */
	public GenericTile takeTile(Point iD) {
		MapTile m = map.getTile(iD.x, iD.y);

		if (m == null)
			return null;

		m.carrierAccess++;
		m.replicaCount += 1;
		prioritet.remove(m);
		prioritet.add(m);
		return m;
	}

	/* Returns neighbor tiles - one point distance */
	public List<GenericTile> getProximity(Point location) {

		ArrayList<GenericTile> res = new ArrayList<GenericTile>();
		GenericTile t = null;
		int[] proxim = {-1, 0, 1};
		for (int i : proxim) {
			for (int j : proxim) {
				t = map.getTile(location.x + i, location.y + j);
				if (t != null) {
					res.add(t);
				}
			}
		}
		return res;

	}

	/* Updates availability of a tile. */
	public void updateTileAvailability(GenericTile gt, long newValue) {
		gt.replicaCount = newValue;
		prioritet.remove(gt);
		prioritet.add(gt);
	}

	public Object getData() {
		return prioritet;
	}

	@Override
	public boolean getStatus() {
		return isActive;
	}

	@Override
	public String run() {
		return new String();
	}

	public String getInfoApp() {
		return String.format("%8s %15f %15f %15d", "Server" + serv.getId(),
				(wifiTrafficPeersSend), (wifiTrafficPeersRecev), toPeers);
	}

	public void responseServer2Peer(long dest_id, ArrayList<Point> ids) {
		GeoCar part = serv.getPeer(dest_id);
		if (part == null)
			return;

		TileApplicationCar part_app = (TileApplicationCar) part
				.getApplication(type);

		Set<GenericTile> toAdd = new LinkedHashSet<GenericTile>();
		Set<GenericTile> remoteLocalStore = new HashSet<GenericTile>();

		wifiTrafficPeersRecev += ids.size() * fingerIDCost;

		String reqTiles = "Act Client " + part.getId() + " needs: ";
		/* Take the request tiles available from the peer localmemory */
		for (int i = 0; i < ids.size(); i++) {
			Point id = ids.get(i);
			reqTiles += id + " ";
			GenericTile tile = map.getTile(id.x, id.y);
			if (tile != null) {
				toAdd.add(tile);
			}
		}

		remoteLocalStore.addAll(part_app.localStore.values());
		TreeSet<GenericTile> discardable = part_app
				.sortRemovables(remoteLocalStore);

		String givTiles = "Server " + serv.getId() + " provides: ";
		String disTiles = "Act Client " + part.getId() + " "
				+ part.getCurrentPos().tile + " discarded: ";

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
						}
						givTiles += gr.id + " ";
						part_app.wifiTrafficServerRecev += gr.size;
						part_app.fromServers++;
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

			/* Discard unnecessary tiles. */
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

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void process(Message m) {
		Object[] data = (Object[]) m.getData();
		/* If the data is destined to other type of application is dropped */
		if (data[0] != type || m.getDestId() != serv.getId())
			return;

		switch (m.getType()) {
		case REQUEST_ROUTE_TILES_SERVER:
			responseServer2Peer(m.getSourceId(), (ArrayList<Point>) data[1]);
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
