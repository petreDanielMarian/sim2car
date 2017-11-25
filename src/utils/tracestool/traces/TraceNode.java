package utils.tracestool.traces;

import utils.GeoNode;

public class TraceNode extends GeoNode {

	/** trace node timestamp */
	public long timestamp;

	/** flag indicating occupancy */
	public int occupied;

	/** street id */
	public long wid;
	
	/** street with oneway */
	public boolean ontheStreetFirst = false;

	/** additional data */
	public int indexNodes;

	public TraceNode(double lat, double lon, int o, long timestamp) {
		this( lat, lon, timestamp );
		occupied = o;
	}

	public TraceNode(double lat, double lon) {
		super(lat, lon);
	}

	public TraceNode(double lat, double lon, long timestamp ) {
		super(lat, lon);
		this.timestamp = timestamp;
	}

	public void setIdStreet(Long id) {
		wid = id;
	}
	
	public void setIndexNodes(int idx) {
		indexNodes = idx;
	}

	@Override
	public boolean equals(Object object){
		TraceNode a = (TraceNode) object;
		/* equality is considered only for lat,long and wid */
		if( (((Double)a.getX() - (Double)this.getX()) < 0.00000001d) && 
			(((Double)a.getY() - (Double)this.getY()) < 0.00000001d) &&
			(a.wid == this.wid) && (a.indexNodes == this.indexNodes) )
			return true;

		return false;
	}

	public String toString() {
		return "lat:" + super.getY() + " long:" + super.getX() + "timestamp " + timestamp
				+ "busy " + occupied + " Street_id" + wid + "\n";
	}
	
}
