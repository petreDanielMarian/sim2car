package model.OSMgraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import model.LocationParse;
import model.PeanoKey;
import model.parameters.Globals;
import utils.Pair;
import utils.SphericalMercator;
import utils.Triplet;
import utils.tracestool.Utils;
import utils.tracestool.traces.TraceNode;


/**
 * Class to represent a street.
 * It is comprised of queues for each way of the road.
 * And each queue has cells in which one car may be!
 */
public class Way {

	/** Way ID */
	public long id;
	/** Coordinates that bound the street into a rectangle. */
	public double min_lat, min_long, max_lat, max_long;
	/** The collection of nodes composing the street. */
	public Vector<Node> nodes;
	/** Neighbors of the street. */
	public TreeMap<Long, Vector<Long>> neighs;
	
	/** Triplet<Long entityId, int segmentNr, Long cellNr> */
	private List<Triplet<Long, Integer, Long>> nextMoveQueue =
			new LinkedList<Triplet<Long, Integer, Long>>();

	/**
	 * Street is represented as a queue. For each segment we have a queue. 
	 * The number of queues is the number of nodes - 1 ( == number_of_segments ).
	 * We need an array of Vectors as there can be more than one lane on the street.
	 */
	public Vector<TreeMap<Long, Cell>>[] streetQueues;
	
	/** If this is a one way street. */
	public boolean oneway = false;
	/** If this is an enclosed street like circle. rectangle etc. */
	public boolean enclosed = false;

	/** The maximum speed (in m/s) allowed on this road */
	private double maximumSpeed = 14;
	
	private int visits = 0;

	@SuppressWarnings("unchecked")
	public Way(long id) {
		this.id = id;
		
		nodes = new Vector<Node>();
		neighs = new TreeMap<Long, Vector<Long>>();
		streetQueues = (Vector<TreeMap<Long, Cell>>[]) new Vector[2];
		
		for (int i = 0; i < streetQueues.length; i++) {
			streetQueues[i] = new Vector<TreeMap<Long, Cell>>();
		}
		
		min_lat = 360;
		min_long = 360;
		max_lat = -360;
		max_long = -360;
	}

	/** It is oneway or not */
	public void setDirection(boolean val) {
		oneway = val;
	}

	public boolean getDirection() {
		return oneway;
	}

	/** It is enclosed or not */
	public void setEnclosed(boolean val) {
		enclosed = val;
	}

	public boolean getEnclosed() {
		return enclosed;
	}

	/** Returns the maximum speed on this road */
	public double getMaximumSpeed() {
		return maximumSpeed;
	}
	
	public void setMaximumSpeed(double maximumSpeed) {
		this.maximumSpeed = maximumSpeed;
	}

	public int getVisits() {
		return visits;
	}

	public void setVisits(int visits) {
		this.visits = visits;
	}

	/* TODO : Rediscover the goal of this function */
	/** Add virtual Node composing the street */
	public void addVirtualNode(Node nd) {
		int i;
		for (i = 0; i < nodes.size(); i++)
			if (nodes.get(i).id == nd.id)
				break;
		if (i < nodes.size())
			nodes.set(i, nd);
		else
			nodes.add(nd);
	}

	/** Add Node for the street */
	public void addNode(Node nd) {
		/* update data for the bounding rectangle */
		if (nd == null) {
			System.out.println("eroare null ptr");
		}

		double crt_lon = (double)nd.lon;
		double crt_lat = (double)nd.lat;

		if (min_lat - crt_lat >= 0) {
			min_lat = crt_lat;
		}
		if (max_lat - crt_lat <= 0) {
			max_lat = crt_lat;
		}
		if (min_long - crt_lon >= 0) {
			min_long = crt_lon;
		}
		if (max_long - crt_lon <= 0) {
			max_long = crt_lon;
		}

		int i;
		for (i = 0; i < nodes.size(); i++) {
			if (nodes.get(i).id == nd.id) {
				break;
			}
		}
		if (i < nodes.size()) {
			nodes.set(i, nd);
		} else {
			nodes.add(nd);
		}
		
		/* If the street has more than one node we can define a queue. For each new node
		 * added there are add a new segment. */
		if (nodes.size() > 1) {
			
			/* direct direction */
			streetQueues[0].add(new TreeMap<Long, Cell>(new Comparator<Long>() {

				@Override
				public int compare(Long o1, Long o2) {
					return (int)(o1 - o2);
				}
			}));
			
			/* TODO This maybe should be valid also for oneway street, because it can be analyzed as 
			 * a street with directions. */

			/* reverse direction */
			if(!oneway){
				streetQueues[1].add(new TreeMap<Long, Cell>(new Comparator<Long>() {

					@Override
					public int compare(Long o1, Long o2) {
						return (int)(o2 - o1);
					}
				}));
			}
		}

	}

	public Node getNode(long id) {
		for (Node node : nodes) {
			if (node.id == id) {
				return node;
			}
		}
		return null;
	}

	/**
	 * Return the index of a node
	 * @param id - the node's id
	 * @return the node's index
	 */
	public int getNodeIndex(long id) {
		for (Node node : nodes) {
			if (node.id == id) {
				return nodes.indexOf(node);
			}
		}
		return -1;
	}
	
