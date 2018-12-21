package model.mobility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import application.routing.RoutingApplicationData.RoutingApplicationState;
import application.routing.RoutingApplicationParameters;
import application.routing.RoutingRoadCost;
import model.Entity;
import model.GeoCar;
import model.GeoCarRoute;
import model.GeoTrafficLightMaster;
import model.MapPoint;
import model.OSMgraph.Cell;
import model.OSMgraph.Node;
import model.OSMgraph.Way;
import model.parameters.Globals;
import model.parameters.MapConfig;
import utils.Pair;
import utils.TraceParsingTool;
import utils.tracestool.Utils;
import utils.tracestool.algorithms.OSMGraph;
import controller.newengine.EngineUtils;
import controller.newengine.SimulationEngine;


public class MobilityEngine {

	private final Logger logger = Logger.getLogger(MobilityEngine.class.getName());

	/* The street graph for the current scenario of (ID, way) ... see @Way */
	public TreeMap<Long,Way> streetsGraph;
	
	public TreeMap<Long,Double> PRGraph;

	private static MobilityEngine _instance = null;
	
	private MobilityEngine(){
		final MapConfig mapConfig = SimulationEngine.getInstance().getMapConfig();
		streetsGraph = EngineUtils.loadGraph(mapConfig.getStreetsFilename(),
				  							 mapConfig.getPartialGraphFilename());
		
		System.out.println(streetsGraph.get(Long.parseLong("48959760")).getClosestNode(37.759842, -122.4767484));
		System.out.println(streetsGraph.get(Long.parseLong("48959760")).nodes.size());
		System.out.println(streetsGraph.get(Long.parseLong("48959760")).nodes.get(0).lat + ", " + streetsGraph.get(Long.parseLong("48959760")).nodes.get(1).lon);
		
		System.out.println(getSegmentNumber(streetsGraph.get(Long.parseLong("48959760")).getClosestNode(37.759842, -122.4767484)));
		
		if( RoutingApplicationParameters.state != RoutingApplicationState.COST_COLLECTING )
		/* TODO: guard this the mobility type */
		PRGraph = EngineUtils.loadPRGraph();
	}

	public static synchronized MobilityEngine getInstance() {
		if (_instance == null) {
			_instance = new MobilityEngine();
		}
		return _instance;
	}
	
	/**
	 * Adds a car to the graph. It creates a cell for it on the street and adds
	 * the car to that cell. The car must have the segment and the cell set.
	 * It can fail if there is another car on that same cell.
	 * 
	 * @param car	the car to be added to the cell queue
	 * @return true	if the cell has been added successfully, false otherwise
	 */
	public boolean addCar(GeoCar car) {
		MapPoint carPoint = car.getCurrentPos();
		Way way = streetsGraph.get(carPoint.wayId);

		int nodeIndex = carPoint.segmentIndex;
		long cellIndex = carPoint.cellIndex;
		
		if (nodeIndex == -1 || cellIndex == -1 || nodeIndex == way.nodes.size()) {
			logger.warning("Could not attach street to car " + car.getId());
			return false;
		}

		synchronized (way) {
			int queueSegmentIndex = (carPoint.direction == 1) ? nodeIndex :
				way.nodes.size() - 1 - nodeIndex;
			int queueNr = (carPoint.direction == 1) ? 0 : 1;

			/*
			 * Test if the segment where current car should be placed is less than the current street number
			 * of segments
			 */
			if (way.streetQueues[queueNr].size() <= queueSegmentIndex)
				return false;
			
			/* Test if the cell with number cellIndex is already populated with other car */
			if (way.streetQueues[queueNr].get(queueSegmentIndex).containsKey(cellIndex)) {
				return false;
			}

			
			Cell newCell = new Cell(cellIndex, car.getId());
			way.streetQueues[queueNr].get(queueSegmentIndex).put(newCell.cellNr, newCell);
			return true;
		}
	}
	
	/**
	 * Removes a given car from the cell queue, based on its current position.
	 * 
	 * @param car	the car to be removed
	 */
	public void removeCar(MapPoint point, long entityId) {

		if (point == null)
			return;

		Way way = streetsGraph.get(point.wayId);
		int queueNr = (point.direction == 1) ? 0 : 1;
		int queueSegment = (point.direction == 1) ? point.segmentIndex
								: way.nodes.size() - 1 - point.segmentIndex;
		long cellIndex = point.cellIndex;
		
		synchronized (way) {
			TreeMap<Long, Cell> queue = way.streetQueues[queueNr].get(queueSegment);
			Cell c = queue.get(cellIndex);
			if (c == null || c.trafficEntityId != entityId) {
				logger.warning("Point not in cell! " + point + " for car " +  entityId);
			} else {
				queue.remove(cellIndex);
			}
		}
	}
	
	public void removeQueuedCar(MapPoint point, long carId) {
		Way way = getWay(point.wayId);
		synchronized (way) {
			way.removeQueuedEntity(carId);
		}
	}
	
	/**
	 * Queues an entity defined by an id and a position on the cell queue
	 * corresponding to the point's position.
	 * 
	 * @param entityId	the id of the entity(car) whose position is to be queued
	 * @param point		the position of the entity
	 */
	public void queueNextMove(long entityId, MapPoint point) {
		Way way = streetsGraph.get(point.wayId);
		synchronized (way) {
			way.queueForNextMove(entityId, point.segmentIndex, point.cellIndex);
		}
	}
	

