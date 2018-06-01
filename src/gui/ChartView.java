package gui;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JPanel;

import model.GeoCar;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import application.ApplicationType;
import application.tiles.TileApplicationCar;

public class ChartView {
	
	public ChartView( JPanel parent, String chartTitle, ArrayList<GeoCar> data, int tip ){ 
		  XYDataset dataset = null;
		  if( tip == 1 )
			  dataset = createDataset1Cars( data );
		  if( dataset == null )
			  return;
	      JFreeChart chart = createChart(dataset);
	      ChartPanel chartPanel = new ChartPanel(chart);
	      chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
	      parent.add(chartPanel);
	    
	}

	private XYDataset createDataset1Cars( ArrayList<GeoCar> lista ) {
	    final XYSeries series1 = new XYSeries("Tiles received from peers");
	    final XYSeries series2 = new XYSeries("Tiles sent to peers");
	    for( int i = 0; i < lista.size(); i++ ){
	    	GeoCar car =  lista.get(i);
	    	TileApplicationCar c_app = (TileApplicationCar) car.getApplication(ApplicationType.TILES_APP);
	    	series1.add( car.getId(),  c_app.fromPeers );
	    	series2.add( car.getId(),  c_app.toPeers );
	    }
	
	    System.out.println("Size " + lista.size() +"  ");
	    final XYSeriesCollection dataset = new XYSeriesCollection();
	    dataset.addSeries(series1);
	    dataset.addSeries(series2);
	            
	    return dataset;
	}

	private JFreeChart createChart(final XYDataset dataset) {
		// create the chart...
		final JFreeChart chart = ChartFactory.createXYLineChart(
				"Tiles exchange",      // chart title
				"Cab id",              // x axis label
				"Tiles",               // y axis label
				dataset,               // data
				PlotOrientation.VERTICAL,
				true,                  // include legend
				true,                  // tooltips
				false                  // urls
				);

		// Do some optional customisation of the chart
		chart.setBackgroundPaint(Color.white);

		// get a reference to the plot for further customisation...
		final XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.lightGray);

		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);

		final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesLinesVisible(0, true);
		renderer.setSeriesLinesVisible(1, true);
		plot.setRenderer(renderer);

		// change the auto tick unit selection to integer units only...
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		return chart;
	}
}
