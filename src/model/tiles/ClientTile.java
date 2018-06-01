package model.tiles;

import java.awt.Point;
import java.util.Date;

public class ClientTile extends GenericTile {

	public int distToTile;
	public Date acquisition;
	public Date lastDistribution = null;
	/* Order in which tile is traversed by a client. */
	public int order;

	public ClientTile(GenericTile gt, Date acq, Point currentPos) {
		this.replicaCount = gt.replicaCount;
		this.size = gt.size;
		this.acquisition = new Date(acq.getTime());
		this.order = 0;
		id = new Point(gt.id.x, gt.id.y);
		distToTile = Math.abs(currentPos.x - id.x) + Math.abs(currentPos.y - id.y);
	}

	public ClientTile(GenericTile gt, int order, Point currentPos) {
		this.replicaCount = gt.replicaCount;
		this.acquisition = null;
		this.order = order;
		this.size = gt.size;
		id = new Point(gt.id.x, gt.id.y);
		distToTile = Math.abs(currentPos.x - id.x)
				+ Math.abs(currentPos.y - id.y);
	}

	public void updateDistance(Point currentPos) {
		distToTile = Math.abs(currentPos.x - id.x)
				+ Math.abs(currentPos.y - id.y);
	}
}