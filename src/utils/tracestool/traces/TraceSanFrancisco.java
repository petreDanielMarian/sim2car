package utils.tracestool.traces;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.logging.Logger;

public class TraceSanFrancisco extends Trace {

	/* Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(TraceSanFrancisco.class.getName());

	public TraceSanFrancisco( String path, String name )
	{
		super(path, name );
	}
	
	/* Line Parsing for SanFrancisco */
	public TraceNode parseLineSanFrancisco( String line ){
		
		
		StringTokenizer st = new StringTokenizer(line," ",false);
		double lat = Double.parseDouble(st.nextToken());
		double lon = Double.parseDouble(st.nextToken());
		int isOccupied = Integer.parseInt(st.nextToken());
		long time = Long.parseLong(st.nextToken());

		TraceNode crt = new TraceNode(lat, lon, isOccupied, time ); 
		if( st.hasMoreTokens() ){
			long wid = Long.parseLong(st.nextToken());
			crt.setIdStreet(wid);
		}

		return crt;
	}

	@Override
	public int readTrace()
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

			prev = parseLineSanFrancisco(line);
			if( prev == null )
			{
				logger.info("Parsing line error");
				br.close();
				fstream.close();
				return -1;
			}
			nodes.add(prev);

			while( (line = br.readLine()) != null ){
				crt = parseLineSanFrancisco(line);

				if( crt.timestamp == prev.timestamp )
					continue;

				nodes.add(0, crt);

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
