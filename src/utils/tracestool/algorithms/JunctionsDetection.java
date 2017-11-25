package utils.tracestool.algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import utils.tracestool.Utils;
import utils.tracestool.parameters.GenericParams;
import utils.tracestool.parameters.TraceJunctionsDetectionParams;
import utils.tracestool.traces.Trace;
import utils.tracestool.traces.TraceNode;
import model.OSMgraph.Node;
import model.OSMgraph.Way;

public class JunctionsDetection {
	 /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(JunctionsDetection.class.getName());

	/* The graph of streets */
	static TreeMap<Long, Way> graph;

	/*
	 * map limits:
	 * limits[0] -> map_lat_min
	 * limits[1] -> map_lon_min 
	 * limits[2] -> map_lat_max
	 * limits[3] -> map_lon_max 
	 */
	static double [] limits = new double[4];

	/* Determine the joints from traces */
	public static void getJunctionsDetection(){

		/* load the graph */
		logger.info("Starting to load the OSM graph");

		long time_start = System.currentTimeMillis();

		graph = OSMGraph.load_graph( GenericParams.mapConfig.getStreetsFilename(), 
		         					 GenericParams.mapConfig.getPartialGraphFilename(), limits );

		
		long time_stop = System.currentTimeMillis();

		logger.info(""+ (time_stop - time_start)/1000 );
		logger.info( "Map's characteristics (" + limits[0] +", "+ limits[1] +") " + " ("+ limits[2] +", "+" " +  limits[3] +" )" );
		logger.info( "Finishing to load the OSM graph" );

		startJunctionsDetection();

	}
	
	public static void startJunctionsDetection( ){
		FileInputStream fstream;

		try {

			fstream = new FileInputStream( GenericParams.mapConfig.getTracesListFilename() );
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String line;

			while( (line = br.readLine()) != null ){
				
				StringTokenizer st = new StringTokenizer(line, " ", false);
				st.nextToken();
				String srcId = st.nextToken();
				srcId = srcId.substring(4, srcId.length() - 1);
				String name = "";
				name = srcId+".txt";

				TraceLayoutAlgorithm( name );

			}

			br.close();

		} catch (FileNotFoundException e) {
			logger.severe( " Error opening file with cabs.txt ");
			e.printStackTrace();
		} catch (IOException e) {
			logger.severe( "Error making operations on file cabs.txt");
			e.printStackTrace();
		}
	}
	

