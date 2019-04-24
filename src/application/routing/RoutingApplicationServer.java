package application.routing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import utils.Pair;
import utils.tracestool.Utils;
import controller.engine.EngineInterface;
import controller.engine.EngineSimulation;
import controller.network.NetworkInterface;
import controller.network.NetworkType;
import controller.newengine.SimulationEngine;
import application.Application;
import application.ApplicationType;
import application.routing.RoutingApplicationData.RoutingApplicationState;
import model.Entity;
import model.GeoCarRoute;
import model.GeoServer;
import model.MapPoint;
import model.OSMgraph.Node;
import model.OSMgraph.Way;
import model.mobility.MobilityEngine;
import model.network.Message;
import model.network.MessageType;

/**
 * This class is used to represent the server functionality
 * It receives messages from the car and other servers
 * Updates it's zone map with costs and responds to new route requests
 * @author Alex
 *
 */
public class RoutingApplicationServer extends Application{

	private final static Logger logger = Logger.getLogger(RoutingApplicationServer.class.getName());
	/* Reference to the server object */
	GeoServer serv;
	/* The size of Servers' Grid */
	long rows, cols;

	/* The size of the area for which current server is responsible */
	double latEdge, lonEdge;

	/* round robin for request */
	public TreeMap<Long,Long> stRound = new TreeMap<Long,Long>();

	/* The type of application  */
	private ApplicationType type = ApplicationType.ROUTING_APP;

	public TreeMap<Long,Double> streetsAVGSpeed = new TreeMap<Long,Double>();
	
	/* The graph for Server area costs */
	public TreeMap<Long, TreeMap<Pair<Long,Long>,RoutingRoadCost>> areaCostsGraph = new TreeMap<Long, TreeMap<Pair<Long,Long>,RoutingRoadCost>>();

	/* The global graph with costs */
	public TreeMap<Long, TreeMap<Pair<Long,Long>,Double>> globalCostsGraph = new TreeMap<Long, TreeMap<Pair<Long,Long>,Double>>();

	public RoutingApplicationServer(GeoServer serv) {
		this.serv = serv;
		initializeStreetsCostGraph();

	}

