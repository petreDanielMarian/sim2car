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
import controller.engine.EngineSimulation;
import controller.network.NetworkInterface;
import controller.network.NetworkType;
import controller.newengine.SimulationEngine;
import application.Application;
import application.ApplicationType;
import application.routing.RoutingApplicationData.RoutingApplicationState;
import model.GeoCarRoute;
import model.GeoServer;
import model.MapPoint;
import model.OSMgraph.Node;
import model.OSMgraph.Way;
import model.mobility.MobilityEngine;
import model.network.Message;
import model.network.MessageType;

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

	@Override
	public String run() {
		NetworkInterface net = serv.getNetworkInterface(NetworkType.Net_WiFi);
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

		//System.out.println("The message received type is " + m.getType() + " from " + m.getSourceId() + "   "+((RoutingApplicationData)m.getPayload()).congestion );
		NetworkInterface net = serv.getNetworkInterface(NetworkType.Net_WiFi);
		if( SimulationEngine.getInstance().getSimulationTime() / RoutingApplicationParameters.SamplingInterval < 1 )
		{
			msgType = MessageType.NO_ROUTE_UPDATE;
		}
		else
		{
			msgType = MessageType.NEW_ROUTE_UPDATE;
		}

		reply = new Message(serv.getId(), m.getSourceId(), null, msgType, ApplicationType.ROUTING_APP);

		if( m.getType() == MessageType.REQUEST_ROUTE_UPDATE || m.getType() == MessageType.COST_ROAD_UPDATE )
		{
			
			TreeMap<Pair<Long,Long>, RoutingRoadCost> costs = areaCostsGraph.get(data.prevStreet);
			if( costs == null )
			{
				logger.info( "Street " + data.prevStreet + " is not available on current area graph \n");
				return;
			}
			RoutingRoadCost localCost = costs.get(new Pair<Long,Long>(data.jointId, data.nextStreet));
			if( localCost == null )
			{
				System.out.println( 
							 "Street " + data.prevStreet + " is not linked with street " + data.nextStreet +
							 " via joinction " + data.jointId
							);

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
		if( msgType == MessageType.NO_ROUTE_UPDATE )
			return;
	

		Long round = stRound.get(data.startRoutePoint.wayId);
		if( round == null )
			return;
		round = (round + 1) % Long.MAX_VALUE;
		stRound.put(data.startRoutePoint.wayId, round);
		if( round % 2 == 0 )
		{
			return;
		}
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
	public void initializeStreetsCostGraph()
	{

		TreeMap<Long, Way> streetsGraph = MobilityEngine.getInstance().streetsGraph;
		/* iterate over each street and detect the output */
		for( Long streetID:streetsGraph.keySet() )
		{
			Way tmpSt = streetsGraph.get(streetID);

			if( tmpSt == null )
			{
				logger.info( streetID + "there is not presented in the graph of Streets \n");
				continue;
			}

			/*  obtain the output streets */
			Vector<Pair<Long,Long>> outstreetsIDs = tmpSt.getAllOutNeighborsWithJoints(streetsGraph);

			TreeMap<Pair<Long,Long>,RoutingRoadCost> costs = new TreeMap<Pair<Long,Long>,RoutingRoadCost>();

			for( Pair<Long,Long> n : outstreetsIDs)
			{
				/* TODO: Enable this condition when multi-area servers feature will be
				 * activated.
				 *
				 * if( belongToCrtArea(streetsGraph, n))
				*/
				costs.put(n, new RoutingRoadCost());
			}

			areaCostsGraph.put( tmpSt.id, costs);

		}
	}

	/* These functions should be used when there are multiple servers */
	public boolean belongToCrtArea( TreeMap<Long, Way> streetsGraph, Pair<Long,Long> n )
	{
		double latmin = 0d, latmax = 0d, lonmin = 0d, lonmax = 0d;
		Way street = streetsGraph.get(n.getSecond());

		if( street.oneway )
			return true;

		Node nd = street.getNode(n.getFirst());

		MapPoint mp = serv.getCurrentPos();
		latmin = mp.lat - latEdge/2;
		latmax = mp.lat + latEdge/2;

		lonmin = mp.lon - lonEdge/2;
		lonmax = mp.lon + lonEdge/2;

		if( latmin <= nd.lat && nd.lat <= latmax )
		{
			if( lonmin <= nd.lon && nd.lon <= lonmax )
				return true;
		}

		/* left edge */
		Node prj = Utils.getOSMProjection( nd, new Node(-1, latmin, lonmin), new Node(-1, latmin, lonmax) );
		if( Utils.distance(nd.lat, nd.lon, prj.lat, prj.lon) < RoutingApplicationParameters.distMax )
			return true;

		/* right edge */
		prj = Utils.getOSMProjection( nd, new Node(-1, latmax, lonmin), new Node(-1, latmax, lonmax) );
		if( Utils.distance(nd.lat, nd.lon, prj.lat, prj.lon) < RoutingApplicationParameters.distMax )
			return true;

		/* top edge */
		prj = Utils.getOSMProjection( nd, new Node(-1, latmin, lonmin), new Node(-1, latmax, lonmin) );
		if( Utils.distance(nd.lat, nd.lon, prj.lat, prj.lon) < RoutingApplicationParameters.distMax )
			return true;

		/* bottom edge */
		prj = Utils.getOSMProjection( nd, new Node(-1, latmax, lonmin), new Node(-1, latmax, lonmax) );
		if( Utils.distance(nd.lat, nd.lon, prj.lat, prj.lon) < RoutingApplicationParameters.distMax )
			return true;

		return false;
	}
}