	/**
	 * Adds a traffic light to the graph. It creates a cell for it on the street and adds
	 * the traffic light to that cell. The traffic light must have the segment and the cell set.
	 * It can fail if there is another car or traffic light on that same cell.
	 * 
	 * @param trafficLight	the traffic light to be added to the cell queue
	 * @return true	if the cell has been added successfully, false otherwise
	 */
	public boolean addTrafficLight(GeoTrafficLightMaster trafficLightMaster) {
		Node node = trafficLightMaster.getNode();
		Way way = streetsGraph.get(node.wayId);

		/*long cellIndex = trafficLightPoint.cellIndex;
		
		//System.out.println("try to add " + way.id + " " + nodeIndex + "Segment " + nodeIndex + " Cell " + cellIndex);
		if (nodeIndex == -1 || cellIndex == -1 || nodeIndex == way.nodes.size()) {
			logger.warning("Could not attach street to traffic light " + trafficLight.getId());
			System.out.println("Could not attach street to traffic light " + trafficLight.getId() + " " + way.nodes.size());
			return false;
		}
		//System.out.println("try to add " + way.id + " " + nodeIndex);*/

		for (Node nodeToSetTrafficLight : trafficLightMaster.getNodes()) {
			nodeToSetTrafficLight.setTrafficLightControl(trafficLightMaster.getId());
		}
//		node.setTrafficLightControl(trafficLightMaster.getId());
		
//		if (way.neighs.containsKey(node.id)) {
//			for (Long wayIdNeigh : way.neighs.get(node.id)) {
//				Way wayNeigh = getWay(wayIdNeigh);
//				//System.out.println(wayIdNeigh + " " + wayNeigh.getDirection());
//				int nodeNeighIndex = wayNeigh.getNodeIndex(node.id);
//				Node nodeNeigh = wayNeigh.getNodeByIndex(nodeNeighIndex);
//				nodeNeigh.setTrafficLightControl(trafficLightMaster.getId());
//			}
//		}
		return true;
	}
	
	/**
	 * Return the cell index for a point between the two given nodes.
	 * Cell 0 is considered to start at node lower.
	 * 
	 * @param lower	the start node of the segment;
	 * 				the reference point in computing the cell index
	 * @param upper	the end node of the segment
	 * @param point the point for which the cell number is to be computed
	 * @return		the cell index
	 */
	public long getCellIndex(Node lower, Node upper, MapPoint point) {
		double segmentLength = TraceParsingTool.distance(
				lower.lat, lower.lon, upper.lat, upper.lon);
		long nrCells = Math.round(segmentLength/Globals.maxCellLen);
		double realCellSize = segmentLength/nrCells;
		
		double distance = TraceParsingTool.distance(
				lower.lat, lower.lon, point.lat, point.lon);
		
		return (long)Math.round(distance/realCellSize);
	}
	
	/**
	 * Return the cell index for a point between the two given nodes.
	 * Cell 0 is considered to start at node lower.
	 * 
	 * @param lower	the start node of the segment;
	 * 				the reference point in computing the cell index
	 * @param upper	the end node of the segment
	 * @param point the point for which the cell number is to be computed
	 * @return		the cell index
	 */
	public long getTrafficLightCellIndex(Node intersection, Node upper) {
		double segmentLength = TraceParsingTool.distance(
				intersection.lat, intersection.lon, upper.lat, upper.lon);
		long nrCells = Math.round(segmentLength/Globals.maxCellLen);
		double realCellSize = segmentLength/nrCells;
		
		double distance = segmentLength;
		
		return (long)0;
	}
	
	/**
	 * Return the cell index for an entity, given a direction, the way and the
	 * segment it is on. The entity is already in the cell queue; if it's not,
	 * this method returns -1.
	 * 
	 * @param way		way of the entity
	 * @param nodeIndex	segment of the entity
	 * @param entityId	id of the entity
	 * @param dir		the direction of the street the entity is on
	 * 					0 for normal, -1 for reverse
	 * @return			the cell index in the queue
	 */
	public long getCellIndex(Way way, int nodeIndex, long entityId, int dir) {
		int queueNr = (dir == 1) ? 0 : 1;
		int queueNodeIndex = (dir == 1) ? nodeIndex :
			way.nodes.size() - 1 - nodeIndex;
		TreeMap<Long, Cell> queue = way.streetQueues[queueNr].get(queueNodeIndex);
		for (Cell cell : queue.values()) {
			if (cell.trafficEntityId == entityId)
				return cell.cellNr;
		}
		return -1;
	}

	/**
	 * Returns the way common to the two given nodes, if such a way exists.
	 * Otherwise returns -1.
	 */
	public long getCommonWayId(Node node1, Node node2) {
		Way way1 = getWay(node1.wayId);
		if (node1.wayId == node2.wayId)
			return node1.wayId;
		if (way1.neighs.containsKey(node1.id)) {
			for (Long wayId : way1.neighs.get(node1.id)) {
				Way way = getWay(wayId);
				for (Node n : way.nodes) {
					if (n.id == node2.id) {
						return wayId;
					}
				}
			}
		}
		
		Way way2 = getWay(node2.wayId);
		if (way2.neighs.containsKey(node2.id)) {
			for (Long wayId : way2.neighs.get(node2.id)) {
				Way way = getWay(wayId);
				for (Node n : way.nodes) {
					if (n.id == node1.id) {
						return wayId;
					}
				}
			}
		}
		return -1;
	}
	
