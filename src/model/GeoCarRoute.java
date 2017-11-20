package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import model.OSMgraph.Node;

public class GeoCarRoute implements Serializable{

	private static final long serialVersionUID = 8798228521356413494L;
	private MapPoint startPoint;
	private MapPoint endPoint;
	private List<Node> intersectionList;
	private List<Node> originalintersectionList;
	
	public GeoCarRoute(MapPoint start, MapPoint end, List<Node> intersections) {
		startPoint = start;
		endPoint = end;
		intersectionList = intersections;
		originalintersectionList = new ArrayList<Node>(intersections);
	}
	
	public MapPoint getStartPoint() {
		return startPoint;
	}
	public void setStartPoint(MapPoint startPoint) {
		this.startPoint = startPoint;
	}
	public MapPoint getEndPoint() {
		return endPoint;
	}
	public void setEndPoint(MapPoint endPoint) {
		this.endPoint = endPoint;
	}
	public List<Node> getIntersectionList() {
		return intersectionList;
	}
	public void setIntersectionList(List<Node> intersectionList) {
		this.intersectionList = intersectionList;
	}

	/* returns the intersections list without being populated with
	 * other intermediate nodes.
	 */
	public List<Node> getOriginalIntersectionList() {
		return originalintersectionList;
	}
	public void reinitIntersectionLists( List<Node> intersectionsList )
	{
		this.originalintersectionList = new ArrayList<Node>(intersectionsList);
		this.intersectionList = intersectionsList;
	}
	
	@Override
	public String toString() {
		String result = "";
		result += "start " + startPoint.lat + " " + startPoint.lon + " " + startPoint.occupied + "\n";
		
		for (Node node : intersectionList) {
			result += node.id + " " + node.lat + " " + node.lon + "\n";
		}
		
		result += "end " + endPoint.lat + " " + endPoint.lon + " " + endPoint.occupied + "\n";
		
		return result;
	}
}
