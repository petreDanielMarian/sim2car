package utils.tracestool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import model.GeoCarRoute;
import model.MapPoint;
import model.OSMgraph.Node;
import model.OSMgraph.Way;
import utils.Pair;
import utils.tracestool.parameters.GenericParams;
import utils.tracestool.parameters.OSM;
import utils.tracestool.traces.Trace;
import utils.tracestool.traces.TraceBeijing;
import utils.tracestool.traces.TraceNode;
import utils.tracestool.traces.TraceRome;
import utils.tracestool.traces.TraceSanFrancisco;

public class Utils {

	private final static Logger logger = Logger.getLogger(Utils.class.getName());
	/* Return the angle formed by two vectors */
	public static double getAngle( double a, double b, double c, double d){
		double c_angle = (a*c + b*d)/ Math.sqrt( (a*a + b*b)*(c*c + d*d) );
		return Math.acos(c_angle) * 180 / Math.PI;
	}

	/* Create the trace according its type */
	public static Trace newTrace( String path, String traceName )
	{
		if( GenericParams.mapConfig.getCity().contains("beijing") )
		{
			return new TraceBeijing( path, traceName );
		}
		if( GenericParams.mapConfig.getCity().contains("sanfrancisco") )
		{
			return new TraceSanFrancisco( path, traceName );
		}
		if( GenericParams.mapConfig.getCity().contains("rome") )
		{
			return new TraceRome( path, traceName );
		}
		return null;
	}

	/* Check if a TraceNode is on rectangle area */
	public static boolean check_limits( TraceNode loc, double mLat, double mLon, double MLat, double MLon){
		double lat = (Double)loc.getY();
		double lon = (Double)loc.getX();

		return  mLat - OSM.DIF <= lat && lat <= MLat + OSM.DIF &&
				mLon - OSM.DIF <= lon && lon <= MLon + OSM.DIF;
	}

	/* Determine the projection of a point on a line
	 * System
	 *  PQ: y - yp = -1/m*(x-xp);
	 *  AB: y - ya = m*(x-xa)
	 *
	 *  PQ: y = -1/m * (x - xp) + yp
	 *  AB: y = m*(x-xa) + ya
	 *
	 *  Xq = 1/(1+m^2) * ( xp + m * yp + m^2 *xa - m * ya )
	 *  Yq = -1/m * ( Xq - xp) + yp
	 * */
	public static  TraceNode getProjection2( TraceNode nod, Node a, Node b ){
		TraceNode prj;

		if( b.lon - a.lon == 0){
			return new TraceNode(  a.lat, a.lon, nod.occupied, nod.timestamp);
		}
		double m = (b.lat-a.lat )/(b.lon-a.lon );
		double raport = -1/(1+m*m);

		double Xq = raport * ( (Double)nod.getX() + m * (Double)nod.getY() + m*m * a.lon - m * a.lat );
		double Yq = -1/m * (Xq - (Double)nod.getX() ) + (Double)nod.getY();


		prj = new TraceNode(  Yq, Xq, nod.occupied, nod.timestamp);
		return prj;

	}
	/*  Determine the projection of a point on a line with vectors */
	public static  TraceNode getProjection( TraceNode nod, Node a, Node b ){
		TraceNode prj;
		double apx = (Double)nod.getX() - a.lon;
		double apy = (Double)nod.getY() - a.lat;
		double abx = b.lon - a.lon;
		double aby = b.lat - a.lat;

		double ab2 = abx * abx + aby * aby;
		double ap_ab = apx * abx + apy * aby;
		double t = ap_ab / ab2;
		if (t < 0) {
			t = 0;
		}
		else if (t > 1) {
			t = 1;
		}

		prj = new TraceNode(  a.lat+aby*t, a.lon+abx *t, nod.occupied, nod.timestamp);
		return prj;
	}

	/*  Determine the projection of a point on a line with vectors */
	public static Node getOSMProjection( Node nod, Node a, Node b ){
		Node prj;
		double apx = (Double)nod.lon - a.lon;
		double apy = (Double)nod.lat - a.lat;
		double abx = b.lon - a.lon;
		double aby = b.lat - a.lat;

		double ab2 = abx * abx + aby * aby;
		double ap_ab = apx * abx + apy * aby;
		double t = ap_ab / ab2;
		if (t < 0) {
			t = 0;
		}
		else if (t > 1) {
			t = 1;
		}

		prj = new Node( nod.id, a.lat+aby*t, a.lon+abx *t);
	return prj;
}