	/**
	 * Returns a point situated in a specific cell, defined by the segment ends
	 * and the index of the cell.
	 * 
	 * @param lower		start node of the segment;
	 * 					the reference point in computing the cell index
	 * @param upper		the end node of the segment
	 * @param cellIndex	the index of the cell in the queue	
	 * @return			a point in the middle of the cell
	 */
	public MapPoint getPointInCell(Node lower, Node upper, long cellIndex) {
		double segmentLength = TraceParsingTool.distance(
				lower.lat, lower.lon, upper.lat, upper.lon);
		long nrCells = Math.round(segmentLength/Globals.maxCellLen);
		
		if( nrCells == 0 )
			return null;

		double realCellSizeLat = (upper.lat - lower.lat)/nrCells;
		double realCellSizeLon = (upper.lon - lower.lon)/nrCells;

		/* TODO: Ask mariana why  realCellSizeLat * 1.5 */
		MapPoint point = MapPoint.getMapPoint(
				lower.lat + realCellSizeLat * cellIndex + realCellSizeLat * 1.5,
				lower.lon + realCellSizeLon * cellIndex + realCellSizeLon * 1.5,
				0, lower.wayId);
		point.cellIndex = cellIndex;
		Way way = getWay(getCommonWayId(lower, upper));
		/* Something wrong with this route */
		if (way == null)
			return null;
		int lowerIndex = way.getNodeIndex(lower.id);
		int upperIndex = way.getNodeIndex(upper.id);
		if (lowerIndex < upperIndex) {
			point.segmentIndex = lowerIndex;
			point.direction = 1;
		} else {
			point.segmentIndex = upperIndex;
			point.direction = -1;
		}
		return point;
	}
	
	/**
	 * Returns the index of the node on the streets it's on.
	 * @param node	the node
	 * @return		the index of the node
	 */
	public int getSegmentNumber(Node node) {
		return streetsGraph.get(node.wayId).getNodeIndex(node.id);
	}
	
	/* TODO This function does not do what the comment say */
	/**
	 * Returns a Node given a point. The node is computed using the
	 * <code>streetSegmentNr</code> of the point, as well as the current way.
	 * @param point	the point for which we need the segment index
	 * @return		the segment the point is on
	 */
	public Node getSegmentByIndex(MapPoint point) {
		return getSegmentByIndex(point.wayId, point.segmentIndex);
	}
	
	/**
	 * Returns a Node given a way that contains the node at the given nodeIndex.
	 * 
	 * @param wayId		the id of the way
	 * @param nodeIndex	the index of the segment
	 * @return			the segment at the given index
	 */
	public Node getSegmentByIndex(long wayId, int nodeIndex) {
		return getWay(wayId).getNodeByIndex(nodeIndex);
	}
	
	/**
	 * Return the Node with the given id which is on the given way.
	 * 
	 * @param wayId		the id of the way
	 * @param nodeId	the id of the node
	 * @return			the node with the given id
	 */
	public Node getSegmentById(long wayId, long nodeId) {
		return getWay(wayId).getNode(nodeId);
	}
	
	/**
	 * Finds the segment of the road the car is on. The car need not be attached
	 * to a cell for this method to work.
	 * 
	 * A segment is the part of the road between two nodes.
	 */
	public int getSegmentForPointNotOnSegment(MapPoint point) {
		Way way = streetsGraph.get(point.wayId);
		Pair<Node, Node> around = getNodesAround(way.id, point);
		if (point.direction == 1)
			return way.getNodeIndex(around.getFirst().id);
		else
			return way.getNodeIndex(around.getSecond().id);
	}
	public static void initDataDijkstraQ( TreeMap<Long, Way> graph, Node startNode, TreeMap<Pair<Long,Long>,Node> path, TreeMap<Pair<Long,Long>,Double> distance, int depthMax ){

		int level = 0;
		LinkedList<Pair<Long,Long>> q = new LinkedList<Pair<Long,Long>>();
		Integer[] unexploredNodes = new Integer[depthMax+2];

		Pair<Long,Long> crt = new Pair<Long,Long>(startNode.id, startNode.wayId);
		distance.put( crt, 0d);
		q.addLast(crt);
		unexploredNodes[level] = 1;

		/*
		 * prepare the next level to count the nodes that will be
		 * explored on it.
		 */
		unexploredNodes[level+1] = 0;

		while( !q.isEmpty() ){

			/* After all the neighbors from level edge were explored, the algorithm will stop */
			if( level == depthMax )
				break;

			crt = q.getFirst();

			unexploredNodes[level]--;

			Way crtW = graph.get(crt.getSecond());
			Node crtNode = crtW.getNode(crt.getFirst());
			Vector<Pair<Long,Long>> neighs = Utils.getDirectLinkedJointsFromCrtPosition( graph, crtNode );

			for( Pair<Long,Long> entry : neighs )
			{

				if( !path.containsKey(entry))
				{
					/* Long.MAX_VALUE ---> infinity */
					distance.put(entry, Double.MAX_VALUE);
					path.put(entry, new Node(-1, -1, -1));
				}
				if( !q.contains(entry) )
				{

					q.addLast(entry);
					unexploredNodes[level+1]++;
				}
			}

			if( unexploredNodes[level] == 0 )
			{
				/*
				 * increment level in order to indicate the number of nodes that should be explored
				 * for current node.
				 */
				level++;
				/*
				 * prepare the next level to count the nodes that will be
				 * explored on it.
				 */
				unexploredNodes[level+1] = 0;
			}

			q.poll();

		}

	}
	