	public void printStreetsCostGraph(String filename)
	{
		try {
			FileWriter wr = new FileWriter( new File( filename ));
			for( Long streetId: areaCostsGraph.keySet() )
			{
				TreeMap<Pair<Long,Long>,RoutingRoadCost> costs = areaCostsGraph.get(streetId);
				for( Map.Entry<Pair<Long,Long>,RoutingRoadCost> c : costs.entrySet() )
				{
					wr.write( streetId + "->" + c.getKey() + ": " + String.format("%.10f",c.getValue().cost) + "\n" );
				}

			}
			wr.flush();
			wr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void printStreetsAVGSpeed(String filename)
	{
		try {
			FileWriter wr = new FileWriter( new File( filename ));
			for( Long streetId: streetsAVGSpeed.keySet() )
			{
				Double cost = streetsAVGSpeed.get(streetId);
				wr.write( streetId + "->" + String.format("%.10f", cost) + "\n" );
			}
			wr.flush();
			wr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean getStatus() {
		return false;
	}

	/* When it runs the server only send the messages it has in queue or the regular zone updates */
	@Override
	public String run() {
		NetworkInterface net = serv.getNetworkInterface(NetworkType.Net_WiFi);
		sendWayCosts();
		net.processOutputQueue();
		if( RoutingApplicationParameters.routingapp_debug )
		{
			if( (SimulationEngine.getInstance().getSimulationTime() + 2) % RoutingApplicationParameters.SamplingInterval == 0 )
			{
				printStreetsCostGraph("streetcostGraph_" + serv.getId() + "_"+SimulationEngine.getInstance().getSimulationTime()+".txt");
				printStreetsAVGSpeed("streetAVGSpeeds_" + serv.getId() + "_"+SimulationEngine.getInstance().getSimulationTime()+".txt");
				streetsAVGSpeed.clear();
			}
		}
		return "";
	}

	long basePar = 500L;
	long baseImpar = 400L;
	/* Sends the information regarding road costs to all the neighbor servers
	 * The streets are only those of interest for the neighbor so not all streets from a server zone are send to
	 * it's neighbors*/
	private void sendWayCosts() {
		if (this.serv.getId() % 2 == 0) {
			if (SimulationEngine.getInstance().getSimulationTime() == basePar) {
				basePar += 500;
				for (Long neighId : serv.neighServers) {
					for (Map.Entry<Long, TreeMap<Pair<Long,Long>,RoutingRoadCost>> entry : areaCostsGraph.entrySet()) {
						Long prevStreet = entry.getKey();
						for (Map.Entry<Pair<Long,Long>,RoutingRoadCost> entry2 : entry.getValue().entrySet()) {
							if (entry2.getValue().commonServers.contains(neighId)) {
								Message m = new Message(serv.getId(), neighId, null, MessageType.SERVER_UPDATE, ApplicationType.ROUTING_APP);
								RoutingApplicationData data = new RoutingApplicationData( "Road Info from: " + serv.getId(),
										0, prevStreet, -1, -1,
										SimulationEngine.getInstance().getSimulationTime());
								data.setC(entry2.getValue());
								data.setP(entry2.getKey());
								m.setPayload(data);
								this.serv.getNetworkInterface(NetworkType.Net_WiFi).putMessage(m);
							}
						}
					}
				}
			}
		} else {
			if (SimulationEngine.getInstance().getSimulationTime() == baseImpar) {
				baseImpar += 400;
				for (Long neighId : serv.neighServers) {
					for (Map.Entry<Long, TreeMap<Pair<Long,Long>,RoutingRoadCost>> entry : areaCostsGraph.entrySet()) {
						Long prevStreet = entry.getKey();
						for (Map.Entry<Pair<Long,Long>,RoutingRoadCost> entry2 : entry.getValue().entrySet()) {
							if (entry2.getValue().commonServers.contains(neighId)) {
								Message m = new Message(serv.getId(), neighId, null, MessageType.SERVER_UPDATE, ApplicationType.ROUTING_APP);
								RoutingApplicationData data = new RoutingApplicationData( "Road Info from: " + serv.getId(),
										0, prevStreet, -1, -1,
										SimulationEngine.getInstance().getSimulationTime());
								data.setC(entry2.getValue());
								data.setP(entry2.getKey());
								m.setPayload(data);
								this.serv.getNetworkInterface(NetworkType.Net_WiFi).putMessage(m);
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public String stop(){
		return null;
	}

	@Override
	public String getInfoApp() {
		return null;
	}

	@Override
	public Object getData() {
		return null;
	}

	@Override
	public ApplicationType getType() {
		return type;
	}

	@Override
	public synchronized void process(Message m) {
		Message reply;
		MessageType msgType = MessageType.UNKNOWN;
		double c = 0d;
		//Object[] data = (Object[]) m.getData();
		/* If the data is destined to other type of application is dropped */
		if (m.getDestId() != serv.getId())
			return;

		RoutingApplicationData data = (RoutingApplicationData)m.getPayload();

		// Update server info with info from neighbor servers.
		if (m.getType() == MessageType.SERVER_UPDATE) {
			TreeMap<Pair<Long,Long>, RoutingRoadCost> costs = areaCostsGraph.get(data.prevStreet);

			/* IF we dnt have the cost we add it */
			if (costs.get(data.getP()) == null) {
				costs.put(data.getP(), data.getC());
				System.out.println("add");
			} else if (costs.get(data.getP()).time > data.getC().time) {
				costs.put(data.getP(), data.getC());
				//System.out.println("update");
			}

			areaCostsGraph.put(data.prevStreet, costs);
			return;
		}

		//System.out.println("The message received type is " + m.getType() + " from " + m.getSourceId() + "   "+((RoutingApplicationData)m.getPayload()).congestion );
		NetworkInterface net = serv.getNetworkInterface(NetworkType.Net_WiFi);

		/* Cars always send the costs of the streets but the update route is only computed after simulation 1800 seconds */
		if( SimulationEngine.getInstance().getSimulationTime() / RoutingApplicationParameters.SamplingInterval < 1 )
		{
			msgType = MessageType.NO_ROUTE_UPDATE;
		}
		else
		{
			msgType = MessageType.NEW_ROUTE_UPDATE;
		}

		reply = new Message(serv.getId(), m.getSourceId(), null, msgType, ApplicationType.ROUTING_APP);

		// Update the costs with the ifo from the car
		if( m.getType() == MessageType.REQUEST_ROUTE_UPDATE || m.getType() == MessageType.COST_ROAD_UPDATE )
		{

			TreeMap<Pair<Long,Long>, RoutingRoadCost> costs = areaCostsGraph.get(data.prevStreet);
			if( costs == null )
			{
				System.out.println("Street " + data.prevStreet + " is not available on current area graph \n");
				return;
			}

			RoutingRoadCost localCost = costs.get(new Pair<Long,Long>(data.jointId, data.nextStreet));
			if( localCost == null )
			{
				//				System.out.println( "SERVER " + serv.getId() + " " +
				//							 "Street " + data.prevStreet + " is not linked with street " + data.nextStreet +
				//							 " via joinction " + data.jointId
				//							);


				if( data.avgspeed != 0 && data.avgspeed < 36)
				{
					Double avgspeed = streetsAVGSpeed.get(data.prevStreet);
					if( avgspeed != null )
					{
						streetsAVGSpeed.put(data.prevStreet , (avgspeed + data.avgspeed )/2);
					}
					else
					{
						streetsAVGSpeed.put(data.prevStreet , data.avgspeed);
					}
				}

				/* if the street is not linked, we will spread the cost to all its output streets */
				for( Map.Entry<Pair<Long,Long>, RoutingRoadCost> p : costs.entrySet() )
				{
					c = p.getValue().updateStreetCost(data.congestion, data.timestamp);
					costs.put(p.getKey(), p.getValue());

				}
			}
			else
			{
				c = localCost.updateStreetCost(data.congestion, data.timestamp);
				costs.put(new Pair<Long,Long>(data.jointId, data.nextStreet), localCost);
			}
			areaCostsGraph.put(data.prevStreet, costs);

			if( data.avgspeed != 0 && data.avgspeed < 45)
			{
				Double avgspeed = streetsAVGSpeed.get(data.prevStreet);
				if( avgspeed != null )
				{
					streetsAVGSpeed.put(data.prevStreet , (avgspeed + data.avgspeed )/2);
				}
				else
				{
					streetsAVGSpeed.put(data.prevStreet , data.avgspeed);
				}
			}

			//System.out.println( "Noul grad de congestie este pentru " + data.prevStreet + "->" + data.nextStreet + " " +  String.format("%.10f",c));
			if( RoutingApplicationParameters.state == RoutingApplicationState.COST_COLLECTING )
				return;
		}
		// IF time < 1800 we accept only updates
		if( msgType == MessageType.NO_ROUTE_UPDATE )
			return;


		/* get destination Entity */
		EngineInterface engine = SimulationEngine.getInstance();
		/* Maintain backward compatibility with old simulator */
		Entity destEntity = engine != null ? ((SimulationEngine)engine).getEntityById(m.getSourceId()): 
			EngineSimulation.getInstance().getCarById((int)m.getSourceId());

		if (Utils.distance(serv.getCurrentPos().lat, serv.getCurrentPos().lon,
				destEntity.getCurrentPos().lat, destEntity.getCurrentPos().lon) > RoutingApplicationParameters.distMax) {
			System.out.println("Car " + destEntity.getId() + "has no server in range");
			return;
		}

//		String x = null;
//		
//		if (x == null)
//			return;
		/* Compute new route for car*/
		if( m.getType() == MessageType.REQUEST_ROUTE_UPDATE )
		{
			GeoCarRoute newroute = null;
			List<Node> intersectionsList = data.route.getOriginalIntersectionList();
			if( data.startRoutePoint != null & data.startRoutePoint != null )
			{
				MapPoint endPoint = intersectionsList.size() > 3 * RoutingApplicationParameters.maxdepth/4 ? 
						MapPoint.getMapPoint( intersectionsList.get(3*RoutingApplicationParameters.maxdepth/4)):
							data.route.getEndPoint();
						newroute = MobilityEngine.getInstance().FindPath( MobilityEngine.getInstance().streetsGraph, data.startRoutePoint, endPoint, RoutingApplicationParameters.maxdepth, areaCostsGraph );
						//System.out.println(data.startRoutePoint +" " + data.route.getEndPoint() +" "+ m.getSourceId() + " " +newroute);
			}
			if( newroute == null )
				return;

			/* add the other intermediary joints */
			if( !newroute.getEndPoint().equals(data.route.getEndPoint()) )
			{
				List<Node> newintersectionsList = newroute.getIntersectionList();
				newroute.setEndPoint(data.route.getEndPoint());
				for( int i = 3 * RoutingApplicationParameters.maxdepth/4; i < intersectionsList.size(); i++)
				{
					newintersectionsList.add(intersectionsList.get(i));
				}
				newroute.reinitIntersectionLists(newintersectionsList);
			}

			data = new RoutingApplicationData( "Reply from server " + serv.getId(),
					c, data.prevStreet, data.nextStreet, data.jointId,
					SimulationEngine.getInstance().getSimulationTime());
			data.setNewRoute( newroute );
			reply.setPayload(data);
			net.putMessage(reply);

		}
	}

	public void setGridSize(long rows, long cols)
	{
		this.rows = rows;
		this.cols = cols;
	}

	public long getRowsNumber()
	{
		return rows;
	}

	public long getColsNumber()
	{
		return cols;
	}

	public void setServerAreaSizes( double latEdge, double lonEdge )
	{
		this.latEdge = latEdge;
		this.lonEdge = lonEdge;
	}

	public void initializeStreetsCostGraph() {

		TreeMap<Long, Way> streetsGraph = MobilityEngine.getInstance().streetsGraph;
		/* iterate over each street and detect the output */
		for( Long streetID:streetsGraph.keySet() ) {
			Way tmpSt = streetsGraph.get(streetID);

			if( tmpSt == null ) {
				logger.info( streetID + "there is not presented in the graph of Streets \n");
				continue;
			}

			/*  obtain the output streets */
			Vector<Pair<Long,Long>> outstreetsIDs = tmpSt.getAllOutNeighborsWithJoints(streetsGraph);

			TreeMap<Pair<Long,Long>,RoutingRoadCost> costs = new TreeMap<Pair<Long,Long>,RoutingRoadCost>();

			for( Pair<Long,Long> n : outstreetsIDs) {
				if( belongToCrtArea(streetsGraph, n, this.serv)) {
					RoutingRoadCost c = new RoutingRoadCost();
					// Add myself as common
					c.commonServers.add(this.serv.getId());
					// Add neighbors as common
					//System.out.println("aici");
					for (Long neighId : this.serv.neighServers) {
						if (SimulationEngine.getInstance().getEntityById(neighId) instanceof GeoServer) {
							GeoServer s = (GeoServer) SimulationEngine.getInstance().getEntityById(neighId); 
							if (s != null && belongToCrtArea(streetsGraph, n, s)) {
								c.commonServers.add(s.getId());
							}
						}
					}

					costs.put(n, c);
				}
			}

			areaCostsGraph.put( tmpSt.id, costs);

		}
	}

	/* These functions should be used when there are multiple servers */
	/* Checks if a way is part of the server area */
	public boolean belongToCrtArea( TreeMap<Long, Way> streetsGraph, Pair<Long,Long> n, GeoServer serve) {
		Way street = streetsGraph.get(n.getSecond());
		Node nd = street.getNode(n.getFirst());

		MapPoint mp = serve.getCurrentPos();

		if (nd == null)
			return false;

		if (Utils.distance(mp.lat, mp.lon, nd.lat, nd.lon) <= RoutingApplicationParameters.distMax) {
			return true;
		}

		return false;
	}

	/* These functions should be used when there are multiple servers */
	public boolean wayBelongHere(Way street) {

		if (street.nodes == null || street.nodes.size() == 0)
			return false;

		Node nd = street.nodes.get(0);

		MapPoint mp = serv.getCurrentPos();

		if (nd == null)
			return false;

		if (Utils.distance(mp.lat, mp.lon, nd.lat, nd.lon) <= RoutingApplicationParameters.distMax) {
			return true;
		}

		return false;
	}
}