	/* Calculate the distance between point on the Earth using haversian formula
	 * d(P1,P2) = 2*RP*arctan( sqrt(x)/sqrt(1-x));
	 * x = sin^2((lat1-lat2)/2)  + cos(lat1)*cos(lat2)*sin^2((long1-long2))/2;
	 * */
	public static double distance( double lat1, double long1, double lat2, double long2 ){
		double dist, x;
		double lat1_rad, long1_rad, lat2_rad, long2_rad;
		lat1_rad = lat1 * Math.PI /180;
		lat2_rad = lat2 * Math.PI /180;
		long1_rad = long1 * Math.PI /180;
		long2_rad = long2 * Math.PI /180;
		x = Math.pow( Math.sin( (lat1_rad - lat2_rad)/2 ), 2 ) +
				Math.cos( lat1_rad ) * Math.cos( lat2_rad ) * Math.pow( Math.sin( (long1_rad - long2_rad) /2 ), 2);
		dist = 2 * OSM.EarthRadius * Math.atan2( Math.sqrt( x),  Math.sqrt(1-x) )*1000;
		return dist;
	}

	/* Return the distance between 2 points (meters) which is measured respecting the layout of the
	 * street, not as direct euclidian distance.
	 */
	public static double getRealDistanceAB( Way street, long AID, long BID)
	{
		double distance_acq = 0f;
		int crtIdx = street.getNodeIndex(AID);
		int jointIdx = street.getNodeIndex(BID);
		int minIdx =  crtIdx < jointIdx ? crtIdx : jointIdx;
		int maxIdx =  crtIdx > jointIdx ? crtIdx : jointIdx;
		for( int i = minIdx; i < maxIdx; i++ )
		{

			Node crtD = street.getNodeByIndex(i);
			Node nextD = street.getNodeByIndex(i+1);
			distance_acq += distance(crtD.lat, crtD.lon, nextD.lat , nextD.lon);

		}
		return distance_acq;
	}

	public static void initDataDijkstra( TreeMap<Long, Way> graph, Node startNode, TreeMap<Pair<Long,Long>,Node> path, TreeMap<Pair<Long,Long>,Long> distance, int depthMax ){

		if( depthMax == 0 )
			return;

		Way w = graph.get( startNode.wayId );
		if( w == null )
			return;

		Vector<Pair<Long,Long>> neigh = Utils.getDirectLinkedJointsFromCrtPosition(graph, startNode );
		for( Pair<Long,Long> entry : neigh ){
			Long jointNodeID = entry.getFirst();
			Pair<Long, Long> p = new Pair<Long, Long>(jointNodeID, entry.getSecond());
			w = graph.get( entry.getSecond() );
			Node aux = w.getNode(jointNodeID);
			Node jointNode = new Node( aux.id, aux.lat, aux.lon, aux.wayId );
			if( !path.containsKey( p )){
				path.put(p, new Node(-1, -1, -1));
				/* Long.MAX_VALUE ---> infinity */
				distance.put(p, Long.MAX_VALUE);
			}
			initDataDijkstra(graph, jointNode, path, distance, depthMax - 1 );
		}
	}

	/* Returns a vector with all the neighbors IDs which are reachable from current node */
	public static Vector<Map.Entry<Long,Vector<Long>>> getAllNeighborsFromCrtPosition( Way w, Node nd) {
		Vector<Map.Entry<Long,Vector<Long>>> neigh = new Vector<Map.Entry<Long,Vector<Long>>>();
		boolean ok = false;
		int idxNd = w.getNodeIndex(nd.id);
		for( Map.Entry<Long,Vector<Long>> entry : w.neighs.entrySet() ) {
			if( w.oneway )
			{
				int idx = w.getNodeIndex(entry.getKey());
				if( idx < idxNd )
					continue;
				ok = true;
			}
			else
			{
				ok = true;
			}

			if( ok )
			{
				neigh.add(entry);
			}

		}

		return neigh;
	}