	public void initDataDijkstra( TreeMap<Long, Way> streetsGraph, Node startNode, TreeMap<Pair<Long,Long>,Node> path, TreeMap<Pair<Long,Long>,Double> distance, int depthMax ){

		if( depthMax == 0 )
			return;

		Way w = streetsGraph.get( startNode.wayId );
		if( w == null )
			return;

		Vector<Pair<Long,Long>> neigh = Utils.getDirectLinkedJointsFromCrtPosition(streetsGraph, startNode );
		for( Pair<Long,Long> entry : neigh ){
			Long jointNodeID = entry.getFirst();
			Pair<Long, Long> p = new Pair<Long, Long>(jointNodeID, entry.getSecond());
			w = streetsGraph.get( entry.getSecond() );
			Node aux = w.getNode(jointNodeID);
			Node jointNode = new Node( aux.id, aux.lat, aux.lon, aux.wayId );

			if( !path.containsKey( p )){
				path.put(p, new Node(-1, -1, -1));
				/* Long.MAX_VALUE ---> infinity */
				distance.put(p, Double.MAX_VALUE);
			}

			initDataDijkstra(streetsGraph, jointNode, path, distance, depthMax - 1 );
		}
	}
	
	/**
	 * FindPath - returns a list with the intersections that have to passed.
	 * @param graph - the street graphs
	 * @param startNode - the junction node from where the route should start
	 * @param stopNode - the junction node where the route should stop.
	 * @param depthMax - the maximum number of junctions that will be reached.
	 * @return
	 */
	public GeoCarRoute FindPath( TreeMap<Long, Way> streetsGraph, MapPoint startNd, MapPoint stopNd, int depthMax, TreeMap<Long, TreeMap<Pair<Long,Long>,RoutingRoadCost>> areaCostsGraph ){
		Double dist;
		int level = 0;
		Node crtNode, jointNode = null, startNode = null, stopNode = null;
		Pair<Long,Long> crt;
		GeoCarRoute aux = null;
		ArrayList<Node> intersections = new ArrayList<Node>();
		LinkedList<Pair<Long,Long>> q = new LinkedList<Pair<Long,Long>>();
		Integer[] unexploredNodes = new Integer[depthMax+2];
		TreeMap<Pair<Long,Long>,Node> path = new TreeMap<Pair<Long,Long>,Node>();
		Vector<Pair<Long,Long>> neighs = new Vector<Pair<Long,Long>>();
		/* In this case, the weight will be the distance between juncture nodes */
		TreeMap<Pair<Long,Long>,Double> distance = new TreeMap<Pair<Long,Long>,Double>();

		if( startNd.nodeId != -1 )
		{
			Way sW =  streetsGraph.get(startNd.wayId);
			if(sW != null && sW.neighs.get(startNd.nodeId) != null )
			{
				startNode = sW.getNode(startNd.nodeId);
			}
			
		}
		else
		{
			startNode = Utils.getClosestJointFromCrtPosition( 
					streetsGraph, startNd.wayId, 
					streetsGraph.get(startNd.wayId).getClosestNode( startNd.lat, startNd.lon)
	 				);
		}

		if( stopNd.nodeId != -1 )
		{
			Way sW =  streetsGraph.get(stopNd.wayId);
			if(sW != null && sW.neighs.get(stopNd.nodeId) != null )
			{
				stopNode = sW.getNode(stopNd.nodeId);
			}
		}
		else
		{
			stopNode = Utils.getClosestJointFromCrtPosition( 
									streetsGraph, stopNd.wayId, 
									streetsGraph.get(stopNd.wayId).getClosestNode( stopNd.lat, stopNd.lon)
	 								);
		}
		
		if( startNode == null || stopNode == null )
			return null;

		initDataDijkstraQ( streetsGraph,  startNode, path, distance, depthMax );

		crtNode = path.get( new Pair<Long,Long>(stopNode.id, stopNode.wayId) );
		if( crtNode == null )
			return aux;

		crt = new Pair<Long,Long>(startNode.id, startNode.wayId);
		distance.put( crt, 0d);
		q.addLast(crt);
		path.put(crt, startNode);
		unexploredNodes[level] = 1;

		/* 
		 * prepare the next level to count the nodes that will be
		 * explored on it. 
		 */
		unexploredNodes[level+1] = 0;

		while( !q.isEmpty() ){

			/* After all the neighbors from level edge were explored, the algorithm will stop */
			if( level == depthMax )
				break;

			crt = q.getFirst();

			unexploredNodes[level]--;

			Way crtWay = streetsGraph.get(crt.getSecond());
			crtNode = crtWay.getNode(crt.getFirst());
			neighs = Utils.getDirectLinkedJointsFromCrtPosition( streetsGraph, crtNode );

			dist = distance.get( crt );
			
			TreeMap<Pair<Long,Long>,RoutingRoadCost> costs = areaCostsGraph.get( crt.getSecond() );

			for( Pair<Long,Long> entry : neighs )
			{

				jointNode = streetsGraph.get(entry.getSecond()).getNode(entry.getFirst());
				
				Double oldd = distance.get(entry);
				
				if( oldd == null )
					continue;

				Way crtW = streetsGraph.get(jointNode.wayId);

				double cong = 0.00000001;
				if( costs != null )
				{
					RoutingRoadCost c = costs.get( new Pair<Long,Long>(jointNode.id, entry.getSecond()));
					if( c != null )
						cong = c.cost;
				}
				 Double newd = dist + cong *0.6 + 0.4*PRGraph.get(entry.getSecond());
				/* Double newd = dist + Utils.getRealDistanceAB(crtW, crtNode.id, jointNode.id)*0.6 + 0.4*PRGraph.get(entry.getSecond()) ;*/
				if( newd < oldd ){
					distance.put( entry, newd );
					path.put( entry, crtNode );
					if( !q.contains(entry) )
					{
						q.addLast(entry);
						unexploredNodes[level+1]++;
					}
				}
			}
			if( unexploredNodes[level] == 0 )
			{
				/* 
				 * increment level in order to indicate the number of nodes that should be explored
				 * for current node.
				 */
				level++;
				/* 
				 * prepare the next level to count the nodes that will be
				 * explored on it. 
				 */
				unexploredNodes[level+1] = 0;
			}

			q.poll();

		}

		crt = new Pair<Long, Long>(stopNode.id, stopNode.wayId);
		crtNode = path.get( crt );

		if( crtNode == null ){
			return null;
		}
		
		
		while( crtNode.id != -1 && crtNode.id != startNode.id ){
			crt = new Pair<Long, Long>(crtNode.id, crtNode.wayId);
			crtNode = path.get( crt );
			intersections.add(0, crtNode);
		}

		if( crtNode.id == startNode.id )
			return new GeoCarRoute(startNd, stopNd, intersections);
		
		return null;


	}

	
	/**
	 * Returns the first street intersection on the road of the given car,
	 * within the given distance.
	 * 
	 * @param car	the car for which the intersection is requested
	 * @param distance	how far would we look
	 * @param intersection will contain the intersction that accomplished the condition.
	 * @return			null if there is no intersection ahead
	 * 					the distance at which the intersection was found
	 */
	public boolean isIntersectionAhead(GeoCar car, double distance, Node intersection) {
		MapPoint crtPos = car.getCurrentPos();
		Way way = streetsGraph.get(crtPos.wayId);
		GeoCarRoute route = car.getCurrentRoute();
		double lat = crtPos.lat;
		double lon = crtPos.lon;
		
		intersection = null;

		if (crtPos.equals(route.getEndPoint()))
			return false;
		
		Iterator<Node> it = route.getIntersectionList().iterator();
		while (it.hasNext()) {
			Node next = it.next();
			double dist = TraceParsingTool.distance(lat, lon, next.lat, next.lon);
			way = getWay(next.wayId);
			if (way.neighs.containsKey(next.id)) { /* is intersection */
				if (dist < distance)
				{	
					intersection = next;
					return true;
				}
			}
			if (dist > distance)
				return false;
			distance -= dist;
			lat = next.lat;
			lon = next.lon;
		}
		return false;
	}


