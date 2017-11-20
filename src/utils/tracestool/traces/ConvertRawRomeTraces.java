package utils.tracestool.traces;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;



public class ConvertRawRomeTraces {

	/* filePath - should point to taxi_february.txt file */
	public static String filePath = "e:/Cursuri/cercetare/taxi_february.txt";
	/* outputPath - indicate the output folder */
	public static String outputPath = "e:/Cursuri/master/cercetare/Sim2Car/trunk/res/res/romecabs";
	static TreeMap<Integer, BufferedWriter> traces = new TreeMap<Integer, BufferedWriter>();
	public static void parseItalyTraces(){
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(filePath);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String line = null;
			while( (line = br.readLine()) != null )
			{
				String ws[] = line.split(";");
				int cabid = Integer.parseInt( ws[0] );
				filePath = outputPath+"/"+ cabid + ".txt";
				
				BufferedWriter bw = traces.get(cabid);
				String coords[] = ws[2].substring(6, ws[2].length()-1).split(" ");
				String crt_data = ws[1] +", "+ coords[0] +", "+coords[1];

				if( bw == null )
				{
					bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath)));
					traces.put( cabid, bw );
				}
				bw.write(crt_data+"\n");
				
			}
			br.close();
			fstream.close();
			
			for( Iterator <Map.Entry<Integer,BufferedWriter>> it = traces.entrySet().iterator();it.hasNext(); ){
				Map.Entry<Integer, BufferedWriter> aux = it.next();
				aux.getValue().close();
			}

		} catch (FileNotFoundException e) {
			System.err.println("File " + filePath +"can not be found");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Impossible to read from " + filePath );
			e.printStackTrace();
		}

		
	}
	public static void main(String[] args) {
		parseItalyTraces();
	}
	
	

}
