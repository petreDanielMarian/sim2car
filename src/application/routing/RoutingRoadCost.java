package application.routing;

import java.io.Serializable;
import java.util.TreeSet;

import controller.newengine.SimulationEngine;
import utils.Pair;

public class RoutingRoadCost implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2346033001073719718L;
	public double cost = 0;
	public TreeSet<Pair<Long,Double>> costs;
	public Double[] c = { -1d, -1d, -1d, -1d };
	public Double w0 = 0.4;
	public int weightNr = 4;
	public Double[] w = { 0.3, 0.15, 0.10, 0.05 };
	long time = 0L;

	public RoutingRoadCost()
	{
		costs = new TreeSet<Pair<Long,Double>>();
	}
	public void addStreetCostMeasure( double cost, long timestamp )
	{
		costs.add(new Pair<Long, Double>(timestamp, cost));
	}

	public double updateStreetCost( double cost, long timestamp )
	{
		time = timestamp;
		long crtHourStart = SimulationEngine.getInstance().getSimulationTime() - RoutingApplicationParameters.SamplingInterval;
		long nr = 0l;
		double costSum = 0d, c_road_now = 0d, c_road = 0d;
		TreeSet<Pair<Long,Double>> tmpcosts = new TreeSet<Pair<Long,Double>>();
		this.costs.add(new Pair<Long, Double>(timestamp, cost));
		/* remove the measurements that are out of the sampling interval */
		for( Pair<Long,Double> p : this.costs )
		{
			if( p.getFirst() >= crtHourStart )
			{
				costSum += p.getSecond();
				nr++;
				tmpcosts.add(p);
			}
		}

		this.costs = new TreeSet<Pair<Long,Double>>(tmpcosts);

		if( nr != 0 )
			c_road_now = costSum/nr;
		else
			return -1;

		c_road = w0 * c_road_now;

		for( int i = weightNr - 1; i >= 0; i-- )
		{
			c_road += (c[i] != -1 ? c[i] : 0) * w[i];
 
			if( i != 0 )
			{
				c[ i ] = c[ i - 1 ];
			}
		}

		c[0] = c_road_now;

		this.cost = c_road;

		return c_road;

	}
	
}