	/**
	 * Returns the car in front of the given car, within a given distance.
	 * 
	 * @param	car
	 * @param	distance
	 * @return	null if it's the end of the route
	 * 			Pair(null, null) if there is no car in front of this one
	 * 			Pair(car, distance) if there is a car, where distance is
	 * 					the distance between the two cars
	 */
	public Pair<Entity, Double> getElementAhead(GeoCar car, double distance) {
		MapPoint crtPos = car.getCurrentPos();
		GeoCarRoute route = car.getCurrentRoute();
		Way way = streetsGraph.get(crtPos.wayId);
		Node crt = way.nodes.get(crtPos.segmentIndex);
		Entity elementAhead = null;
		Long crtCell = crtPos.cellIndex;
		double lat = crtPos.lat;
		double lon = crtPos.lon;
		double distAhead = 0.0;

		if (crtPos.equals(route.getEndPoint()))
			return null;
		
		int queueNr = (crtPos.direction == 1) ? 0 : 1;
		
		Iterator<Node> it = route.getIntersectionList().iterator();
		Node next = null;
		while (it.hasNext()) {
			next = it.next();
			
			/* Check if the next node has a traffic light control */
			if (next.hasTrafficLightControl()) {
				elementAhead = SimulationEngine.getInstance().entities.get(next.getTrafficLightId());
				distAhead += TraceParsingTool.distance(lat, lon,
						elementAhead.getCurrentPos().lat, elementAhead.getCurrentPos().lon);
				return new Pair<Entity, Double>(elementAhead, distAhead);				
			}
			
			if (crt.wayId != next.wayId) {
				crt = getSegmentById(next.wayId, crt.id);
				/**
				 *  TODO(mariana): e ceva gresit cu ruta asta, returnam null ca
				 * apoi sa i se faca skip
				 */
				if (crt == null)
					return null;
			}
			way = streetsGraph.get(crt.wayId);
			
			/**
			 * TODO(Cosmin): Check this condition because it 
			 * seems not to work properly due to the fact that
			 * one street can have a joint node in the middle of
			 * the nodes vector. !!!
			 */			
			int direction = getDirection(crt, next);

			queueNr = (direction == 1) ? 0 : 1;
			
			int segmentIndex = way.getNodeIndex(crt.id);
			int queueSegmentIndex = (direction == 1) ? segmentIndex :
					way.nodes.size() - 1 - segmentIndex;
			
			TreeMap<Long, Cell> segmentQueue = way.streetQueues[queueNr].get(queueSegmentIndex);
			
			/* On the first segment, remove cars behind current car */
			if (way.id == crtPos.wayId && segmentIndex == crtPos.segmentIndex) {
				TreeMap<Long, Cell> aux = new TreeMap<Long, Cell>(segmentQueue);
				segmentQueue = new TreeMap<Long, Cell>(aux.tailMap(crtCell));
				segmentQueue.remove(crtCell);
			}
			
			if (segmentQueue.size() != 0) {
				Cell cell = segmentQueue.firstEntry().getValue();
				elementAhead = SimulationEngine.getInstance().
								entities.get(cell.trafficEntityId);
				
				distAhead += TraceParsingTool.distance(lat, lon,
						elementAhead.getCurrentPos().lat, elementAhead.getCurrentPos().lon);
				return new Pair<Entity, Double>(elementAhead, distAhead);
			}
			
			/* No car on this segment, move to the next one. */
			double dist = TraceParsingTool.distance(lat, lon, crt.lat, crt.lon);
			if (dist > distance)
				return new Pair<Entity, Double>(null, null);
			distance -= dist;
			distAhead += dist;
			
			lat = crt.lat;
			lon = crt.lon;
			crt = next;
		}
		
		return new Pair<Entity, Double>(null, null);
	}
	