	public static Node getTheNearestJoint( Way w, int direction, int idx )
	{
		for( int i = idx + direction * 1; direction == 1 ? i < w.nodes.size() : i >= 0 ; i += direction * 1 )
		{
			Node neighNode = w.getNodeByIndex(i);
			if( w.neighs.containsKey(neighNode.id) )
			{
				return neighNode;
			}
		}
		return null;
	}
	/* Returns a vector with all joints IDs which are reachable from joint current node */
	public static Vector<Pair<Long,Long>> getDirectLinkedJointsFromCrtPosition( TreeMap<Long, Way> graph, Node nd ) {
		Vector<Pair<Long,Long>> neigh = new Vector<Pair<Long,Long>>();
		Node neighNode;
		Way w = graph.get(nd.wayId);
		int idxNd = w.getNodeIndex(nd.id);

		/* search before */
		if( !w.oneway )
		{
			neighNode = getTheNearestJoint( w, -1, idxNd );
			if( neighNode != null )
				neigh.add(new Pair<Long, Long>(neighNode.id, w.id));
		}

		neighNode = getTheNearestJoint( w, 1, idxNd );
		if( neighNode != null )
			neigh.add(new Pair<Long, Long>(neighNode.id, w.id));

		Vector<Long> neighRemote = w.neighs.get(nd.id);
		if( neighRemote != null )
		{
			for( Long wid: neighRemote )
			{
				w = graph.get(wid);
				idxNd = w.getNodeIndex(nd.id);
				if( idxNd == -1 )
					continue;
				/* search before */
				if( !w.oneway)
				{
					neighNode = getTheNearestJoint( w, -1, idxNd );
					if( neighNode != null )
						neigh.add(new Pair<Long, Long>(neighNode.id, w.id));
					/*
					 * If current street has two ways and one intersection, it means
					 * that it is blind alley.
					 */
					if( w.neighs.size() == 1 )
					{
						Node neigh_nd = w.getNodeByIndex( w.nodes.size() -1 );
						if( !w.neighs.containsKey(neigh_nd.id) )
							neigh.add(new Pair<Long, Long>( neigh_nd.id, w.id ) );
						neigh_nd = w.getNodeByIndex( 0 );
						if( !w.neighs.containsKey(neigh_nd.id) )
							neigh.add(new Pair<Long, Long>(neigh_nd.id, w.id ) );
					}

				}

				neighNode = getTheNearestJoint( w, 1, idxNd );
				if( neighNode != null )
					neigh.add(new Pair<Long, Long>(neighNode.id, w.id));
			}
		}

		return neigh;
	}

	/* Returns a vector with all joints IDs which are reachable from current node */
	public static Vector<Long> getAllJointsIDFromCrtPosition( Way w, Node nd ) {
		Vector<Long> jointsID = new Vector<Long>();
		if( w == null )
		{
			System.out.println("Strada nula pentru " + nd);
		}
		if( nd == null )
		{
			System.out.println("Nod nul pentru " + w);
		}
		int idxNode = w.getNodeIndex(nd.id);
		int maxIdx = Integer.MAX_VALUE;
		for( Map.Entry<Long,Vector<Long>> entry : w.neighs.entrySet() ) {
			if( w.oneway )
			{
				int idx = w.getNodeIndex(entry.getKey());
				if( idx == -1 )
					continue;
				if( idx <= idxNode )
				{
					if( maxIdx > idx )
					{
						maxIdx = idx;
					}
					continue;
				}
			}

			jointsID.add(entry.getKey());

		}

		if(  (maxIdx - w.nodes.size() < 0) && (maxIdx != Integer.MAX_VALUE) && (jointsID.size() == 0) )
		{
			jointsID.add(w.getNodeByIndex(maxIdx).id);
		}

		return jointsID;
	}