	public Node getNodeByIndex(int index) {
		return nodes.elementAt(index);
	}

	/* Returns the intermediary nodes between 2 points of a street */
	public Vector<Node> getNodesFromA2B(Object A, Object B) {
		double lat_min = -361, lat_max = -361, lon_min = -361, lon_max = -361;
		Vector<Node> AB = new Vector<Node>();
		if (A instanceof Node && B instanceof Node) {
			Node A1 = (Node) A;
			Node B1 = (Node) B;
			lat_min = A1.lat - B1.lat <= 0 ? A1.lat : B1.lat;
			lat_max = A1.lat - B1.lat > 0 ? A1.lat : B1.lat;
			lon_min = A1.lon - B1.lon <= 0 ? A1.lon : B1.lon;
			lon_max = A1.lon - B1.lon > 0 ? A1.lon : B1.lon;
		}
		/* To be removed this section of code when the old TraceParsingTool will be deleted */
		if (A instanceof LocationParse && B instanceof LocationParse) {
			LocationParse A1 = (LocationParse) A;
			LocationParse B1 = (LocationParse) B;
			lat_min = A1.lat - B1.lat <= 0 ? A1.lat : B1.lat;
			lat_max = A1.lat - B1.lat > 0 ? A1.lat : B1.lat;
			lon_min = A1.lon - B1.lon <= 0 ? A1.lon : B1.lon;
			lon_max = A1.lon - B1.lon > 0 ? A1.lon : B1.lon;
		}

		if (A instanceof TraceNode && B instanceof TraceNode) {
			TraceNode A1 = (TraceNode) A;
			TraceNode B1 = (TraceNode) B;
			lat_min = (Double)A1.getY() - (Double)B1.getY() <= 0 ? (Double)A1.getY() : (Double)B1.getY();
			lat_max = (Double)A1.getY() - (Double)B1.getY() > 0 ? (Double)A1.getY() : (Double)B1.getY();
			lon_min = (Double)A1.getX() - (Double)B1.getX() <= 0 ? (Double)A1.getX() : (Double)B1.getX();
			lon_max = (Double)A1.getX() - (Double)B1.getX() > 0 ? (Double)A1.getX() : (Double)B1.getX();
		}
		if (lat_min == -361)
			return AB;
		for (int i = 0; i < nodes.size(); i++) {
			Node nd = nodes.get(i);
			if (lat_min <= nd.lat && nd.lat <= lat_max && lon_min <= nd.lon
					&& nd.lon <= lon_max) {
				AB.add(nd);
			}
		}
		return AB;
	}
	
	public List<PeanoKey> buildPeanoKeys(SphericalMercator sm) {
		List<PeanoKey> peanoKeys = new ArrayList<PeanoKey>();
		peanoKeys.add(new PeanoKey(nodes.get(0).lat, nodes.get(0).lon, id));
		for (int i = 1; i < nodes.size(); i++) {
			Node prev = nodes.get(i - 1);
			Node curr = nodes.get(i);
			double dist = sm.distance(prev.lat, prev.lon, curr.lat, curr.lon);
			if (dist > Globals.maxCellLen) {
				int count = (int) (dist / Globals.maxCellLen);
				double delta_lat = (curr.lat - prev.lat) / (count + 1);
				double delta_lon = (curr.lon - prev.lon) / (count + 1);
				double lat = prev.lat + delta_lat;
				double lon = prev.lon + delta_lon;
				
				/* Add all intermediate points */
				for (int j = 0; j < count; j++) {
					peanoKeys.add(new PeanoKey(lat, lon, id));
					lat += delta_lat;
					lon += delta_lon;
				}
			}

			/* Add final point of the road segment */
			peanoKeys.add(new PeanoKey(curr.lat, curr.lon, id));
		}
		return peanoKeys;
	}

	/* Returns a vector with all the neighbors IDs */
	public Vector<Long> getAllNeighbors() {
		Vector<Long> neigh = new Vector<Long>();
		for (Iterator<Vector<Long>> it = neighs.values().iterator(); it.hasNext();) {
			neigh.addAll(it.next());
		}
		return neigh;
	}

	/* Returns a vector with all the outbound neighbors IDs */
	public Vector<Long> getAllOutNeighbors( TreeMap<Long,Way> graph ) {
		Vector<Long> neigh = new Vector<Long>();
		for (Iterator<Map.Entry<Long,Vector<Long>>> it = neighs.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Long, Vector<Long>> aux = it.next();
			/* iterate over each street neighbors to use only the one that are out links */
			for( Long wayId : aux.getValue() )
			{
				Way neighWay = graph.get(wayId);
				if(neighWay.enclosed)
					neigh.add(wayId);
				else
				{
					if( !neighWay.oneway )
						neigh.add(wayId);
					else
					{
						int idx = neighWay.getNodeIndex(aux.getKey());
						if( idx == -1 )
							continue;
						/* if this node is not the last from neighWay => this street can be considered a
						 * outgoing links, because it has another a part of street on which can continue 
						 * the way.
						 */
						if( idx < neighWay.nodes.size() - 1)
							neigh.add(wayId);
					}
				}
			}
		}
		return neigh;
	}