	/** Returns the street from the graph that has the given id. */
	public Way getWay(long id) {
		return streetsGraph.get(id);
	}

	public Node getNodeOnOppositeSide(long wayId, MapPoint point, Node side) {
		Way way = getWay(wayId);
		Pair<Node, Node> around = getNodesAround(wayId, point);
		if (way.getNodeIndex(side.id) <= way.getNodeIndex(around.getFirst().id))
			return around.getSecond();
		return around.getFirst();
	}
	
	public MapPoint getNodeOnOppositeSide2(Node upper, Node lower) {
		return getPointInCell(lower, upper, 2);
	}
	
	/**
	 * Return the direction of the car going from the first node to the second.
	 * Attention! The nodes must be on the same way.
	 * @return 1 for normal, -1 for opposite
	 */
	public int getDirection(Node first, Node second) {
		Way way = getWay(first.wayId);
		if (way.getNodeIndex(first.id) <= way.getNodeIndex(second.id))
			return 1;
		return -1;
	}
	
	/**
	 * Returns the ends of the segment that contains the given point.
	 * It guarantees that the first node has an index lower than the second.
	 * 
	 * @param wayId		the id of the way the segment is on 
	 * @param point		the point
	 * @return			a pair with the ends of the segment
	 */
	public Pair<Node, Node> getNodesAround(long wayId, MapPoint point) {
		Way way = getWay(wayId);
		double maxAngle = 0.0;
		Node first = null, second = null;
		
		for (int i = 0; i < way.nodes.size() - 1; i++) {
			Node f = way.nodes.get(i);
			Node s = way.nodes.get(i + 1);
			double angle = getAngleBetween(f.lat, f.lon, s.lat, s.lon, point.lat, point.lon);
			if (angle > maxAngle) {
				maxAngle = angle;
				first = f;
				second = s;
			}
		}
		return new Pair<Node, Node> (first, second);
	}
	
	/** Returns the next node in the way, starting from the node towards the point. */
	public Node getNextNode(long wayId, long nodeId, MapPoint point) {
		Way way = getWay(wayId);
		Node node = way.getNode(nodeId);
		int nodeIndex = way.getNodeIndex(nodeId);
		if (nodeIndex == 0)
			return way.getNodeByIndex(1);
		if (nodeIndex == way.nodes.size() - 1)
			return way.getNodeByIndex(nodeIndex - 1);
		Node left = way.getNodeByIndex(way.getNodeIndex(nodeId) - 1);
		Node right = way.getNodeByIndex(way.getNodeIndex(nodeId) + 1);
		
		double distNodePoint = TraceParsingTool.distance(
				node.lat, node.lon, point.lat, point.lon);
		double distNodeRight = TraceParsingTool.distance(
				node.lat, node.lon, right.lat, right.lon);
		double distNodeLeft = TraceParsingTool.distance(
				node.lat, node.lon, left.lat, left.lon);
		double distPointRight = TraceParsingTool.distance(
				point.lat, point.lon, right.lat, right.lon);
		double distPointLeft = TraceParsingTool.distance(
				point.lat, point.lon, left.lat, left.lon);
		
		/* Compute the angle between the current car, the intersection, and the next point
		 * Use formula BAC = acos((BA^2 + AC^2 - BC^2) / 2*BA*AC)
		 */
		double angleRight = (float) Math.toDegrees(
				Math.acos((distNodePoint * distNodePoint + 
						   distPointRight * distPointRight -
						   distNodeRight * distNodeRight) / 
						   (2.0 * distNodePoint * distPointRight)));
		
		double angleLeft = (float) Math.toDegrees(
				Math.acos((distNodePoint * distNodePoint + 
						   distPointLeft * distPointLeft -
						   distNodeLeft * distNodeLeft) / 
						   (2.0 * distNodePoint * distPointLeft)));
		
		return (angleRight > angleLeft) ? right : left;
	}
	
	public boolean isBetween(double rhsLat, double rhsLon,
			double lhsLat, double lhsLon, double pointLat, double pointLon) {
		return getAngleBetween(rhsLat, rhsLon, lhsLat, lhsLon, pointLat, pointLon) > 90;
	}
	
	public double getAngleBetween(double rhsLat, double rhsLon,
			double lhsLat, double lhsLon, double pointLat, double pointLon) {
		double distRhsPoint = TraceParsingTool.distance(
				rhsLat, rhsLon, pointLat, pointLon);
		double distLhsPoint = TraceParsingTool.distance(
				lhsLat, lhsLon, pointLat, pointLon);
		double distRhsLhs = TraceParsingTool.distance(
				rhsLat, rhsLon, lhsLat, lhsLon);

		/*
		 * Compute the angle between the current car, the intersection, and the next point
		 * Use formula BAC = acos((BA^2 + AC^2 - BC^2) / 2*BA*AC)
		 */
		double angle = (float) Math.toDegrees(
				Math.acos((distRhsPoint * distRhsPoint + 
						   distLhsPoint * distLhsPoint -
						   distRhsLhs * distRhsLhs) / 
						   (2.0 * distRhsPoint * distLhsPoint)));
		
		return angle;
	}