	public static boolean determineAReacheableEndPoint( TreeMap<Long, Way> graph, Node nd )
	{
		Way w = graph.get(nd.wayId);

		/* check if the street is oneway blind alley and it
		 * can not be accessed from other street.
		 */
		if( w.oneway )
		{
			if( w.neighs.size() == 1 )
			{
				/*
				 * get other street because current street is not accessible.
				 */
				for( Long neighID : w.neighs.get(nd.id) )
				{
					if( neighID != nd.wayId )
					{
						Way neighWay = graph.get(neighID);
						if( neighWay.oneway )
						{
							if( neighWay.getNodeIndex(nd.id) != 0 && neighWay.neighs.size() > 1 )
							{
								nd.setWayId(neighID);
								return true;
							}
						}
						else{
							nd.setWayId(neighID);
							return true;
						}
					}
				}
			}
			else
				return true;
		}
		else
			return true;

		return false;
	}
	/* Detect the closest junction */
	public static Node getClosestJointFromCrtPosition( TreeMap<Long, Way> graph, Long wid, Node nd ) {

		if( nd == null )
			return null;

		Way w = graph.get(wid);

		Vector<Long> Joints = Utils.getAllJointsIDFromCrtPosition( w, nd );

		double dist = Long.MAX_VALUE;
		Node joint = null;
		for( Long jointID : Joints )
		{
			Node jointNode = w.getNode(jointID);
			if( jointNode == null )
				continue;
			double aux_dist = Utils.distance(nd.lat, nd.lon, jointNode.lat, jointNode.lon);
			if( aux_dist < dist )
			{
				dist = aux_dist;
				joint = jointNode;
			}
		}

		/* set the ID to joint node in order to be sure that the node has the correct way id */
		if( joint != null )
		{
			Long crt_wid = w.id;
			/*
			 * if the street is oneway and this intersection is the first from current street
			 * due to the Dijkstra convention, we have to set the id of street which is used
			 * to reach this intersection.
			 */
			if( w.oneway )
			{
				if( w.getNodeIndex(joint.id) == 0 )
				{
					for( Long n_wid : w.getAllInNeighbors(graph) )
					{
						w = graph.get(n_wid);
						if( w.getNodeIndex(joint.id) > 0 )
						{
							crt_wid = n_wid;
							break;
						}
					}
				}
			}
			return new Node( joint.id, joint.lat, joint.lon, crt_wid );
		}
		else
		{
			return null;
		}

	}


