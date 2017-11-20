package utils.tracestool.algorithms;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import utils.Pair;
import utils.tracestool.Utils;
import utils.tracestool.parameters.BetweennessCentralityParams;
import utils.tracestool.parameters.GenericParams;
import model.OSMgraph.Way;
import model.threadpool.ThreadPool;

class BC_SSSP_Computation implements Runnable {

	 /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(BC_SSSP_Computation.class.getName());

	/* The graph of streets */
	TreeMap<Long, Way> graph;

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
	TreeMap<Pair<Long,Long>, Pair<Long,Double>> weightGraph;

	int nodeIdx;
	ReentrantLock lock;
	public BC_SSSP_Computation( TreeMap<Long, Way> graph, TreeMap<Pair<Long,Long>, Pair<Long,Double>> weightGraph, int nodeIdx, ReentrantLock lock ) {
		this.graph = graph;
		this.weightGraph = weightGraph;
		this.nodeIdx = nodeIdx;
		this.lock = lock;
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
		Integer[] unexploredNodes = new Integer[BetweennessCentralityParams.depthMax+3];
		unexploredNodes[0] = 1;
		for( int j = 0; j < nodesNb; j++ )
		{
			boolean crt_node = ( nodeIdx == j );
			distance.add( (crt_node)? 0d : Double.MAX_VALUE );
			NP.add( (crt_node)? 1l: 0l );
			P.add( new Vector<Integer>() );
			cb.add( 0d );
		}
		queue.add(nodeIdx);

		unexploredNodes[unexploredNodes[0]] = 1;
		/* 
		 * prepare the next level to count the nodes that will be
		 * explored on it. 
		 */
		unexploredNodes[unexploredNodes[0]+1] = 0;

		starttime = System.currentTimeMillis();
		System.out.println("Before exploration");
		while( !queue.isEmpty() )
		{
			if( unexploredNodes[0] == BetweennessCentralityParams.depthMax )
				break;

			Integer idx_v = queue.poll();
			unexploredNodes[unexploredNodes[0]]--;
			//System.out.println("Explore " + idx_v);
			Double dv = distance.get( idx_v );
			stack.push( idx_v );
			Way crtway = graph.get( nodes.get(idx_v) );
			crtway.getAllOutNeighbors( graph ).parallelStream().forEach(e->exploreNeighbors(e, idx_v, dv, distance, NP, nodes, P, queue,  unexploredNodes ));
			//System.out.println( "Noi vecini sunt" + queue );
			if( unexploredNodes[unexploredNodes[0]] == 0 )
			{
				/* 
				 * increment level in order to indicate the number of nodes that should be explored
				 * for current node.
				 */
				unexploredNodes[0]++;
				/* 
				 * prepare the next level to count the nodes that will be
				 * explored on it. 
				 */
				unexploredNodes[unexploredNodes[0]+1] = 0;
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
	public void exploreNeighbors(Long neighID, int idx_v, Double dv, Vector<Double> distance, Vector<Long> NP, Vector<Long> nodes, Vector<Vector<Integer>> P, ConcurrentLinkedQueue<Integer> queue, Integer [] unexploredNodes )
	{
		Integer idx_w = nodes.indexOf( neighID );
		Long vID = nodes.get(idx_v);
		Double olddw = distance.get( idx_w );
		Pair<Long,Double> tmp = weightGraph.get( new Pair<Long,Long>(vID, neighID) );
		Double weight = (tmp == null) ? BetweennessCentralityParams.defaultWeight :
										BetweennessCentralityParams.defaultWeight + tmp.getSecond();

		Double dvw = dv + weight;
		//System.out.println("Explore neighbor " + neighID);
		if( olddw > dvw )
		{
			lock.lock();
			distance.set( idx_w, dvw );
			lock.unlock();
			if( !queue.contains(idx_w) ){
				queue.add( idx_w );
				synchronized (unexploredNodes) {
					unexploredNodes[unexploredNodes[0]+1]++;
				}
			}
			
		}

		if( distance.get(idx_w) == dvw )
		{
			Long NPv = NP.get( idx_v );
			Long NPw = NP.get( idx_w );
			if( NPw != null && NPv != null )
			{
				lock.lock();
				NP.set( idx_w, NPv + NPw );
				lock.unlock();
			}
			else
			{
				logger.info(nodeIdx +" Impossible to compute NP for " + idx_v +"-" + idx_w);
			}
			Vector<Integer> oldP = P.get( idx_w );
			oldP.add( idx_v );
			lock.lock();
			P.set( idx_w, oldP );
			lock.unlock();
		}
		//System.out.println("Finish Explore neighbor " + neighID);

	}
	@Override
	public void run() {
		startComputeBetweennessCentralityP();
	}
}

public class BetweennessCentralityParallel {
	 /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(BetweennessCentralityParallel.class.getName());

    static final ReentrantLock lock = new ReentrantLock();
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

		System.out.println("Graful are " +  nodes.size() + "noduri");
		for( int i = 0; i < nodes.size(); i++ )
		{
			CB.add(0d);
		}
		for( j = 0; j < nodes.size(); j++ )
		{
			threadPool.submit( new BC_SSSP_Computation(graph, weightGraph, j, lock) );
		}
		threadPool.waitForThreadPoolProcessing();

		try {
			PrintWriter writer = null;
			writer = new PrintWriter( GenericParams.mapConfig.getInterpolateAlgResults() +"/" + GenericParams.mapConfig.getCity() + "_"+ j +"_CBranking.txt", "UTF-8");

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