	/**
	 * Returns true if the crtCar has on the right side the otherCar.
	 * current is the node between the two, and ahead is the following node in
	 * the route of crtCar.
	 */
	boolean hasOnRightSide(MapPoint crtCar, Node current, Node after, MapPoint otherCar) {
		double distCarCrt = TraceParsingTool.distance(
				crtCar.lat, crtCar.lon, current.lat, current.lon);
		double distCarAfter = TraceParsingTool.distance(
				crtCar.lat, crtCar.lon, after.lat, after.lon);
		double distCrtAfter = TraceParsingTool.distance(
				current.lat, current.lon, after.lat, after.lon);
		double distOtherCrt = TraceParsingTool.distance(
				otherCar.lat, otherCar.lon, current.lat, current.lon);
		double distOtherAfter = TraceParsingTool.distance(
				otherCar.lat, otherCar.lon, after.lat, after.lon);
		
		/* 
		 * Compute the angle between the current car, the intersection, and the next point
		 * Use formula BAC = acos((BA^2 + AC^2 - BC^2) / 2*BA*AC)
		 * The angle returned is between 0-180
		 */
		double angleCarAhead = (float) Math.toDegrees(
				Math.acos((distCrtAfter * distCrtAfter + 
						   distCarCrt * distCarCrt -
						   distCarAfter * distCarAfter) / 
						   (2.0 * distCarCrt * distCrtAfter)));

		angleCarAhead *= Math.signum(
				(after.lon - current.lon) * (crtCar.lat - current.lat) -
				(after.lat - current.lat) * (crtCar.lon - current.lon));
		if (angleCarAhead < 0)
			angleCarAhead += 360;

		/* Compute the angle between the other car, the intersection, and the next point */ 
		double angleOtherAhead = (float) Math.toDegrees(
				Math.acos((distCrtAfter * distCrtAfter + 
						   distOtherCrt * distOtherCrt -
						   distOtherAfter * distOtherAfter) / 
						   (2.0 * distOtherCrt * distCrtAfter)));

		angleOtherAhead *= Math.signum(
				(after.lon - current.lon) * (otherCar.lat - current.lat) -
				(after.lat - current.lat) * (otherCar.lon - current.lon));

		if (angleOtherAhead < 0)
			angleOtherAhead += 360;

		return angleCarAhead < angleOtherAhead;
	}
	
	public boolean hasPriorityOver(MapPoint crtCar, Node current, Node after, MapPoint otherCar) {
		Way wayCurrent = getWay(current.wayId);
		Way wayAfter = getWay(current.wayId);
		
		/* The car is already on a road, so it has priority */
		if (crtCar.wayId == wayCurrent.id && wayCurrent.id == wayAfter.id)
			return true;
		
		return !hasOnRightSide(crtCar, current, after, otherCar);
	}
	
	
	/**
	 * Moves the car to the given new position. If the current position cell is
	 * occupied, goes back one cell and tries to move there. The between list
	 * has all the nodes between the car's current position and the new position
	 * and is used to know where to go back.
	 * 
	 * Next are nodes that were part of the current movement (were in the 
	 * between list at some point), but all the cells there are occupied and
	 * the car couldn't move. This list is updated and the changes reflect
	 * outside the scope of this function, so the the nodes could be added back
	 * to the route of the car.
	 * 
	 * This method looks at whether the cell corresponding to the next position
	 * is free - if it is, then it moves the car to that cell and returns the
	 * new position. If the cell is occupied, tries to move the car that's
	 * currently in the cell. If the cell is free afterwards, it moves this car
	 * there, otherwise moves one cell backwards and calls recursively this
	 * method.
	 *
	 * @param car			the car to be moved
	 * @param newPosition	the new position of the car
	 * @param between		a list of nodes between car's current position
	 * 						and new position
	 * @param next			a list of nodes after the car's current position,
	 * 						and part of its route
	 * @return		the new position of the car
	 */
	public MapPoint moveCarInCell(GeoCar car, MapPoint newPosition,
								  List<Node> between, List<Node> next) {
		SimulationEngine simulationEngine = SimulationEngine.getInstance();
		Way newWay = getWay(newPosition.wayId);
		Way oldWay = getWay(car.getCurrentPos().wayId);
		
		if (car.getSpeed() == 0) {
			return car.getCurrentPos();
		}

		/* could not find an empty cell, the car must stay where it is */
		MapPoint currentPosition = car.getCurrentPos();
		if (currentPosition.wayId == newPosition.wayId &&
				currentPosition.segmentIndex == newPosition.segmentIndex &&
				currentPosition.cellIndex == newPosition.cellIndex) {
			return newPosition;
		}

		Node prevNode = null;  /* the previous node of the new position */
		if (between.size() == 0)
			prevNode = oldWay.getNodeByIndex(car.getCurrentPos().segmentIndex);
		else
			prevNode = between.get(between.size() - 1);

		Node nextNode = null;  /* the next node of the new position */
		if (next.size() == 0) {
			nextNode = car.getCurrentRoute().getIntersectionList().get(0);
		} else
			nextNode = next.get(0);

		if (prevNode.wayId != nextNode.wayId) {
			prevNode = getSegmentById(nextNode.wayId, prevNode.id);
		}
		
		int direction = getDirection(prevNode, nextNode);
		int queueNr = (direction == 1) ? 0 : 1;
		
		int segmentIndex = newPosition.segmentIndex;
		int queueSegmentIndex = (direction == 1) ? segmentIndex :
							newWay.nodes.size() - 1 - segmentIndex;
		long cellIndex = newPosition.cellIndex;

		Cell cell = newWay.streetQueues[queueNr].get(queueSegmentIndex).get(cellIndex);
		if (cell == null) {
			cell = new Cell(cellIndex, car.getId());
			synchronized (newWay) {
				newWay.streetQueues[queueNr].get(queueSegmentIndex).put(cellIndex, cell);
			}
			return newPosition;
		}
		/* The cell of the current position is occupied
		 * First move the car that is already there, as it might not have moved yet */
		GeoCar otherCar = (GeoCar) simulationEngine.getEntityById(cell.trafficEntityId);
		otherCar.move();
		
		/* Re-check whether the cell is free or not */
		cell = newWay.streetQueues[queueNr].get(queueSegmentIndex).get(cellIndex);
		if (cell == null) {
			cell = new Cell(cellIndex, car.getId());
			synchronized (newWay) {
				newWay.streetQueues[queueNr].get(queueSegmentIndex).put(cellIndex, cell);
			}
			return newPosition;
		}

		/* The cell is still occupied, we need to find another cell to place the car */
		cellIndex--;
		if (cellIndex == -1) {
			next.add(0, between.remove(between.size() - 1));
			nextNode = next.get(0);
			if (between.size() == 0)
				prevNode = oldWay.getNodeByIndex(car.getCurrentPos().segmentIndex);
			else
				prevNode = between.get(between.size() - 1);
			if (prevNode.wayId != nextNode.wayId) {
				prevNode = getSegmentById(nextNode.wayId, prevNode.id);
			}
			segmentIndex = getWay(prevNode.wayId).getNodeIndex(prevNode.id);
			cellIndex = getCellIndex(prevNode, nextNode, new MapPoint(null, prevNode.lat, prevNode.lon, false, 0));
			direction = getDirection(prevNode, nextNode);
		}
		MapPoint newPoint = getPointInCell(prevNode, nextNode, cellIndex);
		newPoint.occupied = car.getCurrentPos().occupied;
		newPoint.segmentIndex = segmentIndex;
		newPoint.cellIndex = cellIndex;
		newPoint.direction = direction;
		
		return moveCarInCell(car, newPoint, between, next);
	}