	public static void initDataDijkstraQ( TreeMap<Long, Way> graph, Node startNode, TreeMap<Pair<Long,Long>,Node> path, TreeMap<Pair<Long,Long>,Long> distance, int depthMax ){

		int level = 0;
		int ok = 0;
		LinkedList<Pair<Long,Long>> q = new LinkedList<Pair<Long,Long>>();
		BufferedWriter w = null;
		Integer[] unexploredNodes = new Integer[depthMax+2];

		Pair<Long,Long> crt = new Pair<Long,Long>(startNode.id, startNode.wayId);
		distance.put( crt, 0l);
		q.addLast(crt);
		unexploredNodes[level] = 1;

		/*
		 * prepare the next level to count the nodes that will be
		 * explored on it.
		 */
		unexploredNodes[level+1] = 0;

		if( ok == 1 )
		{
			try {
				w = new BufferedWriter(new FileWriter("vecini_test.txt"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		while( !q.isEmpty() ){

			/* After all the neighbors from level edge were explored, the algorithm will stop */
			if( level == depthMax )
				break;

			crt = q.getFirst();

			if( ok == 1 )
			{
				try {
					w.write("Explorez nodul" + crt +"\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			unexploredNodes[level]--;

			Way crtW = graph.get(crt.getSecond());
			Node crtNode = crtW.getNode(crt.getFirst());
			Vector<Pair<Long,Long>> neighs = Utils.getDirectLinkedJointsFromCrtPosition( graph, crtNode );

			if( ok == 1 )
			{
				try {
					w.write("Vecini \n" + neighs +"\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			for( Pair<Long,Long> entry : neighs )
			{

				if( !path.containsKey(entry))
				{
					/* Long.MAX_VALUE ---> infinity */
					distance.put(entry, Long.MAX_VALUE);
					path.put(entry, new Node(-1, -1, -1));
				}
				if( !q.contains(entry) )
				{
					if( ok == 1 )
					{
						try {
							w.write("add in queue \n" + entry +"\n");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

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
		if( ok == 1 )
		{
			try {
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
	public static LinkedList<Node> FindPath( TreeMap<Long, Way> graph, Node startNode, Node stopNode, int depthMax ){
		Long dist;
		int level = 0;
		Node crtNode, jointNode = null;
		Pair<Long,Long> crt;
		LinkedList <Node> aux = new LinkedList <Node>();
		LinkedList<Pair<Long,Long>> q = new LinkedList<Pair<Long,Long>>();
		Integer[] unexploredNodes = new Integer[depthMax+2];
		TreeMap<Pair<Long,Long>,Node> path = new TreeMap<Pair<Long,Long>,Node>();
		Vector<Pair<Long,Long>> neighs = new Vector<Pair<Long,Long>>();
		/* In this case, the weight will be the distance between juncture nodes */
		TreeMap<Pair<Long,Long>,Long> distance = new TreeMap<Pair<Long,Long>,Long>();

		initDataDijkstraQ( graph,  startNode, path, distance, depthMax );

		crtNode = path.get( new Pair<Long,Long>(stopNode.id, stopNode.wayId) );
		if( crtNode == null )
			return aux;

		crt = new Pair<Long,Long>(startNode.id, startNode.wayId);
		distance.put( crt, 0l);
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

			Way crtW = graph.get(crt.getSecond());
			crtNode = crtW.getNode(crt.getFirst());
			neighs = Utils.getDirectLinkedJointsFromCrtPosition( graph, crtNode );

			dist = distance.get( crt );

			for( Pair<Long,Long> entry : neighs )
			{

				jointNode = graph.get(entry.getSecond()).getNode(entry.getFirst());

				Long oldd = distance.get(entry);

				if( oldd == null )
					continue;

				crtW = graph.get(jointNode.wayId);

				Long newd = dist + (long)getRealDistanceAB(crtW, crtNode.id, jointNode.id);

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

		aux.add(stopNode);
		crt = new Pair<Long, Long>(stopNode.id, stopNode.wayId);
		crtNode = path.get( crt );
		/* crtNode.id should not return exception if the JVM works correctly */
		if( crtNode == null ){
			aux.clear();
			return aux;
		}

		if( crtNode.id == startNode.id )
		{
			aux.addFirst(crtNode);
			return aux;
		}

		while( crtNode.id != -1 && crtNode.id != startNode.id ){
			crt = new Pair<Long, Long>(crtNode.id, crtNode.wayId);
			crtNode = path.get( crt );
			aux.addFirst(crtNode);
		}

		if( crtNode.id == startNode.id )
			return aux;

		aux.clear();
		return aux;
	}
	public static void showLocWithoutCand( TreeMap<Integer,TraceNode> cs, String fname ){

		FileOutputStream ostream;
		try {
			ostream = new FileOutputStream( "debug_"+fname );

			BufferedWriter outbr = new BufferedWriter(new OutputStreamWriter(ostream));

			for( Iterator<Map.Entry<Integer, TraceNode>> it = cs.entrySet().iterator(); it.hasNext();){
				Map.Entry<Integer,TraceNode> ca = it.next();
				TraceNode c = ca.getValue();
				outbr.write(  ca.getKey() + " "+ c.getY() +" "+ c.getX() +" " + c.occupied + " "+ c.timestamp +"\n");

			}
			outbr.close();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* Normalize a vector */
	public static Vector<Double> normalize( Vector<Double> v )
	{
		Vector<Double> nv = new Vector<Double>();
		double norm = 0d, sum = 0d;
		for( int i = 0; i < v.size(); i++ )
		{
			double element = v.get(i);
			sum += element * element;
		}
		norm = Math.sqrt(sum);
		if( norm == 0 )
		{
			logger.info("Impossible to normalize current vector. Its norm is zero.!" );
			return v;
		}

		for( int i = 0; i < v.size(); i++ )
		{
			nv.add(v.get(i)/norm);
		}

		return nv;
	}
	public static void showLocCand( TreeMap<Integer,Vector<TraceNode>> cs, String fname ){

		FileOutputStream ostream;
		try {
			ostream = new FileOutputStream( fname );

			BufferedWriter outbr = new BufferedWriter(new OutputStreamWriter(ostream));

			for( Iterator<Map.Entry<Integer, Vector<TraceNode>>> it = cs.entrySet().iterator(); it.hasNext();){
				Map.Entry<Integer,Vector<TraceNode>> ca = it.next();
				Vector<TraceNode> c = ca.getValue();
				for( int i = 0; i < c.size(); i++ ){
					TraceNode cx = c.get(i);
					outbr.write(  ca.getKey() + " "+ cx.getY() +" "+ cx.getX() +" " + cx.occupied + " "+ cx.timestamp + " "+ cx.wid+" "+ cx.indexNodes + "\n");
				}

			}
			outbr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Needs to be in sync with JunctionsDetection.
	 **/
	public static List<GeoCarRoute> readCarTraces(String filename) {
		FileInputStream fis = null;
		BufferedReader br = null;
		List<GeoCarRoute> routes = new LinkedList<GeoCarRoute>();
		MapPoint start = null, end = null;
		ArrayList<Node> intersections = null;
		String line;
		String tokens[];
		double latitude;
		double longitude;
		int isOccupied = 0;
		long wayId, nodeId;
		long time = 0l;
		boolean ok = true;

		try {
			fis = new FileInputStream(filename);
			br = new BufferedReader(new InputStreamReader(fis));

			line = br.readLine();

			/* Unable to read file or file corrupted */
			if (line == null || !line.startsWith("start "))
				return null;

			while(line != null) {
				if (line.startsWith("start")) {
					/* detect the intersection start point of the route */
					tokens = line.split(" ");
					latitude = Double.parseDouble(tokens[1]);
					longitude = Double.parseDouble(tokens[2]);
					isOccupied = Integer.parseInt(tokens[3]);
					wayId = Long.parseLong(tokens[4]);
					time = Long.parseLong(tokens[5]);

					start = MapPoint.getMapPoint(latitude, longitude, isOccupied, wayId);
					start.timestamp = new Date(time * 1000);
					intersections = new ArrayList<Node>();
				} else if (line.startsWith("skip")) {
					/* skip this line
					   maybe do something else here */
				} else if (line.startsWith("end")) {

					try {
						tokens = line.split(" ");
						latitude = Double.parseDouble(tokens[1]);
						longitude = Double.parseDouble(tokens[2]);
						wayId = Long.parseLong(tokens[3]);
						time = Long.parseLong(tokens[4]);
						end = MapPoint.getMapPoint(latitude, longitude, isOccupied, wayId);
						end.timestamp = new Date(time * 1000);
						/* build a GeoCarRoute and add it to the list */
						Node aux = null;
						while( intersections.size() > 0)
						{
							aux = intersections.get( intersections.size() - 1 );
							if( end.lat == aux.lat && end.lon == aux.lon )
								intersections.remove(intersections.size() - 1);
							else
								break;
						}
						while( intersections.size() > 0)
						{
							aux = intersections.get( 0 );
							if( start.lat == aux.lat && start.lon == aux.lon )
								intersections.remove( 0 );
							else
								break;

						}

						routes.add( new GeoCarRoute(start, end, intersections) );

					} catch (Throwable t) { /* if something wrong happens with the car reading, just advance to the next car */
						logger.info("Car parsing failed for "+filename);
						t.printStackTrace();
					}
				} else if (!line.isEmpty()){
					try {
						Node crt = null;
						tokens = line.split(" ");
						nodeId = Long.parseLong(tokens[0]);
						latitude = Double.parseDouble(tokens[1]);
						longitude = Double.parseDouble(tokens[2]);
						wayId = Long.parseLong(tokens[3]);
						crt = new Node(nodeId, latitude, longitude, wayId);

						ok = ( intersections.size() == 0 ) || ( intersections.size() > 0 && intersections.get(intersections.size()-1).id != crt.id );

						if( ok )
							intersections.add(crt);

					} catch (Throwable t) { /* if something wrong happens with the car reading, just advance to the next car */
						System.out.println(filename);
						logger.info("Car parsing failed for "+filename);
						t.printStackTrace();
					}
				}
				line = br.readLine();
			}


		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return routes;
	}
	public static TreeMap<Pair<Long,Long>, Pair<Long,Double>> readCongestionStatistics( String fname )
	{
		/*
		 *  key - <streetid_crossed, streetid_tobereach>
		 *  value -<joind_id, congestion_level_encountered_until_streetid_isreached>
		 */
		TreeMap<Pair<Long,Long>, Pair<Long,Double>> statistics = new TreeMap<Pair<Long,Long>, Pair<Long,Double>>();
		
		try {
			BufferedReader in = new BufferedReader( new FileReader(fname));
			String line = null;
			while( (line = in.readLine()) != null )
			{
				StringTokenizer st = new StringTokenizer( line, "->(): ");
				long wayId1 = Long.parseLong(st.nextToken());
				long jointId = Long.parseLong(st.nextToken());
				long wayId2 = Long.parseLong(st.nextToken());
				double g = Double.valueOf(st.nextToken());
				
				statistics.put( new Pair<Long,Long>(wayId1, wayId2), new Pair<Long, Double>(jointId, g));

				//System.out.println(wayId1 + " "+jointId + " "+ wayId2 +" " +g );
			}

			in.close();

		} catch (FileNotFoundException e) {
			System.out.println("File can not be opened " + fname );
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Impossible to read from " + fname );
			e.printStackTrace();
		}

		return statistics;
		
	}


}
