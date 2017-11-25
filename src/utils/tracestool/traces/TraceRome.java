package utils.tracestool.traces;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Logger;

public class TraceRome extends Trace {

	/* Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(TraceRome.class.getName());

	public TraceRome( String path, String name )
	{
		super(path, name );
	}
	
	/* Line Parsing for Rome */
	public TraceNode parseLineTraceRome( String line ){
		StringTokenizer st = new StringTokenizer(line,",",false);
		/* Set the date format in GMT and ex: 2008-02-02 13:34:12*/
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		/* get the date */
		Date d = null;
		long time = 0;
		try {
			d = sd.parse(st.nextToken());

			/* Because they are miliseconds */
			time = d.getTime()/1000;
		} catch (ParseException e) {
			logger.info("Impossible to parse the timestamp");
			e.printStackTrace();
			return null;
		}

		double lat = Double.parseDouble(st.nextToken());
		double lon = Double.parseDouble(st.nextToken());

		int isOccupied = 0;

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

			prev = parseLineTraceRome(line);
			if( prev == null )
			{
				logger.info("Parsing line error");
				br.close();
				fstream.close();
				return -1;
			}
			nodes.add(prev);

			while( (line = br.readLine()) != null ){
				crt = parseLineTraceRome(line);

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