	/**
	 * Updated the given list of intersections with all intermediary nodes.
	 * The changes are all in place.
	 * 
	 * @param intersections	the list to be updated
	 */
	public void updateIntersectionListWithNodes(List<Node> intersections) {
		List<Node> nodes = new LinkedList<Node>();
		Node prev = null, crt = null;
		nodes.add(intersections.get(0));
		for (int i = 1; i < intersections.size(); i++) {
			prev = intersections.get(i - 1);
			crt = intersections.get(i);
			Way way = getWay(crt.wayId);
			int startIndex = way.getNodeIndex(prev.id);
			int endIndex = way.getNodeIndex(crt.id);
			if (!way.oneway && startIndex > endIndex) {
				for (int j = startIndex - 1; j >= endIndex; j--) {
					nodes.add(way.getNodeByIndex(j));
				}
			} else {
				for (int j = startIndex + 1; j <= endIndex; j++) {
					nodes.add(way.getNodeByIndex(j));
				}
			}
		}
		intersections.clear();
		intersections.addAll(nodes);
	}

	/**
	 * Returns the cars whose next position is on the route defined by the
	 * start and end point, and the intermediate nodes.
	 * @param start		The start node of the route
	 * @param end		The end node of the route
	 * @param nodes		The intermediate nodes of the route
	 * @return			A list of cars on the route
	 */
	public List<Entity> getCarsOnRoute(MapPoint start, MapPoint end, List<Node> nodes) {
		Way way = getWay(start.wayId);
		Node currentNode = null;
		Node nextNode = way.getNodeByIndex(start.segmentIndex);
		List<Entity> result = new LinkedList<Entity>();
		SimulationEngine simulationEngine = SimulationEngine.getInstance();
		
		Iterator<Node> it = nodes.iterator();
		while (it.hasNext()) {
			currentNode = nextNode;
			nextNode = it.next();
			
			if (currentNode.wayId != nextNode.wayId) {
				currentNode = getSegmentById(nextNode.wayId, currentNode.id);
			}
			List<Long> entities = way.getQueuedEntities(getSegmentNumber(currentNode));
			for (Long id : entities) {
				result.add(simulationEngine.getEntityById(id));
			}
		}
		
		return result;
	}

	/**
	 * 
	 * @param point
	 * @param prevNode
	 * @return
	 */
	public MapPoint getPointBefore(MapPoint point, Node prevNode) {
		long cellIndex = point.cellIndex--;
		Node currentNode = getSegmentByIndex(point.wayId, point.segmentIndex);
		int segmentIndex = point.segmentIndex;
		int direction = point.direction;
		if (cellIndex == -1) {
			if (prevNode == null)
				prevNode = getSegmentByIndex(point.wayId, point.segmentIndex - 1);
			segmentIndex = getWay(prevNode.wayId).getNodeIndex(prevNode.id);
			cellIndex = getCellIndex(prevNode, currentNode, new MapPoint(null, prevNode.lat, prevNode.lon, false, 0));
			direction = getDirection(prevNode, currentNode);
		}
		MapPoint newPoint = getPointInCell(prevNode, currentNode, cellIndex);
		newPoint.occupied = point.occupied;
		newPoint.segmentIndex = segmentIndex;
		newPoint.cellIndex = cellIndex;
		newPoint.direction = direction;
		return newPoint;
	}
	
}
