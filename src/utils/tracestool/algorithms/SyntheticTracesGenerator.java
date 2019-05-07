package utils.tracestool.algorithms;


import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import utils.Pair;
import utils.tracestool.Utils;
import utils.tracestool.parameters.GenericParams;
import utils.tracestool.parameters.SyntheticTracesGeneratorParams;
import utils.tracestool.parameters.TraceJunctionsDetectionParams;
import model.OSMgraph.Node;
import model.OSMgraph.Way;

public class SyntheticTracesGenerator {
	 /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(SyntheticTracesGenerator.class.getName());

	/* The graph of streets */
	static TreeMap<Long, Way> graph;

	public static double minALat, maxALat, minALon, maxALon;
	/*
	 * map limits:
	 * limits[0] -> map_lat_min
	 * limits[1] -> map_lon_min 
	 * limits[2] -> map_lat_max
	 * limits[3] -> map_lon_max 
	 */
	static double [] limits = new double[4];
	
	static Vector<Pair<Long,Long>> jointsIDs;

	/* Determine the joints from traces */
	public static void getSyntheticTraces(){

		/* load the graph */
		logger.info("Starting to load the OSM graph");
		System.out.println(limits.toString());

		long startTime = System.currentTimeMillis();

		graph = OSMGraph.load_graph( GenericParams.mapConfig.getStreetsFilename(), 
		         					 GenericParams.mapConfig.getPartialGraphFilename(), limits );

		
		long stopTime = System.currentTimeMillis();

		logger.info(""+ (stopTime - startTime)/1000 );
		logger.info( "Map's characteristics (" + limits[0] +", "+ limits[1] +") " + " ("+ limits[2] +", "+" " +  limits[3] +" )" );
		logger.info( "Finishing to load the OSM graph" );

		startTracesGeneration();

	}
	
	public static void startTracesGeneration( ){

		minALat = GenericParams.mapConfig.getAreaMinLat();
		maxALat = GenericParams.mapConfig.getAreaMaxLat();
		minALon = GenericParams.mapConfig.getAreaMinLon();
		maxALon = GenericParams.mapConfig.getAreaMaxLon();

		jointsIDs = new Vector<Pair<Long,Long>>();

		for( Map.Entry<Long, Way> entry : graph.entrySet() )
		{
			Way street = entry.getValue();
			for( Long nID : street.neighs.keySet() )
			{
				Node j = street.getNode(nID);
				if( j == null )
					continue;
				if( minALat <= j.lat && j.lat <= maxALat)
				{
					if( minALon <= j.lon && j.lon <= maxALon)
					{
							jointsIDs.add(new Pair<Long,Long>(street.id, nID));
					}
				}
			}
		}

		for( int i = 0; i < SyntheticTracesGeneratorParams.generatedTracesNumber; i++ )
		{
			String name = "joints_" + GenericParams.mapConfig.getCity();
			name += "_gen" + i + ".txt";
			TracesGenerationAlgorithm( name );

		}

	}
	

	public static void TracesGenerationAlgorithm( String fname ){
		
		long startTime = System.currentTimeMillis();

		try {
				FileOutputStream ostream = new FileOutputStream( GenericParams.mapConfig.getResultsJunctionsDetectionAlgPath() +fname );
				BufferedWriter outbr = new BufferedWriter(new OutputStreamWriter(ostream));
	
				Node stopJoint = null, startJoint = null;
				int retry = 0;
				for( int r = 0; r < SyntheticTracesGeneratorParams.routesNb; )
				{
					Way auxStreet;
					if( startJoint == null || retry > 20 )
					{
						Pair<Long,Long> start = jointsIDs.get( new Random().nextInt(jointsIDs.size()));
						auxStreet = graph.get(start.getFirst());
						startJoint = auxStreet.getNode(start.getSecond());
						retry = 0;
					}
					do{
						Pair<Long,Long> stop = jointsIDs.get( new Random().nextInt(jointsIDs.size()));
						if( stop.getSecond() == startJoint.id )
							continue;
						auxStreet = graph.get(stop.getFirst());
						stopJoint = auxStreet.getNode(stop.getSecond());
					} 
					while( Utils.distance(startJoint.lat, startJoint.lon, stopJoint.lat, stopJoint.lon) < 300 );
					
					Node startJointP = Utils.getClosestJointFromCrtPosition( 
							graph, startJoint.wayId,
						    graph.get(startJoint.wayId).getClosestNode( startJoint.lat, startJoint.lon)
						  );

					Node stopJointP = Utils.getClosestJointFromCrtPosition( 
							graph, stopJoint.wayId,
						    graph.get(stopJoint.wayId).getClosestNode( stopJoint.lat, stopJoint.lon)
						  );
		
					boolean reacheable = false;
					if( stopJointP != null )
						reacheable = Utils.determineAReacheableEndPoint(graph, stopJointP);
									
					/* find the route between two points */			
					LinkedList<Node> path = new LinkedList<Node>();
					if( reacheable && startJointP != null && stopJointP != null )
						path = Utils.FindPath( graph, startJointP, stopJointP, SyntheticTracesGeneratorParams.max_depth );
		
					if( path.size() > 2 )
					{
						outbr.write("start " + (Double)startJointP.lat + " " + (Double)startJointP.lon + " " + 1 + " " + startJointP.wayId + " " + 0+ "\n");

						for( int  j = 1; j < path.size()-1; j++ ){
							Node joint = path.get(j);
							outbr.write(joint.id + " " + joint.lat + " " + joint.lon + " " + joint.wayId + "\n" );
						}
						r++;
						outbr.write("end " + (Double)stopJointP.lat + " " + (Double)stopJointP.lon + " " + stopJointP.wayId + " " + 0+ "\n");
						startJoint = stopJointP;
						retry = 0;
					}
					else
					{
						retry++;
					}

					if( retry > 20 )
					{

						outbr.write("start " + (Double)startJointP.lat + " " + (Double)startJointP.lon + " " + 1 + " " + startJointP.wayId + " " + 0+ "\n");
						double dist =  Utils.distance(startJoint.lat, startJoint.lon, stopJoint.lat, stopJoint.lon);
						outbr.write("skip too far " + dist + "\n");
						outbr.write("end " + (Double)stopJointP.lat + " " + (Double)stopJointP.lon + " " + stopJointP.wayId + " " + 0+ "\n");
					}

				}
				outbr.close();
		} catch (FileNotFoundException e) {
			logger.info("Impossible to find " + fname + " file ");
			e.printStackTrace();
		} catch (IOException e) {
			logger.info("I/O exception for " + fname);
			e.printStackTrace();
		}

		logger.info("Execution time: " + (System.currentTimeMillis() - startTime)/1000);
	}

}
