package utils.tracestool;

import utils.tracestool.algorithms.BetweennessCentrality;
import utils.tracestool.algorithms.BetweennessCentralityParallel;
import utils.tracestool.algorithms.BetweennessCentralityParallel2;
import utils.tracestool.algorithms.JunctionsDetection;
import utils.tracestool.algorithms.OSMGraph;
import utils.tracestool.algorithms.PageRank;
import utils.tracestool.algorithms.SyntheticTracesGenerator;
import utils.tracestool.algorithms.TracesCorrection;
import utils.tracestool.algorithms.TracesInterpolation;
import model.parameters.Globals;

public class StartTraceTool {

	public static void main(String[] args) {

		Globals.setUp( args );

		/* build the graph */
		//(new OSMGraph()).buildOSMGRAPH( true, true );
		//TracesCorrection.getCorrectTraceCabs();
		//TracesInterpolation.getInterpolTraceCabs();
		//JunctionsDetection.getJunctionsDetection();
		//PageRank.determinePageRank();
		//BetweennessCentrality.determineBetweennessCentrality();
		//BetweennessCentralityParallel.determineBetweennessCentrality();
		//BetweennessCentralityParallel2.determineBetweennessCentrality();
		SyntheticTracesGenerator.getSyntheticTraces();
	}

}
