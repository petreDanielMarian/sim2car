package utils.tracestool.algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import utils.tracestool.Utils;
import utils.tracestool.parameters.GenericParams;
import utils.tracestool.parameters.OSM;
import utils.tracestool.parameters.TraceCorrectionParams;
import utils.tracestool.traces.Trace;
import utils.tracestool.traces.TraceNode;
import model.OSMgraph.Node;
import model.OSMgraph.Way;

public class TracesCorrection {

	/* The graph of streets */
	static TreeMap<Long, Way> graph;

	/* Vector storing index of areas located inside each rectangle */
	static Vector<Vector<Integer>> areas = new Vector<Vector<Integer>>();

	/*
	 * map limits:
	 * limits[0] -> map_lat_min
	 * limits[1] -> map_lon_min 
	 * limits[2] -> map_lat_max
	 * limits[3] -> map_lon_max 
	 */
	static double [] limits = new double[4];
	
	/** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(TracesCorrection.class.getName());
	

	/* Corrects the traces for cabs using the streets graph */
	public static void getCorrectTraceCabs( ){

		/* load the graph */
		logger.info("Starting to load the OSM graph");

		long time_start = System.currentTimeMillis();

		graph = OSMGraph.load_graph( GenericParams.mapConfig.getStreetsFilename(), 
							         GenericParams.mapConfig.getPartialGraphFilename(), limits );

		long time_stop = System.currentTimeMillis();
		logger.info(""+(time_stop - time_start)/1000 );
		logger.info( " Map's characteristics ( " + limits[0] +", "+ limits[1] +" ) " + " ( "+ limits[2] +", "+" " +  limits[3] +" )");
		logger.info("Finishing to load the OSM graph");

		areas = OSMGraph.load_areas( GenericParams.mapConfig.getIndexTableFilename(), limits );

		CorrectionAlgorithmStart( );
 
	}
	
