package utils.tracestool.algorithms;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import utils.tracestool.Utils;
import utils.tracestool.parameters.GenericParams;
import utils.tracestool.parameters.PageRankParams;
import model.OSMgraph.Way;

public class PageRank {
	 /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(PageRank.class.getName());

	/* The graph of streets */
	static TreeMap<Long, Way> graph;

	/* The graph of street with ways */
	static TreeMap<Long, Long> weightGraph;
	
	static double [] limits = new double[4];

	/* Determine Page Rank based on the cars movement layout */
	public static void determinePageRank(){

		logger.info( "Starting to determine the Page Rank" );

		graph = OSMGraph.load_graph( GenericParams.mapConfig.getStreetsFilename(), 
				 GenericParams.mapConfig.getPartialGraphFilename(), limits );

		loadWeightGraph();

		startComputePageRank(PageRankParams.dumpFactor);

		logger.info( "Finishing to determine the Page Rank" );

	}

	/* build the graph based on the street visits */
	public static void loadWeightGraph()
	{
		/* Should get the ways weights from the logs. */
		BufferedReader reader = null;
		weightGraph = new TreeMap<Long,Long>();
		try {
			String line = null;
			reader = new BufferedReader(new FileReader("routingapp_statistics.txt"));
			while( (line = reader.readLine()) != null )
			{
				if( line.startsWith("#") )
					continue;
				String ws[] = line.split(" ");
				Long streetID = Long.parseLong(ws[0]);
				Long weight = Long.parseLong(ws[1]);
				weightGraph.put(streetID, weight);
			}
			reader.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void startComputePageRank( double dFactor )
	{

		/* Graph node's ID */
		Vector<Long> nodes = new Vector<Long>( graph.keySet() );
		/* initial PR */
		Vector<Double> PR = new Vector<Double>(nodes.size()), PR0 = new Vector<Double>(nodes.size());

		double constantF = (1-dFactor)/nodes.size();
		/* initialize PR with the value */
		for( long i = 0; i < nodes.size(); i++ )
		{
			PR0.add((1/(double)nodes.size()));
			PR.add((1/(double)nodes.size()));
		}

		PrintWriter writer = null;
		try {
			writer = new PrintWriter("pr0_statistics.txt", "UTF-8");

			writer.println(PR0);	

			writer.close();

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		for( int j = 0; j < PageRankParams.max_iter; j++ )
		{
			for( int i = 0; i < nodes.size(); i++ )
			{
				long crtStreetID = nodes.get(i);
				Way  street = graph.get(crtStreetID);
				double sum = 0;
				for( Long neighID : street.getAllInNeighbors(graph) )
				{
					Long weight = weightGraph.get(neighID);
					Way streetNeigh = graph.get(neighID);
					long neighborsNb = streetNeigh.getAllOutNeighbors(graph).size();

					/* Avoid OSM mapping errors */
					neighborsNb = neighborsNb == 0 ? 1 : neighborsNb; 
					weight = weight == null ? 1 : weight + 1;
					sum += weight * ( PR0.get(nodes.indexOf(neighID) ) / neighborsNb ); 

				}
				PR.set(i, constantF + dFactor * sum );
			}

			PR = Utils.normalize(PR);
			PR0 = (Vector<Double>) PR.clone();
		}
		
		try {
			writer = new PrintWriter("pr1_statistics.txt", "UTF-8");

			for( int i = 0; i < nodes.size(); i++ )
			{
				writer.println(nodes.get(i) +" "+PR0.get(i));	
			}
			writer.close();

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		
	}
}
