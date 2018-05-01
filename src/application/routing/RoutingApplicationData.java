package application.routing;

import java.io.Serializable;
import java.util.TreeMap;

import model.GeoCarRoute;
import model.MapPoint;
import utils.Pair;

public class RoutingApplicationData implements Serializable {
	
	/* Routing applications states */
	public enum RoutingApplicationState {
		COST_COLLECTING,
		RUN_USING_ONLY_DISTANCE,
		RUN_USING_ONLY_CONGESTION,
		RUN_USING_ONLY_PAGERANK,
		RUN_USING_ONLY_BC,
		RUN_USING_CONG_PR,
		RUN_USING_CONG_BC,
		RUN_USING_CONG_PR_BC,
		UNKNOWN
	};
	private static final long serialVersionUID = 1L;
	public String msg;
	public double congestion;
	public long prevStreet, nextStreet, jointId;
	public long timestamp;
	public double avgspeed = 0;
	public MapPoint startRoutePoint, endRoutePoint;
	GeoCarRoute route = null;
	private Pair<Long,Long> p = null;
	private RoutingRoadCost c = null;

	
	public RoutingApplicationData(String msg, double congestion, long prevStreet, long nextStreet, long jointId, long timestamp) {
		
		this.msg = msg;
		this.congestion = congestion;
		this.prevStreet = prevStreet;
		this.nextStreet = nextStreet;
		this.jointId = jointId;
		this.timestamp = timestamp;
	}

	public void setStartEndpoint( MapPoint startPoint, MapPoint endPoint )
	{
		startRoutePoint = startPoint;
		endRoutePoint = endPoint;
	}

	public void setNewRoute( GeoCarRoute route )
	{
		this.route = route;
	}
	
	public boolean equals(Object md) {
		
		RoutingApplicationData a = (RoutingApplicationData)md;
		return this.congestion == a.congestion &&
			   this.prevStreet == a.prevStreet &&
			   this.nextStreet == a.nextStreet &&
			   this.jointId == a.jointId && 
			   this.msg.compareTo(a.msg) == 0;	
	}

	public Pair<Long,Long> getP() {
		return p;
	}

	public void setP(Pair<Long,Long> p) {
		this.p = p;
	}

	public RoutingRoadCost getC() {
		return c;
	}

	public void setC(RoutingRoadCost c) {
		this.c = c;
	}
}