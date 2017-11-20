package utils.tracestool.algorithms;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;

import utils.Pair;
import utils.tracestool.Utils;
import utils.tracestool.parameters.BetweennessCentralityParams;
import utils.tracestool.parameters.GenericParams;
import model.OSMgraph.Way;
import model.threadpool.ThreadPool;

class BC_PComputation1 implements Runnable {

	 /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(BC_PComputation1.class.getName());

	/* The graph of streets */
	TreeMap<Long, Way> graph;

	/* 
	 * The graph with the congestion weights: 
	 * key -   <street1, street2>   
	 * 			- street1 - represents the street which has already
	 * been crossed to get on street2.
	 *  
	 * value - <jointid, congestion_value>
	 * 			- congestion_value - are congestion level on street1 when driver tried to get
	 * 			on street2.
	 */
	TreeMap<Pair<Long,Long>, Pair<Long,Double>> weightGraph;

	public Lock lock;
	int nodeIdx;

	public BC_PComputation1( TreeMap<Long, Way> graph, TreeMap<Pair<Long,Long>, Pair<Long,Double>> weightGraph, int nodeIdx ) {
		this.graph = graph;
		this.weightGraph = weightGraph;
		this.nodeIdx = nodeIdx;
	}
	
	public void startComputeBetweennessCentralityP( )
	{
		/* Graph node's ID */
		Vector<Long> nodes = new Vector<Long>( graph.keySet() );
		int nodesNb = nodes.size();
		Long starttime1  = System.currentTimeMillis(), starttime;
		Stack<Integer> stack = new Stack<Integer>();
		ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<Integer>();
		Vector<Vector<Integer>> P = new Vector<Vector<Integer>>();
		Vector<Double> distance = new Vector<Double>();
		/* number of equal paths from current node */
		Vector<Long> NP = new Vector<Long>();
		Vector<Double> cb = new Vector<Double>();

		for( int j = 0; j < nodesNb; j++ )
		{
			boolean crt_node = ( nodeIdx == j );
			distance.add( (crt_node)? 0d : Double.MAX_VALUE );
			NP.add( (crt_node)? 1l: 0l );
			P.add( new Vector<Integer>() );
			cb.add( 0d );
		}
		queue.add(nodeIdx);

		starttime = System.currentTimeMillis();
		System.out.println("Before exploration");
		while( !queue.isEmpty() )
		{
			Integer idx_v = queue.poll();
			//System.out.println("Explore " + idx_v);
			Double dv = distance.get( idx_v );
			stack.push( idx_v );
			Way crtway = graph.get( nodes.get(idx_v) );
			for( Long w : crtway.getAllOutNeighbors( graph ) )
			{
				Integer idx_w = nodes.indexOf( w );
				Double olddw = distance.get( idx_w );
				Long vID = nodes.get(idx_v);

				Pair<Long,Double> tmp = weightGraph.get( new Pair<Long,Long>(vID, w) );
				Double weight = (tmp == null) ? BetweennessCentralityParams.defaultWeight :
												BetweennessCentralityParams.defaultWeight + tmp.getSecond();

				Double dvw = dv + weight;
				if( olddw > dvw )
				{
					distance.set( idx_w, dvw );
					if(!queue.contains(idx_w))
						queue.add( idx_w );
				}

				if( distance.get(idx_w) == dvw )
				{
					Long NPv = NP.get( idx_v );
					Long NPw = NP.get( idx_w );
					if( NPw != null && NPv != null )
					{
						NP.set( idx_w, NPv + NPw );
					}
					else
					{
						logger.info("Impossible to compute NP for " + idx_v +"-" + idx_w);
					}
					Vector<Integer> oldP = P.get( idx_w );
					oldP.add( idx_v );
					P.set( idx_w, oldP );
				}

			}
		}
		System.out.println( nodeIdx + " BFS_Stop_time: " + (System.currentTimeMillis() - starttime)/1000 );
		while( !stack.isEmpty() )
		{
			Integer idx_w = stack.pop();
			Long npw = NP.get(idx_w);
			Double cbw = cb.get(idx_w);
			for( Integer idx_v : P.get(idx_w) )
			{
				Double old_cbv = cb.get(idx_v);
				Long npv = NP.get(idx_v);
				Double new_cbv = old_cbv + (npv/npw)*(1+cbw);
				cb.set( idx_v, new_cbv );
			}
			if( idx_w != nodeIdx )
			{
				synchronized (BetweennessCentralityParallel.CB) {
					BetweennessCentralityParallel.CB.set(idx_w, BetweennessCentralityParallel.CB.get(idx_w) + cb.get(idx_w));		
				}
			}
		}
		System.out.println( nodeIdx + " Stop_time: " + (System.currentTimeMillis() - starttime1)/1000 );
	}

	@Override
	public void run() {
		startComputeBetweennessCentralityP();
	}
}

public class BetweennessCentralityParallel2 {
	 /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(BetweennessCentralityParallel2.class.getName());

	/* The graph of streets */
	static TreeMap<Long, Way> graph;
	static Vector<Double> CB = new Vector<Double>();

	/* 
	 * The graph with the congestion weights: 
	 * key -   <street1, street2>   
	 * 			- street1 - represents the street which has already
	 * been crossed to get on street2.
	 *  
	 * value - <jointid, congestion_value>
	 * 			- congestion_value - are congestion level on street1 when driver tried to get
	 * 			on street2.
	 */
	static TreeMap<Pair<Long,Long>, Pair<Long,Double>> weightGraph;

	/** Thread Pool reference */
	static ThreadPool threadPool;

	static double [] limits = new double[4];

	/* Determine Page Rank based on the cars movement layout */
	public static void determineBetweennessCentrality(){

		threadPool = ThreadPool.getInstance();

		logger.info( "Starting to determine the Betweenness Centrality" );

		graph = OSMGraph.load_graph( GenericParams.mapConfig.getStreetsFilename(), 
				 GenericParams.mapConfig.getPartialGraphFilename(), limits );

		weightGraph = Utils.readCongestionStatistics(GenericParams.mapConfig.getCongestionGraphFilename());

		startComputeBetweennessCentrality( );

		logger.info( "Finishing to determine the Betweenness Centrality" );

	}

	public static void startComputeBetweennessCentrality( )
	{
		int j;
		/* Graph node's ID */
		Vector<Long> nodes = new Vector<Long>( graph.keySet() );

		for( int i = 0; i < nodes.size(); i++ )
		{
			CB.add(0d);
		}

		for( j = 0; j < nodes.size(); j++ )
		{
			threadPool.submit( new BC_PComputation1(graph, weightGraph, j) );
		}

		threadPool.waitForThreadPoolProcessing();
		
		try {
			PrintWriter writer = null;
			writer = new PrintWriter(GenericParams.mapConfig.getInterpolateAlgResults() + "/" + GenericParams.mapConfig.getCity() + "_CBranking.txt", "UTF-8");

			Double max = Collections.max(CB);
			for( int i = 0; i < nodes.size(); i++ )
			{
				writer.println(nodes.get(i) +" "+String.format("%.10f",CB.get(i)/max));
			}
			writer.close();

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
	}
}
