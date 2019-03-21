package application.multipleIntersections;

import java.io.Serializable;

import model.MapPoint;
import model.OSMgraph.Node;

public class SynchronizeIntersectionsData implements Serializable{
	private int queueSize = 0;
	private Long wayId;
	private int direction;
	private Node fromNode;
	private MapPoint mapPoint;
	
	
	
	public MapPoint getMapPoint() {
		return mapPoint;
	}

	public void setMapPoint(MapPoint mapPoint) {
		this.mapPoint = mapPoint;
	}

	public SynchronizeIntersectionsData(MapPoint mapPoint, int queueSize, Long wayId, int direction, Node fromNode) {
		super();
		this.queueSize = queueSize;
		this.wayId = wayId;
		this.direction = direction;
		this.fromNode = fromNode;
		this.mapPoint = mapPoint;
	}
	
	public Node getFromNode() {
		return fromNode;
	}

	public void setFromNode(Node fromNode) {
		this.fromNode = fromNode;
	}

	public int getQueueSize() {
		return queueSize;
	}
	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}
	public Long getWayId() {
		return wayId;
	}
	public void setWayId(Long wayId) {
		this.wayId = wayId;
	}
	public int getDirection() {
		return direction;
	}
	public void setDirection(int direction) {
		this.direction = direction;
	}	
	

}
