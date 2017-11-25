package utils.tracestool.traces;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import utils.tracestool.algorithms.OSMGraph;

public class Trace {

	/* The name of the trace */
	public String traceName = null;
	/* Path to the directory where the trace is located */
	public String path = null;
	/* List of trace's points */
	public Vector<TraceNode> nodes;

	/* Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(OSMGraph.class.getName());

	public Trace() {
		nodes = new Vector<TraceNode>();
	}

	public Trace( String path, String name ) {
		this();
		this.traceName = name;
		this.path = path;
	}

	public int readTrace()
	{
		if( traceName == null || path == null )
		{
			logger.info("The trace has not set up the trace name and path");
			return -1;
		}

		logger.info("Generic read function: please implement this function specific for you trace");
		return 0;
	}
	
	/* Line Parsing for Processed Trace */
	public TraceNode parseLineProcessedTrace( String line ){
		StringTokenizer st = new StringTokenizer(line," ",false);

		double lat = Double.parseDouble(st.nextToken());
		double lon = Double.parseDouble(st.nextToken());

		int isOccupied = Integer.parseInt(st.nextToken());

		long timestamp = Long.parseLong(st.nextToken());
		TraceNode crt = new TraceNode(lat, lon, isOccupied, timestamp ); 
		if( st.hasMoreTokens() ){
			long wid = Long.parseLong(st.nextToken());
			crt.setIdStreet(wid);
		}

		return crt;
	}
	
	/* The Processed trace format is the same for all traces */
	public int readProcessedTrace()
	{
		if( traceName == null || path == null )
		{
			logger.info("The trace has not set up the trace name and path");
			return -1;
		}

		FileInputStream fstream;
		String fnamePh = path  +"/" + traceName;

		try {

			fstream = new FileInputStream(fnamePh);

			nodes.clear();

			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String line;
			TraceNode crt, prev;

			line = br.readLine();

			if( line ==  null )
			{
				br.close();
				fstream.close();
				return -1;
			}

			prev = parseLineProcessedTrace(line);
			if( prev == null )
			{
				logger.info("Parsing line error");
				br.close();
				fstream.close();
				return -1;
			}
			nodes.add(prev);

			while( (line = br.readLine()) != null ){
				crt = parseLineProcessedTrace(line);

				if( crt.timestamp == prev.timestamp )
					continue;

				nodes.add(crt);

			}

			br.close();
			fstream.close();
		} catch (FileNotFoundException e) {
			logger.info( "Cannot open the file " + fnamePh );
			e.printStackTrace();
			return -1;
		} catch (IOException e) {
			logger.info( "IO exception for " + fnamePh );
			e.printStackTrace();
			return -1;
		}

		return 0;
	}

}