	/* Returns a vector with all the outbound neighbors IDs grup by intersectioniD */
	public Vector<Pair<Long,Long>> getAllOutNeighborsWithJoints( TreeMap<Long,Way> graph ) {
		Vector<Pair<Long,Long>> neigh = new Vector<Pair<Long,Long>>();
		for (Iterator<Map.Entry<Long,Vector<Long>>> it = neighs.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Long, Vector<Long>> aux = it.next();
			long crtStreetIdx = this.getNodeIndex(aux.getKey());
			if( this.oneway )
			{
				if( crtStreetIdx == 0 )
					continue;
			}
			/* iterate over each street neighbors to use only the one that are out links */
			for( Long wayId : aux.getValue() )
			{
				Way neighWay = graph.get(wayId);
				if(neighWay.enclosed)
					neigh.add(new Pair<Long,Long>(aux.getKey(),wayId));
				else
				{
					if( !neighWay.oneway )
						neigh.add(new Pair<Long,Long>(aux.getKey(),wayId));
					else
					{
						int idx = neighWay.getNodeIndex(aux.getKey());
						if( idx == -1 )
							continue;
						/* if this node is not the last from neighWay => this street can be considered a
						 * outgoing links, because it has another a part of street on which can continue 
						 * the way.
						 */
						if( idx < neighWay.nodes.size() - 1)
							neigh.add(new Pair<Long,Long>(aux.getKey(),wayId));
					}
				}
			}
		}
		return neigh;
	}

	/* Returns a vector with all the inbound neighbors IDs */
	public Vector<Long> getAllInNeighbors( TreeMap<Long,Way> graph ) {
		Vector<Long> neigh = new Vector<Long>();
		for (Iterator<Map.Entry<Long,Vector<Long>>> it = neighs.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Long, Vector<Long>> aux = it.next();
			long crtStreetIdx = this.getNodeIndex(aux.getKey());
			if( this.oneway )
			{
				if( crtStreetIdx == this.nodes.size() -1 )
					continue;
			}
			/* iterate over each street neighbors to use only the one that are out links */
			for( Long wayId : aux.getValue() )
			{

				Way neighWay = graph.get(wayId);
				if(neighWay.enclosed)
				{
					neigh.add(wayId);
				}
				else
				{
					if( !neighWay.oneway )
					{
						neigh.add(wayId);
					}
					else
					{
						int idx = neighWay.getNodeIndex(aux.getKey());
						if( idx == -1 )
							continue;
						/* if this node is one of the neighWay nodes => this street can be considered a
						 * incoming links, because it has another a part of street on which can continue 
						 * the way.
						 */
						if( idx > 0 )
							neigh.add(wayId);
					}
				}
			}
		}
		return neigh;
	}

	/**
	 * Returns the closest node on the street
	 */
	public Node getClosestNode( double lat, double lon) {
		double dist = Double.MAX_VALUE;
		int poz = -1;

		for( int i = 0; i < nodes.size(); i++)
		{
			Node crtNd = nodes.get(i);
			double aux_dist = Utils.distance( lat, lon, crtNd.lat, crtNd.lon );
			if( aux_dist < dist )
			{
				dist = aux_dist;
				poz = i;
			}
		}

		if( poz == -1 )
		{
			return null;
		}
		else
		{
			return nodes.get(poz);
		} 

	}

	@Override
	public String toString() {
		String result = "Way ID = " + id + "\n";
		for (Iterator<Vector<Long>> it = neighs.values().iterator(); it.hasNext();) {
			for (Long id : it.next()) {
				result += id + " ";
			}
			result += "\n";
		}
		
		return result;
	}

	public void queueForNextMove(long entityId, int segmentNr, long cellNr) {
		nextMoveQueue.add(new Triplet<Long, Integer, Long>(entityId, segmentNr, cellNr));
	}
	
	public void clearNextMoveQueue() {
		nextMoveQueue.clear();
	}
	
	public List<Long> getQueuedEntities(int segmentNr, long cellIndex) {
		List<Long> result = new ArrayList<Long>();
		
		for (Triplet<Long, Integer, Long> entry : nextMoveQueue) {
			if (entry.getSecond() == segmentNr && entry.getThird() == cellIndex)
				result.add(entry.getFirst());
		}
		
		return result;
	}
	
	public List<Long> getQueuedEntities(int segmentNr) {
		List<Long> result = new ArrayList<Long>();
		
		for (Triplet<Long, Integer, Long> entry : nextMoveQueue) {
			if (entry.getSecond() == segmentNr)
				result.add(entry.getFirst());
		}
		
		return result;
	}
	
	public void removeQueuedEntity(long entityId) {
		Iterator<Triplet<Long, Integer, Long>> it = nextMoveQueue.iterator();
		while(it.hasNext()) {
			Triplet<Long, Integer, Long> entry = it.next();
			if (entry.getFirst() == entityId) {
				it.remove();
				break;
			}
		}
	}
}