	/* For each trace function keeps only the junctions */
	public static void TraceLayoutAlgorithm( String fname ){
		
		/* Trace object */
		Trace trace = null;
		if( TraceJunctionsDetectionParams.useCorrectedTrace )
			trace = Utils.newTrace( GenericParams.mapConfig.getCorrectionAlgResults(), fname);
		else
			trace = Utils.newTrace( GenericParams.mapConfig.getInterpolateAlgResults(), fname);
		int ret = trace.readProcessedTrace();
		if( ret < 0 ) 
		{
			logger.info( "Error " + ret + " During reading the trace " +  fname );
			return;
		}

		Vector<TraceNode> data = trace.nodes;

		if( data == null ){
			logger.info("Error: unable to read the trace data" + fname );
			return;
		}

		TraceNode crt, prev;

		logger.info( "The points' number of the trace "+ data.size() + " for " +  fname );
		Long startTime = System.currentTimeMillis();
		prev = data.get(0);

		try {
			FileOutputStream ostream = new FileOutputStream( GenericParams.mapConfig.getResultsJunctionsDetectionAlgPath() + "joints_"+fname );
			BufferedWriter outbr = new BufferedWriter(new OutputStreamWriter(ostream));

			FileOutputStream fosSpeeds;
			BufferedWriter bwSpeeds = null;
			if( TraceJunctionsDetectionParams.speedDetectionOn )
			{
				fosSpeeds = new FileOutputStream( GenericParams.mapConfig.getResultsJunctionsDetectionAlgPath() + "speeds_"+fname );
				bwSpeeds = new BufferedWriter(new OutputStreamWriter(fosSpeeds));
			}
			
			outbr.write("start " + (Double)prev.getY() + " " + (Double)prev.getX() + " " + prev.occupied + " " + prev.wid + " " + prev.timestamp + "\n");
		
			for( int i = 1; i < data.size(); i++ ){
				crt = data.get(i);
				
				if (prev.occupied != crt.occupied) {
					outbr.write("end " + (Double)prev.getY() + " " + (Double)prev.getX() + " " + prev.wid + " " + prev.timestamp + "\n");
					outbr.write("start " + (Double)crt.getY() + " " + (Double)crt.getX() + " " + crt.occupied + " " + crt.wid + " " + crt.timestamp + "\n");
					prev = crt;
					continue;
				}
				
				if (prev.wid == crt.wid) {
					prev = crt;
					if( TraceJunctionsDetectionParams.speedDetectionOn )
					{
						double speed = Utils.distance((Double)prev.getY(), (Double)prev.getX(), (Double)crt.getY(), (Double)crt.getX()) / (crt.timestamp - prev.timestamp);
						if (0.0 < speed && speed <= TraceJunctionsDetectionParams.maxSpeed){
							bwSpeeds.write(prev.wid + " " + speed + "\n");
						}
					}
					continue;
				}
				else {
					Node joint = OSMGraph.getIntersectNode( prev.wid, crt.wid, graph );
					
					if( joint != null ){
						outbr.write(joint.id + " " + joint.lat + " " + joint.lon + " " + joint.wayId + "\n" );
						if( TraceJunctionsDetectionParams.speedDetectionOn )
						{
							double dist = Utils.distance((Double)prev.getY(), (Double)prev.getX(), joint.lat, joint.lon) + Utils.distance(joint.lat, joint.lon, (Double)crt.getY(), (Double)crt.getX());
							double speed = dist / (crt.timestamp - prev.timestamp);
							if (0.0 < speed && speed <= TraceJunctionsDetectionParams.maxSpeed) {
								bwSpeeds.write(prev.wid + " " + speed + "\n");
								bwSpeeds.write(crt.wid + " " + speed + "\n");
							}
						}
					}
					else{
						double dist = Utils.distance((Double)prev.getY(), (Double)prev.getX(), (Double)crt.getY(), (Double)crt.getX() );
						if( dist < TraceJunctionsDetectionParams.max_dist ){

							Node startJoint = Utils.getClosestJointFromCrtPosition( 
																					graph, prev.wid,
																				    graph.get(prev.wid).getClosestNode( (Double)prev.getY(), (Double)prev.getX())
																				  );

							Node stopJoint = Utils.getClosestJointFromCrtPosition( 
																					graph, crt.wid, 
																				    graph.get(crt.wid).getClosestNode( (Double)crt.getY(), (Double)crt.getX())
																				  );
							boolean reacheable = false;
							if( stopJoint != null )
								reacheable = Utils.determineAReacheableEndPoint(graph, stopJoint);
							
							/* find the route between two points */			
							LinkedList<Node> path = new LinkedList<Node>();
							if( reacheable && startJoint != null && stopJoint != null )
								path = Utils.FindPath( graph, startJoint, stopJoint, TraceJunctionsDetectionParams.max_depth );

							if (path.size() == 0) {
								outbr.write("end " + (Double)prev.getY() + " " + (Double)prev.getX() + " " + prev.wid + " " + prev.timestamp + "\n");
								outbr.write("start " + (Double)crt.getY() + " " + (Double)crt.getX() + " " + crt.occupied + " " + crt.wid + " " + crt.timestamp + "\n");
							} else {
								double distanceNodes = 0.0;
								Node firstNode = null;
								Node lastNode = null;
								Node prevNode = null;
								for( int  j = 0; j < path.size(); j++ ){
									joint = path.get(j);
									outbr.write(joint.id + " " + joint.lat + " " + joint.lon + " " + joint.wayId + "\n" );

									if( TraceJunctionsDetectionParams.speedDetectionOn )
									{
										if (prevNode != null)
											distanceNodes += Utils.distance(prevNode.lat, prevNode.lon, joint.lat, joint.lon);
										if (j == 0)
											firstNode = joint;
										if (j == path.size() - 2)
											lastNode = joint;
										prevNode = joint;
									}
								}
								if( TraceJunctionsDetectionParams.speedDetectionOn )
								{
									distanceNodes += Utils.distance((Double)prev.getY(), (Double)prev.getX(), firstNode.lat, firstNode.lon);
									distanceNodes += Utils.distance(lastNode.lat, lastNode.lon, (Double)crt.getY(), (Double)crt.getX());
									double speed = distanceNodes / (crt.timestamp - prev.timestamp);
									if (0.0 < speed && speed < TraceJunctionsDetectionParams.maxSpeed) {
										/* detect the way between two joint points */
										for( int  j = 0; j < path.size() - 1; j++ ){
											joint = path.get(j);
											bwSpeeds.write(joint.wayId + " " + speed + "\n");
										}
									}
								}
								prev = new TraceNode(stopJoint.lat, stopJoint.lon );
							}
						} else {
							outbr.write("skip too far " + dist + "\n");
							outbr.write("end " + (Double)prev.getY() + " " + (Double)prev.getX() + " " + prev.wid + " " + prev.timestamp + "\n");
							outbr.write("start " + (Double)crt.getY() + " " + (Double)crt.getX() + " " + crt.occupied + " " + crt.wid + " " + crt.timestamp + "\n");
						}
					}
					
					prev = crt;
				}
			}
			outbr.write("end " + (Double)prev.getY() + " " + (Double)prev.getX() + " " + prev.wid + " " + prev.timestamp + "\n");
			outbr.close();
			if( TraceJunctionsDetectionParams.speedDetectionOn )
			{
				bwSpeeds.close();
			}
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
