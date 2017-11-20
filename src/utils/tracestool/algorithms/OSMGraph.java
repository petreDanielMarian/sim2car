package utils.tracestool.algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import model.OSMgraph.Node;
import model.OSMgraph.Way;
import utils.tracestool.Utils;
import utils.tracestool.parameters.GenericParams;
import utils.tracestool.parameters.OSM;

public class OSMGraph {

	/* Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(OSMGraph.class.getName());

	/*  Returns the streets from osm xml, depending on the way-id tag.
	 *  Initially, count way id. mod - 0 - raw file ; 1 - modified file.
	 */
    void getWays( int mod ){
		FileInputStream fstream;
		FileOutputStream ostream;
		String line;

		Vector<String> keyWords = new Vector<String>( Arrays.asList(OSM.streets_type) );
		Vector<String> restricted_keyWords = new Vector<String>( Arrays.asList(OSM.restricted_streets_type) );

		try {
			fstream = new FileInputStream(GenericParams.mapConfig.getMapFilename());

			ostream = new FileOutputStream( GenericParams.mapConfig.getPartialStreetsFilename() );

			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			BufferedWriter outbr = new BufferedWriter(new OutputStreamWriter(ostream));

			int ok = 0;


			while( ( line = br.readLine()) != null ){

				if( line.contains("<way id")){
					if( mod == 0 ){
						logger.info(line);
						logger.info("Found");
						outbr.write(line +"\n");
						while( !line.contains("</way>")){
							line = br.readLine();
							if( line == null )
								break;
							outbr.write(line + "\n");
						}
						if(line == null )
							break;
					}
					else {
						StringTokenizer st = new StringTokenizer(line, " ");
						st.nextToken();
						String id = st.nextToken();
						id = id.substring(4, id.length() - 1);
						String way_text = "";
						ok = 0;
						way_text += "way_id "+ id+"\n";

						int first = 0;
						while( !line.contains("</way>")){
							line = br.readLine();
							if( line == null )
								break;
							if( line.contains("<tag k=\"highway\"" )){
								int start_index = line.indexOf("v=\"");
								String subStr = line.substring(start_index + "v=\"".length() );
								logger.info( "New subString is " + subStr +"  "+subStr.substring(0, subStr.lastIndexOf('\"') ));
								String street_type = subStr.substring(0, subStr.lastIndexOf('\"'));
								if( keyWords.contains( street_type )&& !restricted_keyWords.contains(street_type) )
									ok = 1;

							}

							if( line.contains("<nd ref=")){
								String []ws = line.split("=");
								if( first == 0){
									way_text += "nodes "+ ws[1].substring(1, ws[1].length()-3) +" ";
									first = 1;
								}
								else{
									way_text += ws[1].substring(1, ws[1].length()-3)+" ";
								}
							}
							else{ 
								if( first == 1 ){
									way_text += "\n";
									first = 0;

								}
								if( !line.contains("</way>") ){
									way_text += line+"\n";
								}
								else{
									way_text += "way_end"+"\n";
								}
							}
						}
						if( ok == 1 ){
							outbr.write( way_text );
						}
						way_text = "";
						if(line == null )
							break;
					}
				}
			}

			outbr.close();
			br.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	/* Function which, based on a file containing way-s, returns the data for every street. Also, for every street, we determine the 
	 * bounding rectangle for a fast search afterwards. 
	 */
    void getStreetsData( ){
		FileInputStream fstream1, fstream2;
		FileOutputStream ostream, ostream1, ostream2;
		String line;
		
		 /* The limits of the map */
	    double map_lon_min, map_lon_max , map_lat_min, map_lat_max;
	    
		/* Path to partial results */
		String indexTableFileName = GenericParams.mapConfig.getIndexTableFilename();
		/* Vector storing index of areas located inside each rectangle */
		Vector<Vector<Integer>> areas = new Vector<Vector<Integer>>();

		try {
			fstream1 = new FileInputStream(GenericParams.mapConfig.getMapFilename());
			fstream2 = new FileInputStream(GenericParams.mapConfig.getPartialStreetsFilename());

			String fn_rez = GenericParams.mapConfig.getStreetsFilename();
	
			String fn_graph = GenericParams.mapConfig.getPartialGraphFilename();

			ostream = new FileOutputStream( fn_rez );
			ostream2 = new FileOutputStream( fn_graph );
			ostream1 = new FileOutputStream( indexTableFileName );

			BufferedReader br = new BufferedReader(new InputStreamReader(fstream1));
			BufferedReader br2 = new BufferedReader(new InputStreamReader(fstream2));
			BufferedWriter outbr = new BufferedWriter(new OutputStreamWriter(ostream));
			TreeMap <Long, Way>ways = new TreeMap <Long, Way>();
			TreeMap <Long, Vector<Long>> nods = new TreeMap <Long, Vector<Long>>();
			Way w = null ;

			while( ( line = br2.readLine()) != null ){
				if( line.startsWith("way_id")){
					StringTokenizer st = new StringTokenizer(line," ");
					st.nextToken();
					w = new Way( Long.parseLong(st.nextToken()));
					line = br2.readLine();
					if( line.startsWith("nodes")){
						st = new StringTokenizer( line, " ");
						st.nextToken();
						while( st.hasMoreTokens() ){
							Long ids = Long.parseLong(st.nextToken());
							if( nods.containsKey(ids)){
								Vector<Long> axs = nods.get( ids );	
								if( axs.contains( w.id) == false ){
									axs.add( w.id );
									nods.put( ids, axs);
								}
								else
								{
									w.setEnclosed(true);
								}
							}
							else {
								Vector<Long> axs = new Vector<Long>();
								axs.add(w.id);
								nods.put( ids, axs );
							}
							w.addVirtualNode( new Node( ids, -360, -360));

						}

						/* Initiate tags */
						while( (line = br2.readLine()) != null ){
							if( line.startsWith("way_end"))
								break;
							if( line.contains("<tag k=\"oneway\"") ){
								if(line.contains( "\"yes\""))
									w.setDirection(true);
							}
							/* determine the maximum speed */
							if( line.contains("<tag k=\"maxspeed\"") ){
									String ws[] =  line.split("v=\"");
									/* replace the speed measure units */
									/* type_speed = 0 kmh; type_speed = 1 mph */
									int type_speed = 0;
									ws[1] = ws[1].replace("kmh", "");
									if( ws[1].contains("mph"))
									{
										type_speed = 1;
										System.out.println(ws[1]);
									}
									int stopIdx = ws[1].indexOf("\"");
									ws[1] = ws[1].substring(0, stopIdx).trim();
									/* (ws[1].split(";"))[0].trim() - this is needed because some streets have two speed separated with ; */
									Double maxspeed = Double.parseDouble( (ws[1].split(";"))[0].trim() );
									/* 1 mile == 1,609344 km */
									/* speed is express in m/s */
									w.setMaximumSpeed( type_speed == 0 ? ( maxspeed * 1000) / 3600  : ( maxspeed * 1609.344) / 3600);
							}
						}
					}

					ways.put(w.id, w);
				}

			}

			while( (line = br.readLine()) != null ){
				if( line.contains("<node") ){

					Long id = Long.parseLong(line.substring( line.indexOf("id=\"")+4,  line.indexOf("id=\"")+4+line.substring( line.indexOf("id=\"")+4).indexOf("\"") ));

					double lat = Double.parseDouble(line.substring( line.indexOf("lat=\"")+5,line.indexOf("lat=\"")+5+line.substring( line.indexOf("lat=\"")+5).indexOf("\"") ));

					double longit = Double.parseDouble(line.substring( line.indexOf("lon=\"")+5,line.indexOf("lon=\"")+5 +line.substring( line.indexOf("lon=\"")+5).indexOf("\"") ));

					if( nods.containsKey( id )){
						Vector<Long> a_way_id = nods.get( id );
						Node nx = new Node( id, lat, longit );
						for( int i = 0; i < a_way_id.size();i++){
							Way aux = ways.get(a_way_id.get(i));

							aux.addNode( nx );
							ways.put( aux.id, aux );
						}
					}
				}
			}
			map_lat_max = -360;
			map_lat_min = 360;
			map_lon_max = -360;
			map_lon_min = 360;

			for( Iterator<Way> it = ways.values().iterator(); it.hasNext(); ){
				w = it.next();

				if( w.min_lat - map_lat_min <= 0 ){
					map_lat_min = w.min_lat;
				}
				if( w.max_lat - map_lat_max >= 0 ){
					map_lat_max = w.max_lat; 
				}
				if( w.min_long - map_lon_min <= 0 ){
					map_lon_min = w.min_long;
				}
				if( w.max_long - map_lon_max >= 0 ){
					map_lon_max = w.max_long;
				}

			}
			logger.info("Finish first stage.");

			int nr_cols = (int)Math.ceil((map_lon_max - map_lon_min)/OSM.SQUARE_L) + 1;
			int nr_rows = (int)Math.ceil((map_lat_max - map_lat_min )/OSM.SQUARE_L) + 1;
			for( int i = 0; i < nr_cols  * nr_rows; i++ )
				areas.add( new Vector<Integer>());
			logger.info( "Columns "+ nr_cols + "Rows "+ nr_rows + " dim " + areas.size() );
			outbr.write("map_lat_min "+ map_lat_min +" map_lon_min " +map_lon_min + " map_lat_max " + map_lat_max + " map_lon_max " + map_lon_max +"\n");

			for( Iterator<Map.Entry<Long,Way>> it = ways.entrySet().iterator(); it.hasNext(); ){
				Map.Entry<Long, Way> a = it.next();
				outbr.write("way_id "+ a.getValue().id+"\n");
				outbr.write("maxspeed "+ a.getValue().getMaximumSpeed()+"\n");
				outbr.write("oneway "+ a.getValue().oneway+"\n");
				outbr.write("enclosed "+ a.getValue().enclosed+"\n");
				Vector<Node> nds_remove = new Vector<Node>();
				Node prev = null;
				for( int i = 0; i < a.getValue().nodes.size(); i++ ){
					Node nds = a.getValue().nodes.get(i);
					/* don't write nodes for which we have no data (latitude and/or longitude) */
					if( nds.lat != -360 && nds.lon != -360 ){
						/*int nr_c = (int)(distance( 0, map_lon_min, 0, nds.longit)/SQUARE_LM);
						int nr_r = (int)(distance( map_lat_min, 0, nds.lat, 0)/SQUARE_LM);*/

						int nr_c = (int) Math.floor(( nds.lon - map_lon_min )/OSM.SQUARE_L);
						int nr_r = (int)Math.floor(( nds.lat - map_lat_min )/OSM.SQUARE_L);

						if( (nr_r * nr_cols + nr_c) == 489 * 638 )
							logger.info( "nr_c " + nr_c +", nr_r " + nr_r + "id cub "+ (nr_r * nr_cols + nr_c) );
						Vector<Integer> ids_street = areas.get( nr_r * nr_cols + nr_c);
						Integer id = (int) a.getValue().id;
						if( ids_street.contains( id ) != true ){
							ids_street.add( id );
							areas.set( nr_r * nr_cols + nr_c, ids_street );
						}

						if( i != 0 && prev != null ){
							double inter_min_lat = nds.lat < prev.lat ? nds.lat : prev.lat; 
							double inter_max_lat = nds.lat > prev.lat ? nds.lat : prev.lat;
							double inter_min_lg = nds.lon < prev.lon ? nds.lon : prev.lon;
							double inter_max_lg = nds.lon > prev.lon ? nds.lon : prev.lon;

							int nr_c_min = (int) Math.floor(( inter_min_lg - map_lon_min )/OSM.SQUARE_L);
							int nr_c_max = (int) Math.floor(( inter_max_lg - map_lon_min )/OSM.SQUARE_L);
							int nr_r_min = (int)Math.floor(( inter_min_lat - map_lat_min )/OSM.SQUARE_L);
							int nr_r_max = (int)Math.floor(( inter_max_lat - map_lat_min )/OSM.SQUARE_L);

							for( int aux_r = nr_r_min; aux_r < nr_r_max; aux_r++)
								for( int aux_c = nr_c_min; aux_c < nr_c_max; aux_c++){
									Vector<Integer> ids_street1 = areas.get( aux_r * nr_cols + aux_c);
									if( ids_street1.contains( id ) != true ){
										ids_street1.add( id );
										areas.set( nr_r * nr_cols + nr_c, ids_street1 );
									}
								}
						}

						outbr.write( "node_id "+nds.id +" lat "+nds.lat + " long "+ nds.lon +"\n" );
						prev = nds;
					}
					else{
						/* remove useless node */
						nds_remove.add(nds);
					}
				}
				for( int i = 0; i < nds_remove.size(); i++ )
					a.getValue().nodes.remove(nds_remove.get(i));
			}
			br.close();
			br2.close();
			outbr.close();

			/* Now, it is created the streets graph using the file with all nodes in order to ease the search */
			/* The graph uses a treemap storing its elements as <current_street_id, <intersection_node, (vector with neighbors streets)>>*/
			TreeMap<Long, TreeMap<Long,Vector<Long>>> grph = new TreeMap<Long, TreeMap<Long,Vector<Long>>>();
			TreeMap<Long,Vector<Long>> ax;
			for( Iterator <Map.Entry<Long, Vector<Long>>> it = nods.entrySet().iterator(); it.hasNext();  ){
				Map.Entry<Long, Vector<Long>> p = it.next();
				Vector<Long> streets_id = p.getValue();
				/* A node is an intersection point if it belongs to a number of streets bigger than 1 */
				if( streets_id.size() <= 1 )
					continue;
				Vector<Long> aux = new Vector<Long>( streets_id ) ; 
				for( int x = 0; x < streets_id.size(); x++ ){
					Long id_crt = streets_id.get(x);
					if( grph.containsKey( id_crt ) ){
						ax = grph.get( id_crt );
						if( ax.containsKey(p.getKey()) ){
							logger.info("Node was previously analyzed already");
						}
						else{
							aux.remove( id_crt );
							ax.put( p.getKey(),  new Vector<Long>(aux) );
							aux.add(id_crt );
						}
					}
					else{
						ax = new TreeMap<Long,Vector<Long>>();
						aux.remove( id_crt );
						ax.put( p.getKey(), new Vector<Long>( aux )  );
						aux.add( id_crt );
					}
					grph.put( id_crt, ax);
				}

			}

			BufferedWriter graph_br = new BufferedWriter( new OutputStreamWriter(ostream2) );
			for( Iterator <Map.Entry<Long, TreeMap<Long,Vector<Long>>>>  it = grph.entrySet().iterator(); it.hasNext();  ){
				Map.Entry<Long, TreeMap<Long,Vector<Long>>> p = it.next();
				graph_br.write("way_id "+p.getKey() + "\n");
				// graph_br.write("oneway "+ways.get(p.getKey()).oneway+"\n");
				for( Iterator<Map.Entry<Long,Vector<Long>>> ita= p.getValue().entrySet().iterator(); ita.hasNext(); ){
					Map.Entry<Long,Vector<Long>> pa = ita.next();
					graph_br.write("join_id "+pa.getKey() );
					for( int ix = 0; ix < pa.getValue().size(); ix++ ){
						graph_br.write( " " + pa.getValue().get(ix) );
					}
					graph_br.write("\n");
				}

			}

			graph_br.close();
			BufferedWriter squares_map_br = new BufferedWriter( new OutputStreamWriter(ostream1) );
			for( int i = 0; i < nr_cols * nr_rows; i++ ){
				squares_map_br.write( "sq_nr " + i +"\n");
				Vector<Integer> v = areas.get(i);
				for( int j = 0; j < v.size(); j++ )
				{
					squares_map_br.write( v.get(j) +" ");
				}
				if( v.size() != 0 )
					squares_map_br.write("\n");
			}
			squares_map_br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static TreeMap<Long, Way> load_graph( String streets_file, String graph_partial_file, double limits[] ){
		FileInputStream fstream, fstream2;
		TreeMap<Long, Way> grph= new TreeMap<Long, Way>();
		try {
			String line;
			fstream = new FileInputStream( streets_file );
			fstream2 = new FileInputStream( graph_partial_file );
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			BufferedReader ways_fr = new BufferedReader(new InputStreamReader(fstream2));
			/* Read the entire file with streets. This is needed because there are streets without jointures in osm file */
			int first = 0;
			Way aux = null;
			line = br.readLine();
			if( line == null )
			{
				br.close();
				ways_fr.close();
				return null;
			}
			if( line.contains("map_lat_min") ){
				String [] ws = line.split(" ");
				limits[0] = /*map_lat_min =*/ Double.parseDouble( ws[1] );
				limits[1] = /*map_lon_min =*/ Double.parseDouble( ws[3] );
				limits[2] = /*map_lat_max =*/ Double.parseDouble( ws[5] );
				limits[3] = /*map_lon_max =*/ Double.parseDouble( ws[7] );
			}

			while( (line = br.readLine()) != null ){

				if( line.contains("way_id")){
					String [] ws = line.split(" ");
					if( first == 0){
						aux = new Way(Long.parseLong(ws[1]));
						first = 1;
					}
					else{
						grph.put( aux.id, aux);
						aux = new Way(Long.parseLong(ws[1]));
					}
				}
				if( line.contains("maxspeed")){
					String [] ws = line.split(" ");
					Double maxspeed = Double.parseDouble(ws[1]);
					aux.setMaximumSpeed(maxspeed);
				}
				if( line.contains("oneway")){
					String [] ws = line.split(" ");
					boolean bl = Boolean.parseBoolean(ws[1]);
					aux.setDirection(bl);
				}
				if( line.contains("enclosed")){
					String [] ws = line.split(" ");
					boolean bl = Boolean.parseBoolean(ws[1]);
					aux.setEnclosed(bl);
				}

				if( line.contains("node_id")){
					String [] ws = line.split(" ");
					double lt = Double.parseDouble(ws[3]);
					double lg = Double.parseDouble(ws[5]);

					Node nd = new Node( Long.parseLong(ws[1]), lt, lg);
					nd.setWayId(aux.id);
					aux.addNode( nd );
				}	
			}
			grph.put( aux.id, aux);

			fstream2 = new FileInputStream( graph_partial_file );
			ways_fr = new BufferedReader(new InputStreamReader(fstream2));
			line = ways_fr.readLine();
			if( line == null ){
				br.close();
				ways_fr.close();
				return null;
			}

			/* Add the links between streets */
			do{
				String [] ws = line.split(" ");
				if( ws[0].compareTo("way_id") == 0 ){
					aux = grph.get(Long.parseLong(ws[1]));
					if( aux == null ){
						logger.warning( "Error: Unfound line for way" + ws[1] );
					}
					while ( ( line = ways_fr.readLine()) != null ){
						if( line.contains("join_id") == false  )
							break;
						ws = line.split(" ");

						Vector<Long> av = aux.neighs.get( Long.parseLong(ws[1]) );
						if( av == null )
							av = new Vector<Long>();
						for( int h = 2; h < ws.length; h++ ){
							av.add( Long.parseLong(ws[h]));
						}
						aux.neighs.put( Long.parseLong(ws[1]), av );
					}
					grph.put( aux.id, aux );

				}

			}
			while( line != null );
			br.close();
			ways_fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return grph;
	}
	
	/* Load the area indexes for a map */
	public static Vector<Vector<Integer>> load_areas( String areas_file, double limits[]  ){
		Vector<Vector<Integer>> ars = new Vector<Vector<Integer>>();
		FileInputStream fstream;
		int nr_cols, nr_rows;
		double map_lon_max, map_lon_min, map_lat_max, map_lat_min;

		map_lat_min = limits[0];
		map_lon_min = limits[1];
		map_lat_max = limits[2];
		map_lon_max = limits[3];

		nr_cols = (int)Math.ceil((map_lon_max - map_lon_min)/OSM.SQUARE_L) + 1;
		nr_rows = (int)Math.ceil((map_lat_max - map_lat_min )/OSM.SQUARE_L) + 1;
		for( int i = 0; i < nr_cols * nr_rows; i++ ){
			ars.add( new Vector<Integer>() );
		}
		try {
			String line;
			fstream = new FileInputStream( areas_file );
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			int id = -1;
			while( (line = br.readLine()) != null ){
				if( line.startsWith("sq_nr")){
					String [] ws = line.split(" ");
					id = Integer.parseInt(ws[1]);

				}
				else{
					if( id != -1 ){
						StringTokenizer st = new StringTokenizer(line, " ");
						Vector<Integer> aux = new Vector<Integer>();
						while( st.hasMoreTokens()){
							aux.add( Integer.parseInt( st.nextToken()));
						}
						if( aux.size() != 0 ){
							ars.set( id, aux );
						}
					}

				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ars;
	}

	/* Returns the intersection node of 2 streets, on the direction from wd1 to wd2 */
	public static Node getIntersectNode( long wid1, long wid2, TreeMap<Long, Way> graph ){
		Node int_nod = null;
		Way w1 = graph.get( wid1 ); 
		if( w1 == null ){
			return null;
		}
		for( Iterator<Map.Entry<Long,Vector<Long>>> it = w1.neighs.entrySet().iterator(); it.hasNext(); ){
			Map.Entry<Long, Vector<Long>> aux = it.next();

			if(  aux.getValue().contains( wid2 ) ){
				int index = w1.getNodeIndex( aux.getKey() );
				if( index == -1 )
					continue;
				int_nod = w1.nodes.get(index);
			}
		}

		return int_nod;

	}
	
	public void buildOSMGRAPH( boolean exec_phase1, boolean exec_phase2)
	{
		logger.info("Starting to build the OSM graph");
		if(exec_phase1)
		{
			logger.info("Gets the ways from the OSM file");
			getWays( OSM.GenModPartialFile );
		}

		if(exec_phase2)
		{
			logger.info("Build the Graph Structure");
			getStreetsData();
		}
		
	}

}