	public static void CorrectionAlgorithmStart( ){
		FileInputStream fstream;

		try {

			String url =  GenericParams.mapConfig.getTracesListFilename();
			fstream = new FileInputStream( url );
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String line;

			while( (line = br.readLine()) != null ){
				
				StringTokenizer st = new StringTokenizer(line, " ", false);
				st.nextToken();
				String srcId = st.nextToken();
				srcId = srcId.substring(4, srcId.length() - 1);
				String name = "";
				name = srcId+".txt";

				CorrectionAlgorithm( name );

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

	public static void CorrectionAlgorithm( String fname ){

		try {

			long time_start;
			int nr_cols, nr_rows;
			FileOutputStream ostream;
			BufferedWriter outbr;
			Object [] kvalues;

			/* Determine the sizes ( columns and rows ) of the grid for current map */
			nr_cols = (int)Math.ceil( (limits[3] - limits[1] /*map_lon_max - map_lon_min*/)/OSM.SQUARE_L ) + 1;
			nr_rows = (int)Math.ceil( (limits[2] - limits[0] /*map_lat_max - map_lat_min*/)/OSM.SQUARE_L ) + 1;

			/* Trace object */
			Trace trace = Utils.newTrace( GenericParams.mapConfig.getTracesPath(), fname);
			int ret = trace.readTrace();
			if( ret < 0 ) 
			{
				logger.info( "Error " + ret + " During reading the trace " +  fname );
				return;
			}

			TraceNode crt, prev;
			logger.info( " Size of the trace nodes' vector "+ trace.nodes.size() + "for the file " +  fname );

			Vector<TraceNode> data = trace.nodes;

			time_start = System.currentTimeMillis();
			/* The candidates tree that keeps a candidates vector for each trace node */
			TreeMap<Integer, Vector<TraceNode>> candidates;
			Vector <TraceNode> ids;
			candidates = new TreeMap<Integer, Vector <TraceNode>> ();
			int d = data.size();
			int [] nr_c = new int[3];
			int [] nr_r = new int[3];
			TreeMap<Integer,TraceNode> out_cand = new TreeMap<Integer,TraceNode>();

			for( int i = 0; i < d; i++ ){

				crt = data.get(i);

				ids = new Vector<TraceNode>();
				

				
				nr_c[0] = (int) Math.floor(( (Double)crt.getX() - limits[1] /*map_lon_min*/ )/OSM.SQUARE_L);
				nr_c[1] = nr_c[0] + 1;
				nr_c[2] = nr_c[0] - 1; 


				nr_r[0] = (int)Math.floor(( (Double)crt.getY() - limits[0] /*map_lat_min*/ )/OSM.SQUARE_L);
				nr_r[1] = nr_r[0] + 1;
				nr_r[2] = nr_r[0] - 1; 

				int last = -1; 
				for( int x = 0; x < 3; x++ ){
					if( nr_r[x] < 0 || nr_r[x] > nr_rows )
						continue;
					for( int y = 0; y < 3; y++ ){
						if( nr_c[y] < 0 || nr_c[y] > nr_cols )
							continue;

						int crt_sq = nr_r[x] * nr_cols + nr_c[y]; 

						if( crt_sq != last && crt_sq < nr_cols * nr_rows ){

							last = crt_sq;
							Vector<Integer> aux = areas.get(crt_sq);
							if( aux == null )
								continue;

							/* take every street and check on which can be placed the current node */
							for( int h = 0; h < aux.size(); h++ ){
								long id = aux.get(h);
								int hz;
								/* check if the street was already added into the candidates list */
								for( hz = 0; hz < ids.size(); hz++)
									if( ids.get(hz).wid == (long)id )
										break;
								/* the current street was already added skip it */ 
								if( hz < ids.size())
									continue;


								Way w = graph.get((long)id);

								if( Utils.check_limits( crt, w.min_lat, w.min_long, w.max_lat, w.max_long ) ){

									Vector<Node> ita = w.nodes;
									Node prev_n = ita.get(0);
									Node crt_n;

									for( int j = 1; j < ita.size(); j++ ){
										crt_n = ita.get(j);

										/* check if the node is really on the street */
										double inter_min_lat = crt_n.lat < prev_n.lat ? crt_n.lat : prev_n.lat; 
										double inter_max_lat = crt_n.lat > prev_n.lat ? crt_n.lat : prev_n.lat;
										double inter_min_lg = crt_n.lon < prev_n.lon ? crt_n.lon : prev_n.lon;
										double inter_max_lg = crt_n.lon > prev_n.lon ? crt_n.lon : prev_n.lon;

										
										if( (Double)crt.getY() >= inter_min_lat && (Double)crt.getY() <= inter_max_lat && 
												(Double)crt.getX() >= inter_min_lg && (Double)crt.getX() <= inter_max_lg ){

											//TraceNode nd_ax = new TraceNode((Double)crt.getY(), (Double)crt.getX() , crt.occupied, crt.timestamp);
											TraceNode nd_ax = Utils.getProjection(crt, prev_n, crt_n );
											nd_ax.ontheStreetFirst = true;
											nd_ax.setIdStreet( w.id );
											nd_ax.setIndexNodes( j );
											if( !ids.contains(nd_ax))
												ids.add( nd_ax );
										    prev_n = crt_n;
											continue;
										}

										TraceNode nd_ax = Utils.getProjection(crt, prev_n, crt_n );
										double dist = Utils.distance( (Double)crt.getY(), (Double)crt.getX(), (Double)nd_ax.getY(), (Double)nd_ax.getX() );

										if(  dist <= TraceCorrectionParams.dist_phase3 ){
											nd_ax.setIdStreet( w.id );
											/* Keep the index for the street that has distance smaller than dist_phase3 */
											nd_ax.setIndexNodes( j );
											if( !ids.contains(nd_ax))
												ids.add( nd_ax );
											//break;
										} /* if( dist <= TraceCorrectionParams.dist_phase3 ) */

										prev_n = crt_n;
									} /* iterate over the street's nodes */
								} /* if the node is include into the street limits */

							} /* iterate over each street from the current square */
						}
					}
				}

				/* Add  only the nodes that has possible candidates */
				if( ids.size() != 0 )
					candidates.put(i, ids);
				/*else{
						out_cand.put(i, crt);
			      }*/

			}


			logger.info( " The size of candidate vector after phase 3 " + candidates.size() );
			if(TraceCorrectionParams.debug)
				Utils.showLocCand(candidates, "phase1"+fname);

			/* Phase 4 */
			/* 	Each original node of the trace is iterated again in order to removed the projection that are on the traffic direction
			 * in case of the streets with two directions. 
			 * 	This operation is done by computing the angle between the moving direction determining from trace and 
			 * the street direction. Two node have the direction if their angle is < 90 degrees.
			 * Nb: The case of the first node from the trace has to be analyzed in particular. 
			 */

			for( Iterator <Map.Entry<Integer, Vector<TraceNode>>> it = candidates.entrySet().iterator();it.hasNext(); ){
				Map.Entry<Integer, Vector<TraceNode>> aux = it.next();

				/* The vector with possible candidates nodes  */
				ids = aux.getValue();
				if( ids.size()  <= 1 ){
					continue;
				}
				int idx = aux.getKey();

				/* crt  - current node of the trace */
				if( idx == 0 && candidates.size()  > 1){
					prev = data.get( idx );
					crt = data.get( idx + 1);
				}
				else{
					prev = data.get( idx - 1);
					crt = data.get( idx );
				}
				
				/* if the node is the same as previous, it should have the same solution */
				if( ((Double)crt.getX() - (Double)prev.getX() == 0) &&  ((Double)crt.getY() - (Double)prev.getY() == 0) )
				{
					candidates.put( idx, candidates.get(idx > 0 ? idx -1 : idx));
					continue;
				}

				Vector<TraceNode> new_cand = new Vector<TraceNode>();
				for( int i = 0; i < ids.size(); i++ ){
					TraceNode prj = ids.get( i );
					Way w = graph.get( prj.wid );

					/* the street allows traffic in both directions */
					if( !w.oneway ){
						new_cand.add(prj);
						continue;
					}

					int indexNode = prj.indexNodes;
					Node crt_n = indexNode < w.nodes.size() - 1 ? w.nodes.get(indexNode): w.nodes.get(indexNode -1);
					Node next_n = indexNode < w.nodes.size() - 1 ? w.nodes.get(indexNode +1 ): w.nodes.get(indexNode);
					double angle = Utils.getAngle( (Double)crt.getY() - (Double)prev.getY(),  (Double)crt.getX() - (Double)prev.getX(),
							next_n.lat - crt_n.lat, next_n.lon - crt_n.lon );
					crt_n = w.nodes.get( indexNode - 1 );
					next_n = w.nodes.get( indexNode );
					double angle2 = Utils.getAngle(  (Double)crt.getY() - (Double)prev.getY(),  (Double)crt.getX() - (Double)prev.getX(),
							next_n.lat - crt_n.lat, next_n.lon - crt_n.lon );

					if(  angle < TraceCorrectionParams.max_angle || angle2  < TraceCorrectionParams.max_angle )
						new_cand.add(prj);
				}

				candidates.put( idx, new_cand);


			}

			if(TraceCorrectionParams.debug)
				Utils.showLocCand(candidates, "phase2"+fname);

			int nr_cand = 0;

			int iter = 0;

			do{

				nr_cand = 0;

				/* -----------------Phase 5---------------------- 
				 * During this phase will be removed the candidates projections on the close street.
				 * If a node is solved then next node should be on the same street or the crossing street with the resolved node's street.
				*/
				kvalues = candidates.entrySet().toArray();
				for( int i = 1; i < kvalues.length; i++){
					Vector<TraceNode> new_cand = new Vector<TraceNode>();
					Map.Entry<Integer,Vector<TraceNode>> prev_aux = (Map.Entry<Integer,Vector<TraceNode>>)kvalues[i-1];
					Map.Entry<Integer,Vector<TraceNode>> crt_aux = (Map.Entry<Integer,Vector<TraceNode>>)kvalues[i];
					if( prev_aux.getValue().size() > 1 )
						continue;
					if( prev_aux.getValue().size() == 0 ){
						if( i > 2 && ((Map.Entry<Integer,Vector<TraceNode>>)kvalues[i-2]).getValue().size() == 1 )
							prev_aux = (Map.Entry<Integer,Vector<TraceNode>>)kvalues[i-2];
					}
					if( prev_aux.getValue().size() == 1 ){
						Way street = graph.get( prev_aux.getValue().get(0).wid );
						for( int j = 0; j < crt_aux.getValue().size(); j++ ){
							TraceNode crtNd = crt_aux.getValue().get(j);
							Long crt_cand_Sid = crtNd.wid; 
							/* check if the both points are on the same street */
							if(  crt_cand_Sid == street.id ){
								if( !new_cand.contains(crtNd) )
										new_cand.add(crtNd);
							}
							else{

								/* check if the both points are on streets are linked by a crossing */
								for( Iterator<Map.Entry<Long,Vector<Long>>> itx = street.neighs.entrySet().iterator(); itx.hasNext(); ){
									Map.Entry< Long, Vector<Long>> ax = itx.next();
									/* if no crossing node is not common then skip it */
									if( graph.get(crt_cand_Sid).nodes.contains(new Node(ax.getKey(), -1, -1 )) == false )
										continue;
									if( ax.getValue().contains( crt_cand_Sid ) ){
										if( new_cand.contains( crtNd ) == false )
											new_cand.add( crtNd );
									}

								}
							}

						}
					}
					if( new_cand.size() != 0){
						candidates.put( crt_aux.getKey(), new_cand );
						/*--------------------------*/
						crt_aux.setValue(new_cand);
						kvalues[i] = crt_aux;
						/*--------------------------*/
					}

				}

				/* Phase 6 
				 * During this phase the nodes that are on the same street and there are nodes between them that are not the same street will
				 * be placed on it.
				 */
				kvalues = candidates.entrySet().toArray();
				int start_poz = -1;
				int skip = 0, bkp_poz = 0;

				for( int i = 1; i < kvalues.length; i++){
					Map.Entry<Integer,Vector<TraceNode>> crt_aux = (Map.Entry<Integer,Vector<TraceNode>>)kvalues[i];
					if( start_poz == -1 ){
						if( crt_aux.getValue().size() == 1 ){
							start_poz = i;
							skip = 0;
							continue;
						}
					}
					else{
						if( i - start_poz > 5 ){
							i = start_poz + 1;
							bkp_poz = i;
							start_poz = -1;
							skip = 0;
							continue;
						}

						if( crt_aux.getValue().size() == 1 ){
							Map.Entry<Integer,Vector<TraceNode>> prev_aux = (Map.Entry<Integer,Vector<TraceNode>>)kvalues[start_poz];
							long c_street_id = crt_aux.getValue().get(0).wid;
							long p_street_id = prev_aux.getValue().get(0).wid ;

							if(  c_street_id == p_street_id ){

								/* Now, all nodes should be on the same street.
								 * If a good candidate can not be found, then the projection on that street is calculated */

								for( int j = start_poz + 1; j < i; j++ ){
									Map.Entry<Integer,Vector<TraceNode>> aux = (Map.Entry<Integer,Vector<TraceNode>>)kvalues[j];
									Vector <TraceNode>cand = aux.getValue();
									int h;
									double dist = Integer.MAX_VALUE;
									int idx = -1;
									TraceNode orig = data.get(aux.getKey());
									for( h = 0; h < cand.size(); h++ ){
										TraceNode c = cand.get(h);
										if( c.wid == c_street_id ){
											Vector<TraceNode> new_c = new Vector<TraceNode>();
											new_c.add(c);
											candidates.put( aux.getKey(), new_c );
											crt_aux.setValue(new_c);
											kvalues[i] = crt_aux;
											break;
										}
										/*
										 else{
												// this case take into consideration the nearest to the as being the best choice
												double aux_dist = distance( orig.lat, orig.lon, c.lat, c.lon);
												if( aux_dist < dist ){
													idx = h;
													dist = aux_dist;
												}
											}
										*/
									}

									/* Remove the candidates that are 20 meters away from the original node */
									if( h == cand.size() ){

										Vector<TraceNode> new_c = new Vector<TraceNode>();
										Way w = graph.get(c_street_id);
										Vector<Node> ita = w.nodes;
										Node prev_n = ita.get(0);
										Node crt_n;
										TraceNode c_aux = data.get(aux.getKey());
										for( int g = 1; g < ita.size(); g++ ){
											crt_n = ita.get(g);
											TraceNode nd_ax = Utils.getProjection( c_aux, prev_n, crt_n );
											if( Utils.distance( (double)c_aux.getY(), (double)c_aux.getX(), (double)nd_ax.getY(), (double)nd_ax.getX() )  <= 20){
												nd_ax.setIdStreet( w.id );
												/* Keep the node index for the one that its projection is closer than 20 meters to original node */
												nd_ax.setIndexNodes( j );
												if( !new_c.contains(nd_ax) )
													new_c.add( nd_ax );
												break;
											}
										}

										if( new_c.size() != 0 ){
											candidates.put( aux.getKey(), new_c );
											crt_aux.setValue(new_c);
											kvalues[i] = crt_aux;
										}
									}
								}
							}
							else{
								if( skip ==  0){
									bkp_poz = i;
								}
								skip++;
								if( skip > 2 ){
									start_poz = -1;
									i = bkp_poz;
								}
							}
						}
						else{
							if( skip ==  0){
								bkp_poz = i;
							}
							skip++;
							if( skip > 2 ){
								start_poz = -1;
								i = bkp_poz;
							}
						}
					}
				}

				kvalues = candidates.entrySet().toArray();
				for( int i = 1; i < kvalues.length; i++){
					Map.Entry<Integer,Vector<TraceNode>> crt_aux = (Map.Entry<Integer,Vector<TraceNode>>)kvalues[i];
					if( crt_aux.getValue().size() > 1 )
						nr_cand++;
					else{
						if( crt_aux.getValue().size() == 1 )
							data.set(crt_aux.getKey(), crt_aux.getValue().get(0));
					}
				}
				
				if( iter == 0)
				{
					/* remove all the candidates that are away with 20 meters from original data */
					kvalues = candidates.entrySet().toArray();
					for( int i = 0; i < kvalues.length; i++){
						Map.Entry<Integer,Vector<TraceNode>> crt_aux = (Map.Entry<Integer,Vector<TraceNode>>)kvalues[i];
						TraceNode orig = data.get(crt_aux.getKey());
						Vector<TraceNode> new_cand_aux = new Vector<TraceNode>();
						Vector<TraceNode> cand_aux = crt_aux.getValue();
						for( int  h = 0; h < crt_aux.getValue().size(); h++ ){
							TraceNode c = cand_aux.get(h);
	
							double aux_dist = Utils.distance( (Double)orig.getY(), (Double)orig.getX(), (Double)c.getY(), (Double)c.getX());
							/* keep only the points that are at only 20 meter away */
							if( aux_dist <= 20 ){
								if( !new_cand_aux.contains(c) )
									new_cand_aux.add(c);
							}
						}
						candidates.put(crt_aux.getKey(), new_cand_aux);
					}
				}
				iter++;

				if(TraceCorrectionParams.debug)
					Utils.showLocCand(candidates, "phase6"+iter+fname);
			}
			while( nr_cand > TraceCorrectionParams.max_cand_nr &&  iter < TraceCorrectionParams.max_iter );

			/* For the remaining unresolved nodes, the nearest node is chose */

			kvalues = candidates.entrySet().toArray();

			for( int j = 0; j < kvalues.length; j++ ){
				Map.Entry<Integer,Vector<TraceNode>> p_aux = null , n_aux = null , aux = null ;
				if( j > 1 )
					p_aux = (Map.Entry<Integer,Vector<TraceNode>>)kvalues[j - 1];
				if( j < kvalues.length - 1 )
					n_aux = (Map.Entry<Integer,Vector<TraceNode>>)kvalues[j + 1];

				aux = (Map.Entry<Integer,Vector<TraceNode>>)kvalues[j];
				Vector <TraceNode>cand = aux.getValue();
				if( cand.size() == 0)
					continue;

				if( j > 1 && j < kvalues.length - 1 ){

					if( p_aux.getValue().size() == 1 && n_aux.getValue().size() == 1 ){
						if( p_aux.getValue().get(0).wid == n_aux.getValue().get(0).wid ){
							Vector<TraceNode> new_c = new Vector<TraceNode>();
							Way w = graph.get(p_aux.getValue().get(0).wid);
							Vector<Node> ita = w.nodes;
							Node prev_n = ita.get(0);
							Node crt_n;

							TraceNode c_aux = data.get(aux.getKey());
							for( int g = 1; g < ita.size(); g++ ){

								crt_n = ita.get(g);
								TraceNode nd_ax = Utils.getProjection( c_aux, prev_n, crt_n );
								if( Utils.distance( (Double)c_aux.getY(), (Double)c_aux.getX(), (Double)nd_ax.getY(), (Double)nd_ax.getX() )  <= 15){
									nd_ax.setIdStreet( w.id );

									nd_ax.setIndexNodes( j );
									new_c.add( nd_ax );
									break;
								}

							}

							if( new_c.size() != 0 ){
								candidates.put(aux.getKey(), new_c);
								continue;
							}
						}
					}
				}
				if( cand.size() <= 1)
					continue;
				double dist = Double.MAX_VALUE;
				int idx = -1;
				TraceNode orig = data.get(aux.getKey());
				for( int  h = 0; h < cand.size(); h++ ){
					TraceNode c = cand.get(h);

					double aux_dist = Utils.distance( (Double)orig.getY(), (Double)orig.getX(), (Double)c.getY(), (Double)c.getX());
					if( aux_dist - dist <= 0 ){
						idx = h;
						dist = aux_dist;
					}

				}
				if( idx != -1 ){
					Vector<TraceNode> new_c = new Vector<TraceNode>();
					new_c.add(cand.get(idx));
					candidates.put( aux.getKey(), new_c );
				}
			}
			ostream = new FileOutputStream( GenericParams.mapConfig.getCorrectionAlgResults() + fname  );
			outbr = new BufferedWriter(new OutputStreamWriter(ostream));
			kvalues = candidates.entrySet().toArray();
			
			for( int i = 0; i <= kvalues.length - 1; i++ ){
				Map.Entry<Integer, Vector<TraceNode>> aux = (Map.Entry<Integer, Vector<TraceNode>>)kvalues[i];
				if(  aux.getValue().size() == 1 ){
					TraceNode rez = aux.getValue().get(0);
					/* latitude longitude cab_state timestamp street_id */
					outbr.write( (Double)rez.getY() + " " + (Double)rez.getX() + " " + rez.occupied + " " + rez.timestamp + " " + rez.wid + "\n" );
				}
			}
			
			outbr.close();
			logger.warning( "The algorithm's time: " + ((System.currentTimeMillis()- time_start)/1000) );

		} catch (FileNotFoundException e) {
			logger.info("Impossibe to open the file");
			e.printStackTrace();
		} catch (IOException e) {
			logger.info("Impossible to perfom IO operation on the file");
			e.printStackTrace();
		}
	}

}
