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
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import utils.tracestool.Utils;
import utils.tracestool.parameters.GenericParams;
import utils.tracestool.parameters.TraceInterpolateParams;
import utils.tracestool.traces.Trace;
import utils.tracestool.traces.TraceNode;
import model.OSMgraph.Node;
import model.OSMgraph.Way;

public class TracesInterpolation {
	
	/* Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(TracesInterpolation.class.getName());
	
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

    
	/**
	 *  Interpolate algorithm for a trace
	 * @param fname - file name
	 */
	public static void InterpolAlgorithm( String fname ){

		long start_t = System.currentTimeMillis();

		/* Trace object */
		Trace trace = Utils.newTrace( GenericParams.mapConfig.getCorrectionAlgResults(), fname);
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

		LinkedList<TraceNode> new_data = new LinkedList<TraceNode>();
		/* minimum timestamp for all traces in order to aligned all of them according to it*/
		long first_time = GenericParams.mapConfig.getInterpolateStartTime();
		/* interpolate interval */
		long DeltaT = GenericParams.mapConfig.getInterpolateInterval();

		/* get the start time for current trace */
		long crt_time_trace  = getTraceStartTime( DeltaT, data.get(0).timestamp, first_time );

		first_time = crt_time_trace;
	
		logger.info( "The points' number of the trace "+ data.size() + " for " +  fname );

		/* 	Faza 1 
		 * 		Se ia fiecare nod in parte si in functie de nodul precedent si nodul urmator se iau urmatorele decizii:
		 * 		1. Daca nodul urmator este la un interval > 3* deltaT decat nodul curent se ignora manifestandu-se fenomenul de tunel,
		 * 		in care se considera ca gps nu a putut transmite date in acest interval.
		 * 		2. Daca totusi diffTimp( next_nod; crt_nod ) <= 3 atunci adauga noi noduri in acest interval astfel incat sa fie la o
		 * 		distanta corespunzatoare cu intervalul de timp.
		 * 			Primul nod din trace se considera la intervalul de timp 0, apoi 60, 120, totul facandu-se un fel de offset fata 
		 * 		de cea mai de timp masuratoare a acestor puncte. Dupa ce au fost analizate toate fisierele se determina
		 * 		si intervalul de timp cel mai devreme si carui fisier corespunde pentru a se face o aliniere totala a trace-ului astfel 
		 *		redarea de catre engine de simulare sa o facut cu cozi de eventuri. 
		 **/

		/**
		 * Phase 1
		 * 
		 */

		//logger.info(" Primul timp " + first_time + "   timp orig " + crt_first_tm +" Timpul  nou calc " + crt_time_trace);
		crt = data.get(0);
		prev = crt;

		//logger.info( "time before modification " + prev.timestamp );
		TraceNode new_l;
		new_l = new TraceNode((Double)crt.getY(), (Double)crt.getX(), crt.occupied, crt.timestamp);
		new_l.setIdStreet(crt.wid);

		new_l.timestamp = crt_time_trace;

		new_data.add( new_l );

		for( int i = 1; i < data.size(); i++ ){
			crt = data.get(i);
			new_l =  new TraceNode((Double)crt.getY(), (Double)crt.getX(), crt.occupied, crt.timestamp);
			new_l.setIdStreet(crt.wid);
			long diff = crt.timestamp - prev.timestamp;
			//logger.info("Diferenta de timp este  "+ diff );
			 if( diff <= DeltaT * 0.1  ){
				prev = crt;
				continue;
			}
			/* Orice este peste deltaT incerc sa il calibrez la distantare de multiplu de 
			 * deltaT. Doar in cazul celor care nu sar peste 3*DeltaT.
			 */

			if( diff >  DeltaT ){
				long rem = diff%DeltaT;
				if( rem == 0 || rem < DeltaT/2 || diff/DeltaT == 2  )
					crt_time_trace += DeltaT * ((int) diff/DeltaT);
				else
					if( diff/DeltaT >= TraceInterpolateParams.max_limit || diff/DeltaT == 1 )
						crt_time_trace += DeltaT * ((int) diff/DeltaT + 1);


				new_l.timestamp = crt_time_trace;
			}

			if( /*DeltaT/4 < diff && */diff <= DeltaT ){
				crt_time_trace += DeltaT;
				new_l.timestamp =  crt_time_trace;
			}

			new_data.add(new_l);

			prev = crt;

		}
		
		System.out.println("Time aligment phase is done");

		/* Clear original data in order to reuse the vector data */
		data.clear();

		prev = new_data.get(0);
		data.add( prev );

		crt_time_trace = first_time;

		for( int i = 1; i < new_data.size();i++){
			crt = new_data.get(i);
			long diff = crt.timestamp  - prev.timestamp;
			if( diff == DeltaT || diff >= TraceInterpolateParams.max_limit* DeltaT ){
				/* 
				 * If the difference between 2 nodes is DeltaT then the node can be kept
				 * else if diff >= max_limit * DeltaT then its solution is postponed. 
				 */

				crt_time_trace += diff;
				new_l =  new TraceNode((Double)crt.getY(), (Double)crt.getX(), crt.occupied, crt_time_trace );
				new_l.setIdStreet(crt.wid);
				data.add( new_l );

				prev = crt;
				continue;
			}
			else{
				/* the node is in the accepted range then solve it */
				if( diff > DeltaT && diff < TraceInterpolateParams.max_limit * DeltaT ){
					/* Determine the intermediare nodes' number to generate */
					long pointsNb = diff/DeltaT;
					/* If both nodes are on the same street */
					if( prev.wid == crt.wid ){

						double step_Lat = ( (Double)crt.getY() - (Double)prev.getY() )/pointsNb;
						double step_Lon = ( (Double)crt.getX() - (Double)prev.getX() )/pointsNb;

						Vector<Node> nds = graph.get(prev.wid).getNodesFromA2B(prev, crt);
						int dim_Nds = nds.size();
						int step = dim_Nds/(int)(pointsNb+1);

						//logger.info( " Numar punct "+ nds.size() +" dimNds " + dim_Nds + "pas " + step +" Nr pct " + nr_pct );
						for( int j = 0; j < pointsNb; j++ ){
							crt_time_trace += DeltaT;
							double p_lat = (Double)prev.getY();
							double p_lon = (Double)prev.getX();
							if( dim_Nds <= 2 || step == 0)
								new_l = new TraceNode( p_lat + (j + 1)* step_Lat, p_lon + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );
							else{
								new_l = new TraceNode( p_lat + (j + 1)* step_Lat, p_lon + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );
								new_l = Utils.getProjection( new_l, nds.get(j * step), nds.get( ( j + 1 )*step ) );
								new_l.timestamp = crt_time_trace;
							}
							new_l.setIdStreet(prev.wid);
							data.add( new_l );
						}


					}
					else{
						/* Check if both nodes are on neighbours' street */

						/* Get the Junction's node */
						Node join_nd = OSMGraph.getIntersectNode( prev.wid, crt.wid, graph );
						if( join_nd != null ){
							double p_lat = (Double)prev.getY();
							double p_lon = (Double)prev.getX();
							double c_lat = (Double)crt.getY();
							double c_lon = (Double)crt.getX();

							//logger.info("Junction points");
							double dist_before_junction = Utils.distance(p_lat, p_lon, join_nd.lat, join_nd.lon);
							double dist_after_junction = Utils.distance(c_lat, c_lon, join_nd.lat, join_nd.lon);
							int odd = 0;

							/*
							 *  If the number of points neccessary to be generated is odd 
							 *  then the middle point is in the intersection.
							 */

							if( pointsNb % 2 == 1){
								odd = 1;
								pointsNb--;
							}

							int ctr = 1;

							/* if only one point is needed to be generated then it is the junction point */
							if( pointsNb == 0){

								crt_time_trace += DeltaT;
								new_l = new TraceNode( join_nd.lat, join_nd.lon, prev.occupied, crt_time_trace );
								data.add( new_l );

							}
							if( pointsNb != 0 && pointsNb % 2 == 0 ){
								long beforePointsNb, afterPointNb;
								/* if distance before junction is longer than after */
								if( dist_before_junction > dist_after_junction ){
									long raport = (long)(dist_before_junction/dist_after_junction);
									if( raport == 1 ){
										beforePointsNb = afterPointNb = pointsNb/2;
									}
									else{
										long allocatedPoints = (raport - 1) % pointsNb;
										beforePointsNb = allocatedPoints + ( pointsNb - allocatedPoints ) /2; 
										afterPointNb = ( pointsNb - allocatedPoints ) % 2 == 0 ? ( pointsNb - allocatedPoints ) : 
											( pointsNb - allocatedPoints ) + 1;
									}
								}
								else{
									long raport = (long)(dist_after_junction/dist_before_junction);
									if( raport == 1 ){
										beforePointsNb =	afterPointNb = pointsNb/2;
									}
									else{
										long allocatedPoints = (raport - 1) % pointsNb;
										afterPointNb =   allocatedPoints + ( pointsNb - allocatedPoints ) /2; 
										beforePointsNb = ( pointsNb - allocatedPoints ) % 2 == 0 ? ( pointsNb - allocatedPoints ) : 
											( pointsNb - allocatedPoints ) + 1;
									}
								}


								double step_Lat = ( join_nd.lat - (Double)prev.getY() )/ beforePointsNb;
								double step_Lon = ( join_nd.lon - (Double)prev.getX() )/beforePointsNb;

								Vector<Node> ndsBef = graph.get(prev.wid).getNodesFromA2B(prev, join_nd);
								int dim_Nds_Before = ndsBef.size();

								int step = dim_Nds_Before/(int)(beforePointsNb+1);		
								for( int j = 0; j < beforePointsNb; j++ ){
									crt_time_trace += DeltaT;
									//new_l = new LocationParse( prev.lat + (j + 1)* step_Lat, prev.lon + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );

									if( dim_Nds_Before <= 0 || step == 0)
										new_l = new TraceNode( p_lat + (j + 1)* step_Lat, p_lon + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );
									else{
										new_l =  new TraceNode( p_lat + (j + 1)* step_Lat, p_lon + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );
										new_l = Utils.getProjection( new_l, ndsBef.get(j * step), ndsBef.get( ( j + 1 )*step - 1) );
										new_l.timestamp = crt_time_trace;
									}
									new_l.setIdStreet(prev.wid);
									data.add( new_l );
									ctr++;
									if( odd == 1&& ctr  == (pointsNb+1)/2 ){
										crt_time_trace += DeltaT;
										new_l = new TraceNode( join_nd.lat, join_nd.lon, prev.occupied, crt_time_trace );
										data.add( new_l );
									}
								}
								
								Vector<Node> ndsAfter = graph.get(crt.wid).getNodesFromA2B(join_nd, crt);
								int dim_Nds_After = ndsBef.size();
								step_Lat = ( c_lat - join_nd.lat )/afterPointNb;
								step_Lon = ( c_lon - join_nd.lon )/afterPointNb;
								step = dim_Nds_After - 2 /(int)(afterPointNb+1);	
								for( int j = 0; j < afterPointNb; j++){
									crt_time_trace += DeltaT;
									//new_l = new LocationParse( join_nd.lat + (j + 1)* step_Lat, join_nd.longit + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );
									if( dim_Nds_After <= 0 || step == 0 )
										new_l =  new TraceNode( join_nd.lat + (j + 1)* step_Lat, join_nd.lon + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );
									else{
										new_l =  new TraceNode( join_nd.lat + (j + 1)* step_Lat, join_nd.lon + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );
										new_l = Utils.getProjection( new_l, ndsAfter.get(j * step), ndsAfter.get( ( j + 1 )*step ) );
										new_l.timestamp = crt_time_trace;
									}
									new_l.setIdStreet(crt.wid);
									data.add( new_l );
									ctr++;
									if( odd == 1 && ctr  == (pointsNb+1)/2 ){
										crt_time_trace += DeltaT;
										new_l =  new TraceNode( join_nd.lat, join_nd.lon, prev.occupied, crt_time_trace );
										data.add( new_l );
									}
								}
							}


						}
						else{

							Node startJoint = Utils.getClosestJointFromCrtPosition( 
																					graph, prev.wid, 
																				    graph.get(prev.wid).getClosestNode( (Double)prev.getY(), (Double)prev.getX())
																				  );

							Node stopJoint = Utils.getClosestJointFromCrtPosition( 
																					graph, crt.wid, 
																				    graph.get(crt.wid).getClosestNode( (Double)crt.getY(), (Double)crt.getX())
																				  );
							if( startJoint != null && stopJoint != null )
							{
								/* Determine a path between prev and crt node */
								LinkedList<Node> path = Utils.FindPath( graph, startJoint, stopJoint, TraceInterpolateParams.max_depth);
	
								if( path.size() != 0 ){
	
									/* iterate over each intersection node  */
									for( int hj = 1; hj < path.size(); hj++ ){
										Node join_node = path.get(hj);
										crt_time_trace += DeltaT;
										new_l = new TraceNode( join_node.lat, join_node.lon, prev.occupied, crt_time_trace );
										new_l.setIdStreet(join_node.wayId);
										data.add( new_l );
									}
	
								}

							}
						}
					}

					prev = crt;
					crt_time_trace += DeltaT;
					new_l = new TraceNode( (Double)crt.getY() , (Double)crt.getX(), crt.occupied, crt_time_trace );
					new_l.setIdStreet(crt.wid);
					data.add( new_l );


				} /* if( diff > DeltaT && diff < max_limit * DeltaT ) */

			}		

		}

		try {
			FileOutputStream ostream;
			ostream = new FileOutputStream( GenericParams.mapConfig.getInterpolateAlgResults() + fname );

			BufferedWriter outbr = new BufferedWriter(new OutputStreamWriter(ostream));
			int routeNmb = 0;
			int cabActive = 0;
			for( int i = 0; i < data.size(); i++ ){

				if( TraceInterpolateParams.busy_on && TraceInterpolateParams.taxi_logic_on ){
					if( routeNmb == 0 ){
						if( cabActive == 1 ){
							routeNmb = new Random().nextInt(300)+ 5;
							cabActive = 0;
						}
						else{
							routeNmb = new Random().nextInt(100)+ 5;
							cabActive = 1;
						}
					}
				}

				TraceNode l = data.get(i);
				long d = TraceInterpolateParams.busy_on ? cabActive : l.occupied;
				routeNmb--;
				outbr.write( (Double)l.getY() +" "+(Double)l.getX() +" " + d +" "+ l.timestamp+ " " + l.wid +"\n" );
			}
			outbr.close();

			logger.warning( "Interpolate algorithm time " + fname +" "+(System.currentTimeMillis() - start_t)/1000 );
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	/**
	 * Return the startTime for the current Trace
	 * @param DeltaT - the time interval for discretisation.
	 * @param origStartTime - the start time for the current trace
	 * @param referenceStartTime - the absolute start time for the current set of traces
	 **/
	static long getTraceStartTime( long DeltaT, long origStartTime, long referenceStartTime )
	{

		/* determine the start time of the current trace */
		long diff = 0, rem = ( origStartTime - referenceStartTime ) % DeltaT;
		if(  rem == 0 || rem <= DeltaT/2){
			diff = ( origStartTime - referenceStartTime ) / DeltaT; 
		}
		else{
			if( rem > DeltaT/2 )
				diff = (origStartTime - referenceStartTime ) / DeltaT + 1;
		}

		return  (referenceStartTime + diff * DeltaT);  
	}

	/* Functie care interpoleaza punctele dintr-un trace tinand cont de trace-uri.
	 * Acesta o sa opereze in 2 moduri:
	 * a. primul mod este cel in care se interpoleaza un set de date dintr-un
	 * trace in care nu se cunoaste pe ce strada se afla un punct.
	 * b. al doilea mod este cel in care se interpoleaza un set date dintr-un
	 * trace prin care a fost trecut prin algorimult de corectie.
	 */
	public static void getInterpolTraceCabs( ){

		/* load the graph */
		logger.info("Starting to load the OSM graph");

		long time_start = System.currentTimeMillis();

		graph = OSMGraph.load_graph( GenericParams.mapConfig.getStreetsFilename(), 
		         					 GenericParams.mapConfig.getPartialGraphFilename(), limits );

		long time_stop = System.currentTimeMillis();

		logger.info(""+ (time_stop - time_start)/1000 );
		logger.info( " Map's characteristics (" + limits[0] +", "+ limits[1] +") " + " ("+ limits[2] +", "+" " +  limits[3] +" )");
		logger.info("Finishing to load the OSM graph");

		InterpolAlgorithmStart( );
	}

	public static void InterpolAlgorithmStart( ){
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
				name = srcId + ".txt";

				InterpolAlgorithm( name );

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
	
}
