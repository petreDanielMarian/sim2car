package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

import model.GeoCarRoute;
import model.LocationParse;
import model.MapPoint;
import model.PixelLocation;
import model.OSMgraph.Node;
import model.OSMgraph.Way;
import model.parameters.Globals;

class TraceToolPair implements Comparable<TraceToolPair> {

	private long time;
	private String filename;

	TraceToolPair (long time, String filename){
		this.time = time;
		this.filename = filename;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getTime() {
		return this.time;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getFilename() {
		return filename;
	}

	@Override
	public int compareTo(TraceToolPair o) {
		/* Use the file read order. */
		if (getTime() - o.getTime() == 0)
			return -1;
		return (int)(getTime() - o.getTime());
	}
}

public class TraceParsingTool {

    /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(TraceParsingTool.class.getName());
	
	static String mapPath, rootPath, dir_diff_time;
	static String tracesListPath, resultsCorectionAlg,  resultsInterpolationAlg;
	static String  resultsPartialPath, graphPartialFileName;
	static String streetsFileName, streetsPartialFileName, indexTableFileName, routeTimeData;
	/* 1 - beijing, 2 - san-franciso, 3 - paris; 4 -bucharest; 5- shanghai; 6- brussel */
	static int tip_parsare = 2;
	static String ConfigFileName = get_TipParsare(tip_parsare);

	final static double RP = 6380;

	/* TreeSet storing timestamps in traces */
	static TreeSet<TraceToolPair> time_start_traces;

	/* The graph of streets */
	static TreeMap<Long, Way> graph;

	/* Vector storing index of areas located inside each rectangle */
	static Vector<Vector<Integer>> areas = new Vector<Vector<Integer>>();
	final static double SQUARE_L = 0.0011357 * 5; /* an edge of 500 m */
	final static double DIF = SQUARE_L;
	final static int SQUARE_LM = 100;
	static double map_lon_min, map_lon_max , map_lat_min, map_lat_max; 

	public static String get_TipParsare( int i ){
		String s="";
		switch( i ){
		case 1: 
			s = "TPbeijing.properties";
			break;
		case 2:
			s= "TPsan-francisco.properties";
			break;
		case 3:
			s= "TPparis.properties";
			break;
		case 4:
			s= "TPbucuresti.properties";
			break;
		case 5:
			s= "TPshanghai.properties";
			break;
		case 6:
			s= "TPbrussels.properties";
			break;
		}
		return s;
	}

	/* Returns the intersection node of 2 streets, on the direction from wd1 to wd2 */
	public static Node getIntersectNode( long wid1, long wid2 ){
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

	/* Converts a trace file in a file with timestamp diferences between consecutive positions.
	 */
	static void getDiff( String fname, int tip ){
		FileInputStream fstream;
		FileOutputStream ostream;
		BufferedWriter outbr = null ;
		String line;
		long prev,crt = 0;
		try {
			fstream = new FileInputStream(fname);
			int index = fname.lastIndexOf('\\');
			String fn_rez = dir_diff_time + "\\rez_" + fname.substring(index + 1);

			TraceToolPair c = null;
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			if( tip == 2) {
				ostream = new FileOutputStream( fn_rez );
				outbr = new BufferedWriter(new OutputStreamWriter(ostream));
			}
			line = br.readLine();
			if( line == null || line.length() < 2 )
				return;
			String [] ws = line.split(" ");
			prev = Integer.parseInt(ws[3]);
			c = new TraceToolPair( prev, fname.substring(index + 1));
			while( ( line = br.readLine() ) != null ){
				ws = line.split(" ");
				crt = Integer.parseInt(ws[3]);

				if( tip == 2)
					outbr.write( (prev - crt) +"\n");
				else{ 
					c = new TraceToolPair( crt, fname.substring(index + 1));
					break;
				}
				prev = crt;
			}

			if( tip == 2 )
				outbr.close();
			if( tip == 3)
				time_start_traces.add( c );

			br.close();
		} catch (FileNotFoundException e) {
			logger.severe( " Error opening file "+ fname);
			e.printStackTrace();
		} catch (IOException e) {
			logger.severe( "Error operations on file " + fname );
			e.printStackTrace();
		}


	}
	
	/* Url path to the folder with traces 
	 * fct_apel -> 0 - fct call for timestamp differences 
	 * 			-> 1 - fct call for GPS data correction algorithm
	 * 			-> 2 - fct call for interpolation algorithm /
	 */	
	public static void parse_data( String url, int fct_apel, int deltaT ){
		FileInputStream fstream;

		try {
			int i = 0;
			fstream = new FileInputStream( url+ "\\_cabs.txt" );
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String line;
			if( fct_apel == 3 ){
				time_start_traces = new TreeSet<TraceToolPair>();
			}


			while( (line = br.readLine()) != null ){
				
				StringTokenizer st = new StringTokenizer(line, " ", false);
				st.nextToken();
				String srcId = st.nextToken();
				srcId = srcId.substring(4, srcId.length() - 1);
				String name = "";
				if (tip_parsare == 1)
					name = srcId+".txt";
				else 
					name = "new_"+srcId+".txt";
				/* TBD corect this function */
				
				
				switch( fct_apel ){
				case 0:
					getDiff( url +"\\"+ name, 2);
					break;
				case 1:
					CorrectionAlgorithm(name );
					break;
				case 2:
					InterpolAlgorithm( name, deltaT );
					break;
				case 3:
					getDiff( resultsCorectionAlg +"\\"+ name, 3);
					break;
				case 4:
					name = srcId+".txt";
					TraceLayoutAlgorithm( name );
					break;
				}

			}

			if( fct_apel == 3 ){
				FileOutputStream ostream;
				BufferedWriter outbr;
				ostream = new FileOutputStream( resultsPartialPath +"\\times_traces" );
				outbr = new BufferedWriter(new OutputStreamWriter(ostream));
				for( Iterator<TraceToolPair> it = time_start_traces.iterator(); it.hasNext(); ){
					TraceToolPair aux = it.next();
					outbr.write( aux.getTime() +" "+ aux.getFilename() +"\n");
				}
				outbr.close();
			}
		} catch (FileNotFoundException e) {
			logger.severe( " Error opening file with cabs.txt ");
			e.printStackTrace();
		} catch (IOException e) {
			logger.severe( "Error making operations on file cabs.txt");
			e.printStackTrace();
		}
	}
	
	public static void getTimeIntervalAllCabs( ){
		parse_data(tracesListPath, 0, 0);
	}
	
	/* Returns the streets from osm xml, depending on the way-id tag.
	 * Initially, count way id. mod - 0 - raw file ; 1 - modified file.
	 **/
	static void getWays(String fname, int mod ){
		FileInputStream fstream;
		FileOutputStream ostream;
		String line;

		String []s  = new String[] {"motorway", "motorway_link", "trunk", "trunk_link", "primary",
				"primary_link", "secondary", "secondary_link", "tertiary", "tertiary_link",
				"living_street", "residential", "unclassified", "service", "track", "road"}; 
		Vector<String> keyWords = new Vector<String>( Arrays.asList(s) );

		try {
			fstream = new FileInputStream(fname);
			int index = fname.lastIndexOf('\\');
			String fn_rez = resultsPartialPath+ "rez_" +( mod == 0 ?"brut_":"mod_")+ fname.substring(index + 1);

			ostream = new FileOutputStream( fn_rez );
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
						//outbr.write( "way_id "+ id+"\n"  );
						int first = 0;
						while( !line.contains("</way>")){
							line = br.readLine();
							if( line == null )
								break;
							if( line.contains("<tag k=\"highway\"" )){
								int start_index = line.indexOf("v=\"");
								String subStr = line.substring(start_index + "v=\"".length() );
								logger.info( "New subString is " + subStr +"  "+subStr.substring(0, subStr.lastIndexOf('\"') ));

								if( keyWords.contains(subStr.substring(0, subStr.lastIndexOf('\"'))))
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
	static void getStreetsData( String fname, String fways){
		FileInputStream fstream1, fstream2;
		FileOutputStream ostream, ostream1, ostream2;
		String line;

		try {
			fstream1 = new FileInputStream(fname);
			fstream2 = new FileInputStream(fways);
			int index = fname.lastIndexOf('\\');
			String fn_rez = resultsPartialPath + "streets_rez_"+ fname.substring(index + 1);
			String fn_graph = resultsPartialPath + "streets_graph_"+ fname.substring(index + 1);
			ostream = new FileOutputStream( fn_rez );
			ostream2 = new FileOutputStream( fn_graph );
			ostream1 = new FileOutputStream( resultsPartialPath + indexTableFileName );
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream1));
			BufferedReader br2 = new BufferedReader(new InputStreamReader(fstream2));
			BufferedWriter outbr = new BufferedWriter(new OutputStreamWriter(ostream));
			TreeMap <Long, Way>ways = new TreeMap <Long, Way>();
			TreeMap <Long, Vector<Long>> nods = new TreeMap <Long, Vector<Long>>();
			Way w = null ;
			//int first = 0;
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
								Vector axs = nods.get( ids );	
								if( axs.contains( w.id) == false ){
									axs.add( w.id );
									nods.put( ids, axs);
								}
							}
							else {
								Vector axs = new Vector();
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
								w.setDirection(true);
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

			int nr_cols = (int)Math.ceil((map_lon_max - map_lon_min)/SQUARE_L) + 1;
			int nr_rows = (int)Math.ceil((map_lat_max - map_lat_min )/SQUARE_L) + 1;
			for( int i = 0; i < nr_cols  * nr_rows; i++ )
				areas.add( new Vector<Integer>());
			logger.info( "Columns "+ nr_cols + "Rows "+ nr_rows + " dim " + areas.size() );
			outbr.write("map_lat_min "+ map_lat_min +" map_lon_min " +map_lon_min + " map_lat_max " + map_lat_max + " map_lon_max " + map_lon_max +"\n");

			for( Iterator<Map.Entry<Long,Way>> it = ways.entrySet().iterator(); it.hasNext(); ){
				Map.Entry<Long, Way> a = it.next();
				outbr.write("way_id "+ a.getValue().id+"\n");
				outbr.write("oneway "+ a.getValue().oneway+"\n");
				Vector<Node> nds_remove = new Vector<Node>();
				Node prev = null;
				for( int i = 0; i < a.getValue().nodes.size(); i++ ){
					Node nds = a.getValue().nodes.get(i);
					/* don't write nodes for whic we have no data (latitude and/or longitude) */
					if( nds.lat != -360 && nds.lon != -360 ){
						/*int nr_c = (int)(distance( 0, map_lon_min, 0, nds.longit)/SQUARE_LM);
						int nr_r = (int)(distance( map_lat_min, 0, nds.lat, 0)/SQUARE_LM);*/

						int nr_c = (int) Math.floor(( nds.lon - map_lon_min )/SQUARE_L);
						int nr_r = (int)Math.floor(( nds.lat - map_lat_min )/SQUARE_L);

						if( (nr_r * nr_cols + nr_c) == 489 * 638 )
							logger.info( "nr_c " + nr_c +", nr_r " + nr_r + "id cub "+ (nr_r * nr_cols + nr_c) );
						Vector ids_street = areas.get( nr_r * nr_cols + nr_c);
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

							int nr_c_min = (int) Math.floor(( inter_min_lg - map_lon_min )/SQUARE_L);
							int nr_c_max = (int) Math.floor(( inter_max_lg - map_lon_min )/SQUARE_L);
							int nr_r_min = (int)Math.floor(( inter_min_lat - map_lat_min )/SQUARE_L);
							int nr_r_max = (int)Math.floor(( inter_max_lat - map_lat_min )/SQUARE_L);

							for( int aux_r = nr_r_min; aux_r < nr_r_max; aux_r++)
								for( int aux_c = nr_c_min; aux_c < nr_c_max; aux_c++){
									Vector ids_street1 = areas.get( aux_r * nr_cols + aux_c);
									if( ids_street1.contains( id ) != true ){
										ids_street1.add( id );
										areas.set( nr_r * nr_cols + nr_c, ids_street1 );
									}
								}
						}

						outbr.write( nds.toString() );
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
			/*BufferedWriter graph_br = new BufferedWriter( new OutputStreamWriter(ostream2) );
			for( Iterator <Map.Entry<Long, Vector<Long>>> it = nods.entrySet().iterator(); it.hasNext();  ){
				Map.Entry<Long, Vector<Long>> p = it.next();
				graph_br.write( p.getKey()+" \n" );
				graph_br.write( p.getValue() +" \n");
			}
			graph_br.close();*/
			/* Acum creez graful strazilor avand si fisierul cu toate nodurile pentru a face o cautare mai usoara*/
			//logger.info(" Cheile strazilor sunt " + ways.size());
			/* Graful retinut in treeset o sa aiba forma : < id_strada_curenta <id_nod_comun, (strazile vecine) > > */
			TreeMap<Long, TreeMap<Long,Vector<Long>>> grph = new TreeMap<Long, TreeMap<Long,Vector<Long>>>();
			TreeMap<Long,Vector<Long>> ax;
			for( Iterator <Map.Entry<Long, Vector<Long>>> it = nods.entrySet().iterator(); it.hasNext();  ){
				Map.Entry<Long, Vector<Long>> p = it.next();
				Vector<Long> streets_id = p.getValue();
				// Un nod este punct de intersectie daca strees_id are dimensiunea mai mare decat 2 
				if( streets_id.size() <= 1 )
					continue;
				Vector<Long> aux = new Vector( streets_id ) ; 
				for( int x = 0; x < streets_id.size(); x++ ){
					Long id_crt = streets_id.get(x);
					if( grph.containsKey( id_crt ) ){
						ax = grph.get( id_crt );
						if( ax.containsKey(p.getKey()) ){
							logger.info("Node was previously analyzed already");
						}
						else{
							aux.remove( id_crt );
							ax.put( p.getKey(),  new Vector(aux) );
							aux.add(id_crt );
						}
					}
					else{
						ax = new TreeMap<Long,Vector<Long>>();
						aux.remove( id_crt );
						ax.put( p.getKey(), new Vector( aux )  );
						aux.add( id_crt );
					}
					grph.put( id_crt, ax);
				}

			}

			BufferedWriter graph_br = new BufferedWriter( new OutputStreamWriter(ostream2) );
			for( Iterator <Map.Entry<Long, TreeMap<Long,Vector<Long>>>>  it = grph.entrySet().iterator(); it.hasNext();  ){
				Map.Entry<Long, TreeMap<Long,Vector<Long>>> p = it.next();
				graph_br.write("way_id "+p.getKey() +" "+ ways.get(p.getKey()));
				// graph_br.write("oneway "+ways.get(p.getKey()).oneway+"\n");
				for( Iterator<Map.Entry<Long,Vector<Long>>> ita= p.getValue().entrySet().iterator(); ita.hasNext(); ){
					Map.Entry<Long,Vector<Long>> pa = ita.next();
					graph_br.write("join_id "+pa.getKey() );
					for( int ix = 0; ix < pa.getValue().size(); ix++ ){
						graph_br.write( " "+pa.getValue().get(ix) );
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
					squares_map_br.write( v.get(j) +" ");
				if( v.size() != 0 )
					squares_map_br.write("\n");
			}
			squares_map_br.close();
			//logger.info(" Numarul de cai "+ways.size() +"  numarul de cai dupa "+ graph.size());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/* Functie care intoarce distanta dintre 2 puncte pe glob folosind formula 
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
		dist = 2 * RP * Math.atan2( Math.sqrt( x),  Math.sqrt(1-x) )*1000; 
		return dist;
	}
	/* Functie de parsare pentru o linie din traceul cabs san-francisco*/
	public static LocationParse parseLineTrace( String line ){
		StringTokenizer st = new StringTokenizer(line," ",false);
		double lat = Double.parseDouble(st.nextToken());
		double lon = Double.parseDouble(st.nextToken());
		int isOccupied = Integer.parseInt(st.nextToken());
		long time = Long.parseLong(st.nextToken());
		LocationParse crt = new LocationParse(lat, lon, isOccupied, time); 
		if( st.hasMoreTokens() ){
			long wid = Long.parseLong(st.nextToken());
			crt.setIdStreet(wid);
		}
		return crt;
	}
	/* Functie de parsare pentru o linie din traceul cabs san-francisco*/
	public static LocationParse parseLineTraceBeijing( String line ){
		StringTokenizer st = new StringTokenizer(line,",",false);
		/* skip the id of taxi */
		st.nextToken();
		/* Set the date format in GMT and ex: 2008-02-02 13:34:12*/
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		/* get the date */
		Date d = null;
		long time = 0;
		try {
			d = sd.parse(st.nextToken());

			/* Because are retun miliseconds */
			time = d.getTime()/1000;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//logger.info( "Momentul de timp citit este " + d.getTime()/1000 );
		double lon = Double.parseDouble(st.nextToken());
		double lat = Double.parseDouble(st.nextToken());

		int isOccupied = 0;

		LocationParse crt = new LocationParse(lat, lon, isOccupied, time ); 
		if( st.hasMoreTokens() ){
			long wid = Long.parseLong(st.nextToken());
			crt.setIdStreet(wid);
		}
		return crt;
	}
	/* Functie care verifica daca o zona contine un punct */
	public static boolean check_limits( LocationParse loc, double mLat, double mLon, double MLat, double MLon){
		return  mLat - DIF <= loc.lat  && loc.lat <= MLat + DIF && 
				mLon - DIF <= loc.lon && loc.lon <= MLon + DIF;
	}

	/* Functie care  calculeaza unghiul dintre 2 vectori */
	public static double getAngle( double a, double b, double c, double d){
		double c_angle = (a*c + b*d)/ Math.sqrt( (a*a + b*b)*(c*c + d*d) );
		return Math.acos(c_angle)* 180 /Math.PI;
	}
	/* Functie care incarca vectorul cu zone de pe o harta */
	public static Vector<Vector<Integer>> load_areas( String areas_file ){
		Vector<Vector<Integer>> ars = new Vector<Vector<Integer>>();
		FileInputStream fstream;
		int nr_cols, nr_rows;
		/*int nr_cols = 1 + (int)distance( 0, map_lon_min, 0, map_lon_max )/SQUARE_LM;
		int nr_rows = 1 + (int)distance( map_lat_min, 0, map_lat_max, 0 )/SQUARE_LM;*/
		nr_cols = (int)Math.ceil((map_lon_max - map_lon_min)/SQUARE_L) + 1;
		nr_rows = (int)Math.ceil((map_lat_max - map_lat_min )/SQUARE_L) + 1;
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
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ars;
	}
	/* Functie care incarca graful complet in memorie. 
	 * Tot in aceasta functie se construieste si vectorul de patrate in care pun strazile */
	public static TreeMap<Long, Way> load_graph( String streets_file, String graph_partial_file ){
		FileInputStream fstream, fstream2;
		TreeMap<Long, Way> grph= new TreeMap<Long, Way>();
		try {
			String line;
			fstream = new FileInputStream( streets_file );
			fstream2 = new FileInputStream( graph_partial_file );
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			BufferedReader ways_fr = new BufferedReader(new InputStreamReader(fstream2));
			/* Citesc tot fisierul cu strazi. Fac acest lucru pentru ca exista unele strazi nu au intersectii in fisierul de osm */
			int first = 0;
			Way aux = null;
			line = br.readLine();
			if( line == null )
				return null;
			if( line.contains("map_lat_min") ){
				String [] ws = line.split(" ");
				map_lat_min = Double.parseDouble( ws[1] );
				map_lon_min = Double.parseDouble( ws[3] );
				map_lat_max = Double.parseDouble( ws[5] );
				map_lon_max = Double.parseDouble( ws[7] );
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
				if( line.contains("oneway")){
					String [] ws = line.split(" ");
					boolean bl = Boolean.parseBoolean(ws[1]);
					aux.setDirection(bl);
				}
				if( line.contains("nod_id")){
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
				return null;
			}

			/* acum adaug si legaturile dintre strazile care au intersectii comune */
			do{
				String [] ws = line.split(" ");
				if( ws[0].compareTo("way_id") == 0 ){
					aux = grph.get(Long.parseLong(ws[1]));
					if( aux == null ){
						logger.warning( "Eroare negasit" + ws[1] );
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

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return grph;
	}
	/* Functie care incarca in memorie fisierul cu timpi minimi per trace */
	public static TreeSet<TraceToolPair> loadTimeStartFile( String file ){
		String line;
		TreeSet<TraceToolPair> t = new TreeSet<TraceToolPair>();
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			while( ( line = br.readLine()) != null ){
				String []ws = line.split(" ");
				t.add(  new TraceToolPair( Long.parseLong(ws[0]), ws[1]) );
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info( ""+t.size ());
		return t;
	}
	/* Determine the projection of a point on a line
	 * System 
	 *  PQ: y-yp = -1/m(x-xp);
	 *  d: y = mx + b;
	 *  
	 *  y -y0 = m( x-x0)
	 *  y = mx -mx0 + y0 => b = -mx0 + y0;
	 * 
	 * Xq = (m* yp + xp -mb)/(1+m^2);
	 * Yq = m*Xq+b
	 * */
	public static  LocationParse getProjection2( LocationParse nod, Node a, Node b ){
		LocationParse prj;

		if( b.lat - a.lat == 0){
			logger.info("Ies pe aici");
			return nod;
		}
		double panta = (b.lon-a.lon )/(b.lat-a.lat );
		double bp = -panta* a.lat + a.lon;

		double Xq = ( panta * nod.lon +nod.lat - panta * bp);
		double Yq = panta * Xq + bp;


		prj = new LocationParse(  Xq, Yq, nod.occupied, nod.timestamp);
		return prj;

	}
	/* Functie care determina punctul de proiectie */
	public static  LocationParse getProjection( LocationParse nod, Node a, Node b ){
		LocationParse prj;
		double apx = nod.lat - a.lat;
		double apy = nod.lon - a.lon;
		double abx = b.lat - a.lat;
		double aby = b.lon - a.lon;

		double ab2 = abx * abx + aby * aby;
		double ap_ab = apx * abx + apy * aby;
		double t = ap_ab / ab2;
		if (t < 0) {
			t = 0;
		} 
		else if (t > 1) {
			t = 1;
		}

		prj = new LocationParse(  a.lat+abx*t, a.lon+aby *t, nod.occupied, nod.timestamp);
		return prj;
	}
	
	public static LinkedList<LocationParse> readTraceData( String path, String fname ){
		FileInputStream fstream;
		String fnamePh = path  +"\\" + fname;
		/*
		String workingDir = System.getProperty("user.dir");
		logger.info("Current working directory : " + workingDir);
		logger.info( fnamePh );
		*/
		try {
			fstream = new FileInputStream(fnamePh);


			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String line;
			LocationParse crt, prev;
			LinkedList<LocationParse> data = new LinkedList<LocationParse>();
			line = br.readLine();
			if( line == null|| line.length() < 5)
				return null;
			prev = parseLineTrace(line);
			data.add(prev);
			while( (line = br.readLine()) != null ){
				crt = parseLineTrace(line);
				/* Aici se eliminau punctele din trace care apar la un 
				 * interval prea mare. Gen peste 1 minut.
				 */
				/*if( prev.timestamp - crt.timestamp  > 70 ){
			   	 		index_loc.add( data.size() - 1);
			   	 	}*/

				data.add(crt);
				//prev = crt;
			}
			br.close();
			fstream.close();
			/* Le ordonez in ordine cronologica */ 
			Collections.reverse( data );
			return data;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	public static LinkedList<LocationParse> readTraceDataBeijing( String path, String fname ){
		FileInputStream fstream;
		String fnamePh = path  +"\\" + fname;
		try {
			fstream = new FileInputStream(fnamePh);
			LinkedList<LocationParse> data = new LinkedList<LocationParse>();

			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String line;
			LocationParse crt, prev;

			line = br.readLine();

			if( line ==  null )
				return null;
			prev = parseLineTraceBeijing(line);

			data.add(prev);

			while( (line = br.readLine()) != null ){
				crt = parseLineTraceBeijing(line);

				if( crt.timestamp == prev.timestamp )
					continue;
				data.add(crt);

			}
			br.close();
			fstream.close();
			return data;
		} catch (FileNotFoundException e) {

			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}
	public static boolean  outofmap( LocationParse nod ){
		return  nod.lat < map_lat_min || nod.lat > map_lat_max ||
				nod.lon < map_lat_min || nod.lon > map_lon_max;
	}
	public static void showLocWithoutCand( TreeMap<Integer,LocationParse> cs, String fname ){

		FileOutputStream ostream;
		try {
			ostream = new FileOutputStream( "fc1_"+fname );

			BufferedWriter outbr = new BufferedWriter(new OutputStreamWriter(ostream));

			for( Iterator<Map.Entry<Integer, LocationParse>> it = cs.entrySet().iterator(); it.hasNext();){
				Map.Entry<Integer,LocationParse> ca = it.next();
				LocationParse c = ca.getValue();
				outbr.write(  ca.getKey() + " "+ c.lat +" "+ c.lon +" " + c.occupied + " "+ c.timestamp +"\n");

			}
			outbr.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void showLocCand( TreeMap<Integer,Vector<LocationParse>> cs, String fname ){

		FileOutputStream ostream;
		try {
			ostream = new FileOutputStream( "fc1_"+fname );

			BufferedWriter outbr = new BufferedWriter(new OutputStreamWriter(ostream));

			for( Iterator<Map.Entry<Integer, Vector<LocationParse>>> it = cs.entrySet().iterator(); it.hasNext();){
				Map.Entry<Integer,Vector<LocationParse>> ca = it.next();
				Vector<LocationParse> c = ca.getValue();
				for( int i = 0; i < c.size(); i++ ){
					LocationParse cx = c.get(i);
					outbr.write(  ca.getKey() + " "+ cx.lat +" "+ cx.lon +" " + cx.occupied + " "+ cx.timestamp +"\n");
				}

			}
			outbr.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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

			/*nr_cols = 1 + (int)distance( 0, map_lon_min, 0, map_lon_max )/SQUARE_LM;
			nr_rows = 1 + (int)distance( map_lat_min, 0, map_lat_max, 0 )/SQUARE_LM;*/

			nr_cols = (int)Math.ceil((map_lon_max - map_lon_min)/SQUARE_L) + 1;
			nr_rows = (int)Math.ceil((map_lat_max - map_lat_min )/SQUARE_L) + 1;

			LinkedList<LocationParse> data;
			if( tip_parsare == 1 )
				data = readTraceDataBeijing( tracesListPath, fname );
			else
				data = readTraceData( tracesListPath, fname );

			if( data == null ){
				System.err.println("Eroare citire trace " + fname );
			}
			LocationParse crt, prev;
			logger.info( "Dimensiune puncte in trace "+ data.size() + "pentru fisierul " +  fname );



			/* Se iau punctele in care apar diferentele mari de timp si pentru ele se incearca sa se localizeze */
			time_start = System.currentTimeMillis();
			/* Vectorul care tine pentru fiecare node din date idurile strazile candidate */
			TreeMap<Integer, Vector<LocationParse>> candidates;
			Vector <LocationParse> ids;
			candidates = new TreeMap<Integer, Vector <LocationParse>> ();
			int d = data.size();
			int [] nr_c = new int[3];
			int [] nr_r = new int[3];
			TreeMap<Integer,LocationParse> out_cand = new TreeMap<Integer,LocationParse>();
			for( int i = 0; i < d; i++ ){
				//logger.info( "Am ajuns la "+ i + "din "+ d);

				crt = data.get(i);
				/*if( outofmap(crt) )
						continue;*/
				ids = new Vector<LocationParse>();



				nr_c[0] = (int) Math.floor(( crt.lon - map_lon_min )/SQUARE_L);
				nr_c[1] = nr_c[0] + 1;
				nr_c[2] = nr_c[0] -1; 


				nr_r[0] = (int)Math.floor(( crt.lat - map_lat_min )/SQUARE_L);
				nr_r[1] = nr_r[0] + 1;
				nr_r[2] = nr_r[0] - 1; 

				//					if( i == 80){
				//						logger.info( " Randul "+ nr_r[0] + " Coloana " + nr_c[0]);
				//					}
				int last = -1; 
				for( int x = 0; x < 3; x++ ){
					if( nr_r[x] < 0 || nr_r[x] > nr_rows )
						continue;
					for( int y = 0; y < 3; y++ ){
						if( nr_c[y] < 0 || nr_c[y] > nr_cols )
							continue;

						int crt_sq = nr_r[x] * nr_cols + nr_c[y]; 
						//							if( i == 80){
						//								logger.info( " Patrat curent" + crt_sq + " rand " + nr_r[x]+ "col " + nr_c[y]);
						//							}
						if(  crt_sq != last && crt_sq < nr_cols * nr_rows){
							//								if( i == 80){
							//									logger.info("Intru mai departe pentru analiza " + crt_sq); 
							//								}
							last = crt_sq;
							Vector<Integer> aux = areas.get(crt_sq);
							if( aux == null )
								continue;
							for( int h = 0; h < aux.size(); h++ ){
								long id = aux.get(h);
								int hz;
								/* caut sa vad daca strada a mai fost adaugata */
								for( hz = 0; hz < ids.size(); hz++)
									if( ids.get(hz).wid == (long)id )
										break;
								if( hz < ids.size())
									continue;

								Way w = graph.get((long)id);

								if( check_limits( crt, w.min_lat, w.min_long, w.max_lat, w.max_long ) ){
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

										if( crt.lat >= inter_min_lat && crt.lat <= inter_max_lat && 
												crt.lon >= inter_min_lg && crt.lon <= inter_max_lg ){
											LocationParse nd_ax = new LocationParse(crt.lat, crt.lon, crt.occupied, crt.timestamp);
											nd_ax.ontheStreetFirst = true;
											nd_ax.setIdStreet( w.id );
											// Seteaza indexul unde s-a gasit o proiectie mai mica decat 10 m 
											nd_ax.setIndexNodes( j );
											ids.add( nd_ax );
											continue;
										}

										LocationParse nd_ax = getProjection(crt, prev_n, crt_n );
										double dist = distance( crt.lat, crt.lon, nd_ax.lat, nd_ax.lon );
										/*if( i == 80){
												logger.info( "Street id " + w.id + "Intru mai departe pentru analiza " + dist); 
											}*/
										if(  dist <= 100){
											nd_ax.setIdStreet( w.id );
											// Seteaza indexul unde s-a gasit o proiectie mai mica decat 10 m 
											nd_ax.setIndexNodes( j );
											ids.add( nd_ax );
											//break;
										}

									}
								}

							}
						}
					}
				}

				// Pun doar nodurile care au posibile noduri candidate 
				//if( i < 13 ) logger.info( "i " + i + " are candidati " + ids.size()  );
				if( ids.size() != 0 )
					candidates.put(i, ids);
				/*else{
						out_cand.put(i, crt);
					}*/

			}


			logger.info( " Terminare determinare candidati Dimensiune " + candidates.size() );
			//showLocCand(candidates, fname);



			/* faza 4 */
			/* 	Iau fiecare nod din trace si nodul care l-a precedat pentru a putea elimina din candidatii
			 * care apar pentru nod din trace.
			 * 	Pentru nodul curent las proiectiile candidate care se afla pe strada cu acelasi sens de mers ca 
			 * masina( sensul dat din trace-ul respectiv ). Aceasta situatie este posibila in momentul in care 
			 * unghiul format de directia de mers a masinii in trace si directia strazii este < 90 grade.
			 * 		Obs. Trebuie tratat special cazul pentru primul nod din trace, deoarece pentru aceasta nu avem
			 * un nod anterior trebuie sa luam nodul urmator din trace. 
			 */

			for( Iterator <Map.Entry<Integer, Vector<LocationParse>>> it = candidates.entrySet().iterator();it.hasNext(); ){
				Map.Entry<Integer, Vector<LocationParse>> aux = it.next();

				/* Vectorul cu posibile noduri candidate */
				ids = aux.getValue();
				if( ids.size()  <= 1 ){
					continue;
				}
				int idx = aux.getKey();

				/* crt  - este nodul curent din trace */
				if( idx == 0 && candidates.size()  > 1){
					prev = data.get( idx );
					crt = data.get( idx + 1);
				}
				else{
					prev = data.get( idx - 1);
					crt = data.get( idx );
				}

				Vector<LocationParse> new_cand = new Vector<LocationParse>();
				for( int i = 0; i < ids.size(); i++ ){
					LocationParse prj = ids.get( i );
					Way w = graph.get( prj.wid );
					//logger.info( "Strada " + w.id + "are oneway" + w.oneway );
					if( !w.oneway ){
						new_cand.add(prj);
						continue;
					}
					int indexNode = prj.indexNodes;
					Node crt_n = indexNode < w.nodes.size() -1 ? w.nodes.get(indexNode): w.nodes.get(indexNode -1);
					Node viit_n = indexNode < w.nodes.size() -1 ? w.nodes.get(indexNode +1 ): w.nodes.get(indexNode);
					double unghi = getAngle( crt.lat - prev.lat,  crt.lon - prev.lon, 
							viit_n.lat - crt_n.lat, viit_n.lon - crt_n.lon );
					crt_n = w.nodes.get(indexNode - 1 );
					viit_n = w.nodes.get(indexNode );
					double unghi2 = getAngle( crt.lat - prev.lat,  crt.lon - prev.lon, 
							viit_n.lat - crt_n.lat, viit_n.lon - crt_n.lon );

					//logger.info( "Unghiu format pe randul lui " + idx + "-" + i+ " este " + unghi +" unghi 2" + unghi2);


					if(  unghi < 90 || unghi2  < 90 )
						new_cand.add(prj);



				}
				//logger.info( "Noii candidati \n" + new_cand );
				candidates.put( idx, new_cand);


			}
			//showLocCand(candidates, "sens" + fname);

			int nr_cand = 0;

			int iter = 0, iter_max = 3;



			do{

				nr_cand = 0;

				/* -----------------Faza 5---------------------- 
				 * 		In cadrul acestei faze elimin proiectiile candidate de pe strazile care sunt foarte apropiate.
				 * Daca un nod este rezolvat atunci si nodul care urmeaza poate sa fie  pe aceeasi strada ori pe o strada
				 * care se intersecteaza cu strada pe care se afla nodul rezolvat.
				 * */
				kvalues = candidates.entrySet().toArray();
				for( int i = 1; i < kvalues.length; i++){
					Vector<LocationParse> new_cand = new Vector<LocationParse>();
					Map.Entry<Integer,Vector<LocationParse>> prev_aux = (Map.Entry<Integer,Vector<LocationParse>>)kvalues[i-1];
					Map.Entry<Integer,Vector<LocationParse>> crt_aux = (Map.Entry<Integer,Vector<LocationParse>>)kvalues[i];
					if( prev_aux.getValue().size() > 1 )
						continue;
					if( prev_aux.getValue().size() == 0 ){
						if( i > 2 && ((Map.Entry<Integer,Vector<LocationParse>>)kvalues[i-2]).getValue().size() == 1 )
							prev_aux = (Map.Entry<Integer,Vector<LocationParse>>)kvalues[i-2];
					}
					if( prev_aux.getValue().size() == 1 ){
						Way street = graph.get( prev_aux.getValue().get(0).wid );
						for( int j = 0; j < crt_aux.getValue().size(); j++ ){
							Long crt_cand_Sid = crt_aux.getValue().get(j).wid; 
							/* verific daca cele 2 puncte se afla pe aceeasi strada*/
							if(  crt_cand_Sid == street.id ){
								new_cand.add(crt_aux.getValue().get(j));
							}
							else{

								/* verific daca se afla pe strazi care se intersecteaza */
								for( Iterator<Map.Entry<Long,Vector<Long>>> itx = street.neighs.entrySet().iterator(); itx.hasNext(); ){
									Map.Entry< Long, Vector<Long>> ax = itx.next();
									/* Daca nu contine nodul de intersectie nu ma intereseaza */
									if( graph.get(crt_cand_Sid).nodes.contains(ax.getKey()) == false )
										continue;
									if( ax.getValue().contains( crt_cand_Sid ) ){
										if( new_cand.contains(crt_aux.getValue().get(j) ) == false )
											new_cand.add( crt_aux.getValue().get(j));
									}

								}
							}

						}
					}
					if( new_cand.size() != 0){
						candidates.put( crt_aux.getKey(), new_cand );
						//--------------------------
						crt_aux.setValue(new_cand);
						kvalues[i] = crt_aux;
						//--------------------------
					}

				}
				/*if( iter == iter_max -1 ){
						ostream = new FileOutputStream( "dupa_faza5_" + fname );

						outbr = new BufferedWriter(new OutputStreamWriter(ostream));
							for( Iterator <Map.Entry<Integer, Vector<LocationParse>>> it = candidates.entrySet().iterator(); it.hasNext(); ){
								Map.Entry<Integer, Vector<LocationParse>> aux = it.next();
											outbr.write( aux.getKey() +" " + aux.getValue()+"\n" );
							}
						outbr.close();
					}*/

				/* Faza 6 
				 *  In cadrul acestei faze se  cauta un nod care are deja o solutie si urmatorul nod care are la fel o solutie 
				 *  si daca nodurile respective sunt pe aceeasi strada inseamna ca si nodurile dintre ele tb sa fie pe aceeasi strada.
				 * */

				kvalues = candidates.entrySet().toArray();
				int start_poz = -1;
				int skip = 0, bkp_poz = 0;

				for( int i = 1; i < kvalues.length; i++){
					Map.Entry<Integer,Vector<LocationParse>> crt_aux = (Map.Entry<Integer,Vector<LocationParse>>)kvalues[i];
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
							Map.Entry<Integer,Vector<LocationParse>> prev_aux = (Map.Entry<Integer,Vector<LocationParse>>)kvalues[start_poz];
							long c_street_id = crt_aux.getValue().get(0).wid;
							long p_street_id = prev_aux.getValue().get(0).wid ;

							if(  c_street_id == p_street_id ){

								/* Atunci toate nodurile tb sa fie pe aceeasi strada.
								 * Daca nu gasesc un candidat propice ii calculam o proiectie pe strada respectiva*/

								//logger.info( "Am gasit nod egal i:" +  i +  "  - j: " + (start_poz+ 1) );
								for( int j = start_poz + 1; j < i; j++ ){
									Map.Entry<Integer,Vector<LocationParse>> aux = (Map.Entry<Integer,Vector<LocationParse>>)kvalues[j];
									Vector <LocationParse>cand = aux.getValue();
									int h;
									double dist = Integer.MAX_VALUE;
									int idx = -1;
									LocationParse orig = data.get(aux.getKey());
									for( h = 0; h < cand.size(); h++ ){
										LocationParse c = cand.get(h);
										if( c.wid == c_street_id ){
											Vector<LocationParse> new_c = new Vector<LocationParse>();
											new_c.add(c);
											candidates.put( aux.getKey(), new_c );
											crt_aux.setValue(new_c);
											kvalues[i] = crt_aux;
											break;
										}
										/*else{
												// acesta era cazul in care luam cel mai apropiat nod de drumul cel bun 
												double aux_dist = distance( orig.lat, orig.lon, c.lat, c.lon);
												if( aux_dist < dist ){
													idx = h;
													dist = aux_dist;
												}
											}*/
									}
									/* Elimin candidatii care sunt mai departati de 20 de metri */
									if( h == cand.size() ){

										Vector<LocationParse> new_c = new Vector<LocationParse>();
										Way w = graph.get(c_street_id);
										Vector<Node> ita = w.nodes;
										Node prev_n = ita.get(0);
										Node crt_n;
										LocationParse c_aux = data.get(aux.getKey());
										for( int g = 1; g < ita.size(); g++ ){
											crt_n = ita.get(g);
											LocationParse nd_ax = getProjection( c_aux, prev_n, crt_n );
											if( distance( c_aux.lat, c_aux.lon, nd_ax.lat, nd_ax.lon )  <= 20){
												nd_ax.setIdStreet( w.id );
												/* Seteaza indexul unde s-a gasit o proiectie mai mica decat 15 m */
												nd_ax.setIndexNodes( j );
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
					Map.Entry<Integer,Vector<LocationParse>> crt_aux = (Map.Entry<Integer,Vector<LocationParse>>)kvalues[i];
					if( crt_aux.getValue().size() > 1 )
						nr_cand++;
					else{
						if( crt_aux.getValue().size()  == 1 )
							data.set(crt_aux.getKey(), crt_aux.getValue().get(0));
					}
				}
				iter++;
				//logger.info( iter + " Numarul de candidati nerezolvati " + nr_cand);
			}
			while( nr_cand > 100 &&  iter < iter_max );

			/* ostream = new FileOutputStream( "dupa_panainmicfaza6_" + fname  );

				 outbr = new BufferedWriter(new OutputStreamWriter(ostream));
				 for( Iterator <Map.Entry<Integer, Vector<LocationParse>>> it = candidates.entrySet().iterator(); it.hasNext(); ){
						Map.Entry<Integer, Vector<LocationParse>> aux = it.next();
						outbr.write( aux.getKey() +" " + aux.getValue()+"\n" );
				}
				outbr.close();*/


			/* Pentru toate celelalte aleg nodul cel mai apropiat  */

			kvalues = candidates.entrySet().toArray();

			for( int j = 0; j < kvalues.length; j++ ){
				Map.Entry<Integer,Vector<LocationParse>> p_aux = null , n_aux = null , aux = null ;
				if( j > 1 )
					p_aux = (Map.Entry<Integer,Vector<LocationParse>>)kvalues[j - 1 ];
				if( j < kvalues.length - 1 )
					n_aux = (Map.Entry<Integer,Vector<LocationParse>>)kvalues[j + 1 ];

				aux = (Map.Entry<Integer,Vector<LocationParse>>)kvalues[j];
				Vector <LocationParse>cand = aux.getValue();
				if( cand.size() == 0)
					continue;



				if( j > 1 && j < kvalues.length - 1 ){

					if( p_aux.getValue().size() == 1 && n_aux.getValue().size() == 1 ){
						if( p_aux.getValue().get(0).wid == n_aux.getValue().get(0).wid ){
							Vector<LocationParse> new_c = new Vector<LocationParse>();
							Way w = graph.get(p_aux.getValue().get(0).wid);
							Vector<Node> ita = w.nodes;
							Node prev_n = ita.get(0);
							Node crt_n;

							LocationParse c_aux = data.get(aux.getKey());
							for( int g = 1; g < ita.size(); g++ ){

								crt_n = ita.get(g);
								LocationParse nd_ax = getProjection( c_aux, prev_n, crt_n );
								if( distance( c_aux.lat, c_aux.lon, nd_ax.lat, nd_ax.lon )  <= 15){
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
				double dist = Integer.MAX_VALUE;
				int idx = -1;
				LocationParse orig = data.get(aux.getKey());
				for( int  h = 0; h < cand.size(); h++ ){
					LocationParse c = cand.get(h);
					if( c.ontheStreetFirst ){
						idx = h;
						break;
					}

					double aux_dist = distance( orig.lat, orig.lon, c.lat, c.lon);
					if( aux_dist - dist <= 0 ){
						idx = h;
						dist = aux_dist;
					}

				}
				if( idx != -1 ){
					Vector<LocationParse> new_c = new Vector<LocationParse>();
					new_c.add(cand.get(idx));
					candidates.put( aux.getKey(), new_c );
				}
			}
			ostream = new FileOutputStream( resultsCorectionAlg + fname  );
			outbr = new BufferedWriter(new OutputStreamWriter(ostream));
			kvalues = candidates.entrySet().toArray();
			//for( int i = kvalues.length - 1; i >=0; i-- ){
				for( int i = 0; i <= kvalues.length - 1; i++ ){
					Map.Entry<Integer, Vector<LocationParse>> aux = (Map.Entry<Integer, Vector<LocationParse>>)kvalues[i];
					if(  aux.getValue().size() == 1 ){
						LocationParse rez = aux.getValue().get(0);
						outbr.write( rez.lat+" "+rez.lon+" "+rez.occupied +" "+ rez.timestamp +" "+rez.wid + "\n" );
					}
				}
				outbr.close();
				logger.warning( "Timpul algoritmului " + ((System.currentTimeMillis()- time_start)/1000));

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/* Functie care corecteaza traceurile pentru un cabs pe baza grafului de strazi */
	public static void getCorrectTraceCabs( ){
		/* se incarca graful in memorie */
		logger.info("Se incarca graful ");

		long time_start = System.currentTimeMillis();

		/*graph = load_graph( "C:\\Users\\Iceman89\\workspace\\VNView\\Xml\\streets_rez_san-francisco.osm", 
					"C:\\Users\\Iceman89\\workspace\\VNView\\Xml\\streets_graph_san-francisco.osm" );*/
		graph = load_graph( resultsPartialPath + "\\" + streetsFileName, 
				resultsPartialPath + "\\" + graphPartialFileName );

		long time_stop = System.currentTimeMillis();
		logger.info(""+(time_stop - time_start)/1000 );
		logger.info( " Caracteristici harta (" + map_lon_min +", "+map_lon_max +") " + " ("+map_lat_min +", "+" " + map_lat_max +" )");
		logger.info("S-a terminat incarcarea grafului ");

		areas = load_areas( resultsPartialPath + "\\" + indexTableFileName );

		parse_data(tracesListPath, 1, 0);    
	}

	public static void initDataDijksttra( long start_id, TreeMap<Long,Long> path, TreeMap<Long,Long> distance, int depthMax ){
		if( depthMax  == 0 )
			return;
		Way w = graph.get( start_id );
		if( w == null )
			return;
		for( Iterator<Vector<Long>> it = w.neighs.values().iterator(); it.hasNext();){
			Vector<Long> neigh = it.next();
			for( int i = 0; i < neigh.size(); i++ ){
				Long n_id = neigh.get(i);
				if( !path.containsKey( n_id )){
					path.put( n_id, -1L);
					// O valoare foarte mare insemna inf
					distance.put(n_id, 320000L);
					initDataDijksttra(n_id, path, distance, depthMax - 1 );
				}
			}
		}

	}

	public static LinkedList<Long> FindPath( long start_id, long stop_id, int depthMax ){
		LinkedList <Long> aux = new LinkedList <Long>();
		Long crt, dist, nr = 0l;
		LinkedList<Long> q = new LinkedList<Long>();
		TreeMap<Long,Long> path = new TreeMap<Long,Long>();
		Vector<Long> neighs = new Vector<Long>();
		/* Consider ca fiind pondere numarul de noduri de pe o strada */
		TreeMap<Long,Long> distance = new TreeMap<Long,Long>();

		initDataDijksttra( start_id, path, distance, depthMax + 1);
		crt = path.get( stop_id );
		if( crt == null )
			return aux;

		//logger.info( "Start id " + start_id +" stop_id "+stop_id ); 
		distance.put(start_id, 0l);
		q.addLast(start_id);

		while( !q.isEmpty() ){
			if( nr == depthMax -1 )
				break;

			crt = q.getFirst();
			if( crt == stop_id )
				break;
			dist = distance.get(crt);
			neighs = graph.get(crt).getAllNeighbors();
			for( int i = 0; i < neighs.size(); i++ ){
				Long ng = neighs.get(i);
				long newd = dist + graph.get(ng).nodes.size();
				Long oldd= distance.get(ng);
				if( oldd == null )
					continue;
				if( newd < oldd ){
					distance.put( ng, newd );
					path.put( ng, crt);
					if( !q.contains(ng))
						q.addLast( ng );
				}
			}
			nr++;
			q.poll();

		}
		//logger.info( path );
		crt = stop_id;
		aux.addFirst(crt);
		crt = path.get( crt );
		if( crt == -1 || crt == null){
			aux.clear();
			return aux;
		}
		while( crt != -1 ){
			aux.addFirst(crt);
			crt = path.get( crt );
		}
		return aux;
	}


	/**
	 *  Functie care efectueaza algoritmul de interpolare asupra unui trace
	 * @param fname - numele fisierului
	 * @param DeltaT - intervalul de timp la care se efectueaza discretizarea.
	 */
	public static  void InterpolAlgorithm( String fname, int DeltaT ){

		long start_t = System.currentTimeMillis();

		LinkedList<LocationParse> data = readTraceData( resultsCorectionAlg, fname );


		if( data == null ){
			System.err.println("Eroare citire trace " + fname );
			return;
		}
		Collections.reverse(data);


		//		//------------------------------------------------------------------
		LinkedList<LocationParse> new_data = new LinkedList<LocationParse>();
		long first_time = time_start_traces.first().getTime();
		long max_limit = 4;
		long crt_first_tm = data.get(0).timestamp; 
		long diff = 0, rem = ( crt_first_tm - first_time ) % DeltaT;
		if(  rem == 0 || rem <= DeltaT/2){
			diff = ( crt_first_tm - first_time ) / DeltaT; 
		}
		else{
			if( rem > DeltaT/2 )
				diff = (crt_first_tm - first_time ) / DeltaT + 1;
		}

		long  crt_time_trace =  first_time + diff * DeltaT;  
		first_time = crt_time_trace;
		//logger.info( "Dimensiune puncte in trace "+ data.size() + "pentru fisierul " +  fname );

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

		//logger.info(" Primul timp " + first_time + "   timp orig " + crt_first_tm +" Timpul  nou calc " + crt_time_trace);
		LocationParse crt = data.get(0);
		LocationParse prev = crt;

		//logger.info( "timp inainte de modificare " + prev.timestamp );
		LocationParse new_l;
		new_l = new LocationParse(crt.lat, crt.lon, crt.occupied, crt.timestamp);
		new_l.setIdStreet(crt.wid);
		/* Sa face distributia uniforma a punctelor */
		new_l.timestamp = crt_time_trace;
		new_data.add( new_l );

		for( int i = 1; i < data.size(); i++ ){
			crt = data.get(i);
			new_l = new LocationParse(crt.lat, crt.lon, crt.occupied, crt.timestamp);
			new_l.setIdStreet(crt.wid);
			diff = crt.timestamp - prev.timestamp;
			//logger.info("Diferenta de timp este  "+ diff );
			if( diff <= DeltaT/4  ){
				prev = crt;
				continue;
			}
			/* Orice este peste deltaT incerc sa il calibrez la distantare de multiplu de 
			 * deltaT. Doar in cazul celor care nu sar peste 3*DeltaT.
			 */

			if( diff >  DeltaT ){
				rem = diff%DeltaT;
				if( rem == 0 || rem < DeltaT/2 || diff/DeltaT == 2  )
					crt_time_trace += DeltaT * ((int) diff/DeltaT);
				else
					if( diff/DeltaT >= max_limit || diff/DeltaT == 1 )
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



		/*
		logger.info(" Faza de aliniere s-a terminat");

		try {
			FileOutputStream ostream;
			ostream = new FileOutputStream( "preliminare1_"+fname );

			BufferedWriter outbr = new BufferedWriter(new OutputStreamWriter(ostream));
			for( int i = 0; i < new_data.size(); i++ ){
				LocationParse l = new_data.get(i);
				outbr.write( l.lat +" "+l.lon +" " + l.occupied +" "+ l.timestamp+"\n" );
			}
			outbr.close();


		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 */
		data.clear();
		prev = new_data.get(0);
		data.add( prev );
		//		logger.info( "Size pentru noul " + new_data.size());
		crt_time_trace = first_time;
		for( int i = 1; i < new_data.size();i++){
			crt = new_data.get(i);
			diff = crt.timestamp  - prev.timestamp;
			if( diff == DeltaT || diff >= max_limit* DeltaT ){
				/* Daca diferenta dintre cele 2 noduri este deltaT inseamna ca este rezolvat
				 *  altfel daca diff >= max_limit* DeltaT  atunci se amana si rezolvarea acestuia.*/
				crt_time_trace += diff;
				new_l = new LocationParse( crt.lat , crt.lon, crt.occupied, crt_time_trace );
				new_l.setIdStreet(crt.wid);
				data.add( new_l );

				prev = crt;
				continue;
			}
			else{
				if( diff > DeltaT && diff < max_limit * DeltaT ){
					/* Determinam numarul de puncte intermediare de generat */
					long nr_pct = diff/DeltaT;
					/* Daca cele 2 puncte intre care exista gapul se afla pe aceeasi strada */
					if( prev.wid == crt.wid ){
						/*double step_Lat = Math.abs( crt.lat - prev.lat )/nr_pct;
						double step_Lon = Math.abs( crt.lon - prev.lon )/nr_pct;*/
						double step_Lat = ( crt.lat - prev.lat )/nr_pct;
						double step_Lon = ( crt.lon - prev.lon )/nr_pct;
						//logger.info( "Id strada " + prev.wid);
						Vector<Node> nds = graph.get(prev.wid).getNodesFromA2B(prev, crt);
						int dim_Nds = nds.size();
						int step = dim_Nds/(int)(nr_pct+1);
						//logger.info( " Numar punct "+ nds.size() +" dimNds " + dim_Nds + "pas " + step +" Nr pct " + nr_pct );
						for( int j = 0; j < nr_pct; j++ ){
							crt_time_trace += DeltaT;
							if( dim_Nds <= 2 || step == 0)
								new_l = new LocationParse( prev.lat + (j + 1)* step_Lat, prev.lon + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );
							else{
								new_l = new LocationParse( prev.lat + (j + 1)* step_Lat, prev.lon + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );
								new_l = getProjection( new_l, nds.get(j * step), nds.get( ( j + 1 )*step ) );
								new_l.timestamp = crt_time_trace;
							}
							new_l.setIdStreet(prev.wid);
							data.add( new_l );
						}


					}
					else{
						Way w = graph.get( prev.wid );
						/* De intrebat situatia cu Intersectia daca nu este mai ok sa 
						 * o lasam neinterpolata 
						 */

						Node join_nd = getIntersectNode( prev.wid, crt.wid );
						if( join_nd != null ){
							//logger.info("Puncte la intersectie");
							double dist_before_junction = distance(prev.lat, prev.lon, join_nd.lat, join_nd.lon);
							double dist_after_junction = distance( crt.lat, crt.lon, join_nd.lat, join_nd.lon);
							int imp = 0;

							// Daca este un numar impar de noduri  atunci se seteaza 
							// nodul lipsa chiar in punctul de intersectie.

							if( nr_pct % 2 == 1){
								imp = 1;
								nr_pct--;
							}
							int ctr = 1;
							if( nr_pct == 0){
								crt_time_trace += DeltaT;

								new_l = new LocationParse( join_nd.lat, join_nd.lon, prev.occupied, crt_time_trace );
								data.add( new_l );
							}
							if( nr_pct != 0 && nr_pct % 2 == 0 ){
								long nr_pct_before, nr_pct_after;
								if( dist_before_junction > dist_after_junction){
									long raport = (long)(dist_before_junction/dist_after_junction);
									if( raport == 1 ){
										nr_pct_before =	nr_pct_after = nr_pct/2;
									}
									else{
										long alocate = (raport - 1) % nr_pct;
										nr_pct_before =   alocate + ( nr_pct - alocate ) /2; 
										nr_pct_after = ( nr_pct - alocate ) % 2 == 0 ? ( nr_pct - alocate ) : 
											( nr_pct - alocate ) + 1;
									}

								}
								else{
									long raport = (long)(dist_after_junction/dist_before_junction);
									if( raport == 1 ){
										nr_pct_before =	nr_pct_after = nr_pct/2;
									}
									else{
										long alocate = (raport - 1) % nr_pct;
										nr_pct_after =   alocate + ( nr_pct - alocate ) /2; 
										nr_pct_before = ( nr_pct - alocate ) % 2 == 0 ? ( nr_pct - alocate ) : 
											( nr_pct - alocate ) + 1;
									}
								}


								double step_Lat = ( join_nd.lat - prev.lat )/ nr_pct_before;
								double step_Lon = ( join_nd.lon - prev.lon )/nr_pct_before;

								Vector<Node> ndsBef = graph.get(prev.wid).getNodesFromA2B(prev, join_nd);
								int dim_Nds_Before = ndsBef.size();

								Vector<Node> ndsAfter = graph.get(crt.wid).getNodesFromA2B(join_nd, crt);
								int dim_Nds_After = ndsBef.size();

								int step = dim_Nds_Before/(int)(nr_pct_before+1);		
								for( int j = 0; j < nr_pct_before; j++){
									crt_time_trace += DeltaT;
									//new_l = new LocationParse( prev.lat + (j + 1)* step_Lat, prev.lon + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );


									if( dim_Nds_Before <= 0 || step == 0)
										new_l = new LocationParse( prev.lat + (j + 1)* step_Lat, prev.lon + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );
									else{
										new_l = new LocationParse( prev.lat + (j + 1)* step_Lat, prev.lon + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );
										new_l = getProjection( new_l, ndsBef.get(j * step), ndsBef.get( ( j + 1 )*step - 1) );
										new_l.timestamp = crt_time_trace;
									}
									new_l.setIdStreet(prev.wid);
									data.add( new_l );
									ctr++;
									if( imp == 1&& ctr  == (nr_pct+1)/2 ){
										crt_time_trace += DeltaT;
										new_l = new LocationParse( join_nd.lat, join_nd.lon, prev.occupied, crt_time_trace );
										data.add( new_l );
									}
								}
								step_Lat = ( crt.lat - join_nd.lat )/nr_pct_after;
								step_Lon = ( crt.lon - join_nd.lon )/nr_pct_after;
								step = dim_Nds_After - 2 /(int)(nr_pct_after+1);	
								for( int j = 0; j < nr_pct_after; j++){
									crt_time_trace += DeltaT;
									//new_l = new LocationParse( join_nd.lat + (j + 1)* step_Lat, join_nd.longit + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );
									if( dim_Nds_After <= 0 || step == 0 )
										new_l = new LocationParse( join_nd.lat + (j + 1)* step_Lat, join_nd.lon + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );
									else{
										new_l = new LocationParse( join_nd.lat + (j + 1)* step_Lat, join_nd.lon + ( j + 1) * step_Lon, prev.occupied, crt_time_trace );
										new_l = getProjection( new_l, ndsAfter.get(j * step), ndsAfter.get( ( j + 1 )*step ) );
										new_l.timestamp = crt_time_trace;
									}
									new_l.setIdStreet(crt.wid);
									data.add( new_l );
									ctr++;
									if( imp == 1&& ctr  == (nr_pct+1)/2 ){
										crt_time_trace += DeltaT;
										new_l = new LocationParse( join_nd.lat, join_nd.lon, prev.occupied, crt_time_trace );
										data.add( new_l );
									}
								}
							}


						}
						else{
							/* Daca nu sunt pe aceeasi strada trebuie gasita calea catre ea*/
							LinkedList<Long> path = FindPath(prev.wid, crt.wid, 5);

							if( path.size() != 0 ){
								//loger.info( "Calea intre " + prev.wid +" "+ crt.wid+" este "+ path);
								long prev_id = prev.wid;
								for( int hj = 1; hj < path.size(); hj++ ){
									Node join_node = getIntersectNode( prev_id, path.get(hj));
									if( join_node == null )
										break;
									crt_time_trace += DeltaT;
									new_l = new LocationParse( join_node.lat, join_node.lon, prev.occupied, crt_time_trace );
									data.add( new_l );
									if( hj < path.size() -1 ){
										Node join_next_nd = getIntersectNode( path.get(hj), path.get(hj+1));
										if( join_next_nd != null ){
											double step_Lat = ( join_next_nd.lat - join_node.lat )/2;
											double step_Lon = ( join_next_nd.lon - join_node.lon )/2;
											crt_time_trace += DeltaT;
											new_l = new LocationParse( join_node.lat + step_Lat, join_node.lon + step_Lon, prev.occupied, crt_time_trace );
											Vector<Node> nds = graph.get(path.get(hj)).getNodesFromA2B( join_node, join_next_nd );
											if( nds != null){
												if( nds.size() >= 3){
													new_l = getProjection( new_l, nds.get( nds.size()/2 -1 ), nds.get( nds.size()/2 + 1 )  );
													new_l.timestamp = crt_time_trace;
												}
											}
											data.add( new_l );
										}
									}
									prev_id = path.get(hj);

								}

							}
						}
					}
					prev = crt;
					crt_time_trace += DeltaT;
					new_l = new LocationParse( crt.lat , crt.lon, crt.occupied, crt_time_trace );
					new_l.setIdStreet(crt.wid);
					data.add( new_l );


				}

			}		

		}

		//Collections.reverse(data);
		try {
			FileOutputStream ostream;
			ostream = new FileOutputStream( resultsInterpolationAlg + fname );

			BufferedWriter outbr = new BufferedWriter(new OutputStreamWriter(ostream));
			int nr_p_route = 0;
			int Nactive = 0;
			for( int i = 0; i < data.size(); i++ ){
				if( tip_parsare == 1 ){
					if( nr_p_route == 0 ){
						if( Nactive == 1 ){
							nr_p_route = new Random().nextInt(300)+ 5;
							Nactive = 0;
						}
						else{
							nr_p_route = new Random().nextInt(100)+ 5;
							Nactive = 1;
						}
					}
				}
				LocationParse l = data.get(i);
				long d = tip_parsare == 1?Nactive : l.occupied;
				nr_p_route--;
				outbr.write( l.lat +" "+l.lon +" " + d +" "+ l.timestamp+ " " + l.wid +"\n" );
			}
			outbr.close();

			logger.warning( "Timp pentru " + fname +" "+(System.currentTimeMillis() - start_t)/1000 );
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	/* Functie care interpoleaza punctele dintr-un trace tinand cont de trace-uri.
	 * Acesta o sa opereze in 2 moduri:
	 * a. primul mod este cel in care se interpoleaza un set de date dintr-un
	 * trace in care nu se cunoaste pe ce strada se afla un punct.
	 * b. al doilea mod este cel in care se interpoleaza un set date dintr-un
	 * trace prin care a fost trecut prin algorimult de corectie.
	 */
	public static void getInterpolTraceCabs( int DeltaT ){
		/* se incarca graful in memorie */
		logger.info("Se incarca graful ");

		long time_start = System.currentTimeMillis();

		/*graph = load_graph( "C:\\Users\\Iceman89\\workspace\\VNView\\Xml\\streets_rez_san-francisco.osm", 
					"C:\\Users\\Iceman89\\workspace\\VNView\\Xml\\streets_graph_san-francisco.osm" );*/
		graph = load_graph( resultsPartialPath + "\\" + streetsFileName, 
				resultsPartialPath + "\\" + graphPartialFileName );
		long time_stop = System.currentTimeMillis();

		logger.info(""+ (time_stop - time_start)/1000 );
		logger.info( " Caracteristici harta (" + map_lon_min +", "+map_lon_max +") " + " ("+map_lat_min +", "+" " + map_lat_max +" )");
		logger.info("S-a terminat incarcarea grafului ");

		areas = load_areas( resultsPartialPath + "\\" + indexTableFileName );
		time_start_traces = loadTimeStartFile(resultsPartialPath + "\\times_traces");
		parse_data( resultsCorectionAlg, 2, DeltaT );
	}
	/* Functie care incarca in memorie caile catre fisierele necesare */
	public static void loadProperties(){
		Properties prop = new Properties();

		try {

			//load a properties file
			File f = new File("src/" + ConfigFileName  );
			String absPath = f.getAbsoluteFile().toString();
			//rootPath = absPath.substring(0,absPath.lastIndexOf( ConfigFileName ) -1 );

			prop.load( new FileInputStream(f) );

			mapPath = /*rootPath + */prop.getProperty("mapPath") ;
			tracesListPath = /*rootPath + */ prop.getProperty("tracesPath");
			resultsPartialPath = /*rootPath + */ prop.getProperty("resultsPartial");
			resultsCorectionAlg = prop.getProperty("resultsCorectationAlg");
			resultsInterpolationAlg = prop.getProperty("resultsInterpolationAlg");
			graphPartialFileName = prop.getProperty("graphPartialFileName");
			streetsPartialFileName = prop.getProperty("streetsPartialFileName");
			streetsFileName = prop.getProperty("streetsFileName");
			indexTableFileName = prop.getProperty("indexTableFileName");
			dir_diff_time  = prop.getProperty("diff_dir");
			routeTimeData = prop.getProperty("routeTimeData");
			File dir_res  = new File( resultsPartialPath );
			if( dir_res.exists() ){
				if( !dir_res.isDirectory()){
					logger.warning( resultsPartialPath + "  nu este director. Trebuie sters pentru a merge ");
				}

			}
			else{
				if( dir_res.mkdir() ){
					logger.info(" Directorul pentru rezultate partiale a fost creat.");
				}
				else 
					logger.warning(" Eroare la crearea directorului pentru rezultate partiale.");
			}


		} catch (IOException ex) {
			logger.warning(" Eroare loading properties file ");
			ex.printStackTrace();
		}
	}
	
	/* Generate a grid of markers for the Google Map ( goal : debug ) */ 
	public static void generateGridDebugging( String fname ){
		FileOutputStream ostream;
		BufferedWriter outbr = null ;


		String line;
		FileInputStream fstream;
		try {
			fstream = new FileInputStream( resultsPartialPath + "\\" + streetsFileName );

			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			ostream = new FileOutputStream( fname );
			outbr = new BufferedWriter(new OutputStreamWriter(ostream));

			line = br.readLine();
			if( line == null )
				return ;
			if( line.contains("map_lat_min") ){
				String [] ws = line.split(" ");
				map_lat_min = Double.parseDouble( ws[1] );
				map_lon_min = Double.parseDouble( ws[3] );
				map_lat_max = Double.parseDouble( ws[5] );
				map_lon_max = Double.parseDouble( ws[7] );
			}
			
			//logger.info( map_lat_min +" "+ map_lon_min);
			/*int nr_cols = (int)distance( 0, map_lon_min, 0, map_lon_max )/SQUARE_LM;
			int nr_rows =  (int)distance( map_lat_min, 0, map_lat_max, 0 )/SQUARE_LM;*/
			//outbr.write( "<?xml version=\"1.0\"?>\n<markers>\n");

			int nr_cols, nr_rows;

			nr_cols = (int)Math.ceil((map_lon_max - map_lon_min)/SQUARE_L) + 1;
			nr_rows = (int)Math.ceil((map_lat_max - map_lat_min )/SQUARE_L) + 1;
			logger.info( nr_cols +" " + nr_rows );
			double lat, lon, lat2, lon2;
			for( int i = 0; i < nr_rows; i++ ){
				for( int j = 0; j < nr_cols; j++){
					lat = map_lat_min + i * SQUARE_L;
					lon = map_lon_min + j * SQUARE_L;
					String s_google = "<marker>\n<name>"+i* nr_cols + j+"</name>\n<address>A</address>\n"+
							"<lat>"+lat+"</lat>\n"+
							"<lng>"+lon+"</lng>\n</marker>\n";


					//if( nr_rows - 400 < i && nr_rows -300 >= i){
					String s = lat +" "+ lon+"\n";
					outbr.write( s );
					//}

				}

			}

			//outbr.write( "</markers>\n");
			outbr.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/* Functie care genereaza un fisier xml pentru scriptul de punere a markerilor pe google maps*/
	public static void generateMarkerFile( String url, String fname, int interval, int tip){

		FileOutputStream ostream;
		BufferedWriter outbr = null ;

		LinkedList<LocationParse> data;
		if( tip == 0 )
			data = readTraceData(url, fname);
		else 
			data = readTraceDataBeijing(url, fname);
		try {

			int index = fname.lastIndexOf(".");
			String fn_rez = "markers_" + fname.substring(0,index)+".xml";


			long time_start = data.get(0).timestamp;

			ostream = new FileOutputStream( fn_rez );
			outbr = new BufferedWriter(new OutputStreamWriter(ostream));
			outbr.write( "<?xml version=\"1.0\"?>\n<markers>\n");


			for( int i = 0;  i < data.size(); i++ ){
				LocationParse c = data.get(i);
				String s = "</name>\n<address>A</address>\n"+
						"<lat>"+c.lat+"</lat>\n"+
						"<lng>"+c.lon+"</lng>\n</marker>\n";
				outbr.write( "<marker>\n<name>"+i+ s );
				/*if( c.timestamp - time_start > interval)
					break;*/
			}
			outbr.write( "</markers>\n");
			outbr.close();


		} catch (FileNotFoundException e) {
			logger.warning( " Eroare deschidere fisier cu "+ fname);
			e.printStackTrace();
		} catch (IOException e) {
			logger.warning( "Eroare operatii efectuate pe fisierul" + fname );
			e.printStackTrace();
		}

	}
	/* Function used only for debugging */
	public static void getTracesInaRange(double min_lat, double max_lat, double min_lon, double max_lon ){

		File dir = new File("H:\\Licenta\\Geolife Trajectories 1.2\\Geolife Trajectories 1.2\\Data");
		try {
			BufferedWriter output = new BufferedWriter( new OutputStreamWriter( new FileOutputStream("outputGeo")));		

			for( File d : dir.listFiles()){
				logger.info( d.getName());
				for( File f : d.listFiles()[0].listFiles() ){
					try {
						RandomAccessFile in = new RandomAccessFile(f.getAbsolutePath(), "r");
						String s = null;
						int i = 0;
						while( i < 6){
							in.readLine();
							i++;
						}
						int ok = 1;	
						while( (s = in.readLine()) != null ){

							StringTokenizer st = new StringTokenizer(s, ",");
							double lat = Double.parseDouble(st.nextToken());
							double lg = Double.parseDouble(st.nextToken());
							if( lat < min_lat || lat > max_lat || lg < min_lon || lg > max_lon ){
								ok = 0;
								break;
							}
						}
						if( ok == 1 )
							output.write(d.getName() +"  "+ f.getName() +"\n ");

						in.close();
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
			output.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/* Determine the joints from traces */
	public static void getTracesJoints(){

		/* load the graph of the streets */
		graph = load_graph( resultsPartialPath + "\\" + streetsFileName, 
				resultsPartialPath + "\\" + graphPartialFileName );
		
		logger.info( " Caracteristici harta (" + map_lon_min +", "+map_lon_max +") " + " ("+map_lat_min +", "+" " + map_lat_max +" )");
		logger.info("S-a terminat incarcarea grafului ");

		areas = load_areas( resultsPartialPath + "\\" + indexTableFileName );
		parse_data( resultsInterpolationAlg, 4, 0 );
	}
	
	/* ---------------------------------------------------------------------------- */
	/* For each trace function keeps only the junctions */
	public static void TraceLayoutAlgorithm( String fname ){
		LinkedList<LocationParse> data;
		/* maximum distance between 2 nodes */
		float max_dist = 2000; /* 2000 m */
		data = readTraceData( tracesListPath, fname );

		if( data == null ){
			System.err.println("Eroare citire trace " + fname );
		}
		
		Collections.reverse(data);
		LocationParse crt, prev;
		logger.info( "Dimensiune puncte in trace "+ data.size() + "pentru fisierul " +  fname );
		prev = data.get(0);

		try {
			FileOutputStream ostream = new FileOutputStream( resultsInterpolationAlg + "joints_"+fname );
			BufferedWriter outbr = new BufferedWriter(new OutputStreamWriter(ostream));
/*START*/			
			FileOutputStream fosSpeeds = new FileOutputStream( resultsInterpolationAlg + "speeds_"+fname );
			BufferedWriter bwSpeeds = new BufferedWriter(new OutputStreamWriter(fosSpeeds));
			double maxSpeed = 37.5;
/*END*/			
			outbr.write("start " + prev.lat + " " + prev.lon + " " + prev.occupied + " " + prev.wid + " " + prev.timestamp + "\n");
		
			for( int i = 1; i < data.size(); i++ ){
				crt = data.get(i);
				
				if (prev.occupied != crt.occupied) {
					outbr.write("end " + prev.lat + " " + prev.lon + " " + prev.wid + " " + prev.timestamp + "\n");
					outbr.write("start " + crt.lat + " " + crt.lon + " " + crt.occupied + " " + crt.wid + " " + crt.timestamp + "\n");
					prev = crt;
					continue;
				}
				
				if (prev.wid == crt.wid) {
					prev = crt;
/*START*/
					double speed = distance(prev.lat, prev.lon, crt.lat, crt.lon) / (crt.timestamp - prev.timestamp);
					System.out.println(speed);
					if (0.0 < speed && speed <= maxSpeed)
						bwSpeeds.write(prev.wid + " " + speed + "\n");
/*END*/
					continue;
				}
				else {
					Node joint = getIntersectNode( prev.wid, crt.wid );
					
					if( joint != null ){
						outbr.write(joint.id + " " + joint.lat + " " + joint.lon + " " + joint.wayId + "\n" );
/*START*/
						double dist = distance(prev.lat, prev.lon, joint.lat, joint.lon) + distance(joint.lat, joint.lon, crt.lat, crt.lon);
						double speed = dist / (crt.timestamp - prev.timestamp);
						if (0.0 < speed && speed <= maxSpeed) {
							bwSpeeds.write(prev.wid + " " + speed + "\n");
							bwSpeeds.write(crt.wid + " " + speed + "\n");
						}
/*END*/
					}
					else{
						double dist = distance( prev.lat, prev.lon, crt.lat, crt.lon );
						if( dist < max_dist ){
							/*find the route between two points */
							LinkedList<Long> path = FindPath(prev.wid, crt.wid, 20);
							if (path.size() == 0) {
								outbr.write("skip no path\n");
								outbr.write("end " + prev.lat + " " + prev.lon + " " + prev.wid + " " + prev.timestamp + "\n");
								outbr.write("start " + crt.lat + " " + crt.lon + " " + crt.occupied + " " + crt.wid + " " + crt.timestamp + "\n");
							} else {
								double distanceNodes = 0.0;
								Node firstNode = null;
								Node lastNode = null;
								Node prevNode = null;
								for( int  j = 0; j < path.size() - 1; j++ ){
									Long wid1= path.get(j);
									Long wid2= path.get(j+1);
									joint = getIntersectNode( wid1, wid2 );
									outbr.write(joint.id + " " + joint.lat + " " + joint.lon + " " + joint.wayId + "\n" );
/*START*/									
									if (prevNode != null)
										distanceNodes += distance(prevNode.lat, prevNode.lon, joint.lat, joint.lon);
									if (j == 0)
										firstNode = joint;
									if (j == path.size() - 2)
										lastNode = joint;
									prevNode = joint;
								}
								distanceNodes += distance(prev.lat, prev.lon, firstNode.lat, firstNode.lon);
								distanceNodes += distance(lastNode.lat, lastNode.lon, crt.lat, crt.lon);
								double speed = distanceNodes / (crt.timestamp - prev.timestamp);
								if (0.0 < speed && speed < maxSpeed) {
									for (Long wayId : path) {
										bwSpeeds.write(wayId + " " + speed + "\n");
									}
								}
/*END*/
							}
						} else {
							outbr.write("skip too far " + dist + "\n");
							outbr.write("end " + prev.lat + " " + prev.lon + " " + prev.wid + " " + prev.timestamp + "\n");
							outbr.write("start " + crt.lat + " " + crt.lon + " " + crt.occupied + " " + crt.wid + " " + crt.timestamp + "\n");
						}
					}
					
					prev = crt;
				}
			}
			outbr.write("end " + prev.lat + " " + prev.lon + " " + prev.wid + " " + prev.timestamp + "\n");
			outbr.close();
//			bwSpeeds.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static private MapPoint getPoint(double lat, double lon, int isOccupied, SphericalMercator sm) {
		MapPoint point;
		PixelLocation pix = sm.LatLonToPixelLoc(lat, lon, Globals.zoomLevel);

		pix.tile.y -= 6177;
		pix.tile.x -= 13457;

		point = new MapPoint(pix, lat, lon,
				((isOccupied == 0) ? false : true), -1); // time doesn't matter
		return point;
	}
	
	/**
	 * Needs to be in sync with TraceLayoutAlgorithm.
	 */
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
					tokens = line.split(" ");
					latitude = Double.parseDouble(tokens[1]);
					longitude = Double.parseDouble(tokens[2]);
					isOccupied = Integer.parseInt(tokens[3]);
					wayId = Long.parseLong(tokens[4]);
					long time = Long.parseLong(tokens[5]);
					
					start = MapPoint.getMapPoint(latitude, longitude, isOccupied, wayId);
					start.timestamp = new Date(time * 1000);
					intersections = new ArrayList<Node>();
				} else if (line.startsWith("skip")) {
					/* skip this line
					   maybe do something else here */
				} else if (line.startsWith("end")) {
					long time = 0l;
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
						if( intersections.size() > 0)
						{
							aux = intersections.get( intersections.size() - 1 );
							if( end.lat == aux.lat && end.lon == aux.lon )
								intersections.remove(intersections.size() - 1);
						}
						if( intersections.size() > 0)
						{
							aux = intersections.get( 0 );
							if( start.lat == aux.lat && start.lon == aux.lon )
								intersections.remove( 0 );

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

	
	public static void main( String []args ){

		/* Se incarca linkurile utile */
		loadProperties();

		/* Se alege optiunea parsarului 
		 *  opt = 0 --> Se calculeaza pentru fiecare trace diferenta de timp 
		 *  la care au fost inregistrate cele 2 puncte.
		 *  opt = 1 --> se obtin strazile din graf eliminand din harta osm detaliile care 
		 *  nu sunt utile.
		 *  opt = 2 -->  Se obtin strazile si nodurile din care sunt alcatuite( un fel de graph partial )
		 *  opt = 3 -->  Se fac corecturile asupra traceurilor 
		 * */

		

		int opt = 10, DeltaT = 60;

		switch( opt ){
		case 0:
			getTimeIntervalAllCabs();
			break;
		case 1:
			getWays( mapPath, 1 );
			break;
		case 2:
			getStreetsData( mapPath, resultsPartialPath + "\\" + streetsPartialFileName );
			break;
		case 3:
			getCorrectTraceCabs( );
			break;
		case 4:
			getInterpolTraceCabs( DeltaT );
			break;
		case 5:
			/* In acest modul se determina toti timpii minime pentru traceurile*/
			parse_data( resultsCorectionAlg , 3, 0);
			break;
		case 6:
			getDiff("preliminare_new_enyenewl.txt", 2);
			break;

		case 7: 

			/* In acest modul se genereaza xml pentru markeri*/
			//generateMarkerFile("Crescatoare" ,"1024.txt", 3600, 0);
			generateMarkerFile(resultsCorectionAlg , "1024.txt", 3600, 0);
			break;
		case 8:

			generateGridDebugging("grid.xml");
			break;
		case 9:

			getTracesInaRange(39.41400, 40.42600,  115.68600, 117.11900);
			break;
		case 10:
			getTracesJoints( );
			break;



		}
	}
}
