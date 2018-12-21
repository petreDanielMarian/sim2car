package model.parameters;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import utils.OSUtils;

/**
 * Class that loads the configurations for the current Map from the properties file.
 */
public class MapConfiguration implements MapConfig {
	
    /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(MapConfiguration.class.getName());
	
	private Properties prop = null;
	
	private static final MapConfiguration _instance = new MapConfiguration();
	
	private MapConfiguration() {
	}

	private void load(String configFilename) {
		prop = new Properties();
		File f = new File(configFilename);
		logger.info(f.getAbsolutePath());
		try {
			prop.load(new FileInputStream(f));
		} catch (IOException e) {
			logger.severe(e.getLocalizedMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public static MapConfiguration getInstance(String configFilename) {
		synchronized (MapConfiguration.class) {
			if (_instance.prop == null) {
				_instance.load(configFilename);
			}
		}
		return _instance;
	}

	@Override
	public String getMapFilename() {
		return OSUtils.correctThePath(prop.getProperty("mapPath"));
	}

	@Override
	public String getTracesPath() {
		return OSUtils.correctThePath(prop.getProperty("tracesPath"));
	}

	@Override
	public String getPartialResultsPath() {
		return OSUtils.correctThePath(prop.getProperty("resultsPartial"));
	}

	@Override
	public String getCorrectionAlgResults() {
		return OSUtils.correctThePath(prop.getProperty("resultsCorectationAlg"));
	}

	@Override
	public String getInterpolateAlgResults() {
		return OSUtils.correctThePath(prop.getProperty("resultsInterpolationAlg"));
	}

	@Override
	public String getPartialGraphFilename() {
		return OSUtils.correctThePath(prop.getProperty("graphPartialFileName"));
	}

	@Override
	public String getPartialStreetsFilename() {
		return OSUtils.correctThePath(prop.getProperty("streetsPartialFileName"));
	}

	@Override
	public String getStreetsFilename() {
		return OSUtils.correctThePath(prop.getProperty("streetsFileName"));
	}
	
	@Override
	public String getTrafficLightsFilename() {
		return OSUtils.correctThePath(prop.getProperty("trafficLightsFileName"));
	}
	
	@Override
	public String getTrafficLightsLoaded() {
		return OSUtils.correctThePath(prop.getProperty("trafficLightsLoaded"));
	}

	@Override
	public String getIndexTableFilename() {
		return OSUtils.correctThePath(prop.getProperty("indexTableFileName"));
	}

	@Override
	public String getTracesListFilename() {
		return OSUtils.correctThePath(prop.getProperty("tracesListPath"));
	}

	@Override
	public String getGeneratedTracesListFilename() {
		return OSUtils.correctThePath(prop.getProperty("generatedTracesListPath"));
	}
	@Override
	public String getAccessPointsFilename() {
		return OSUtils.correctThePath(prop.getProperty("accessPointsPath"));
	}

	@Override
	public Integer getN() {
		try {
			return (int) (Integer.parseInt(prop.getProperty("N")) *
					Math.pow(2, getQuot()));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public Integer getM() {
		try {
			return (int) (Integer.parseInt(prop.getProperty("M")) *
					Math.pow(2, getQuot()));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public Point2D getMapCentre() {
		try {
			Double x = Double.parseDouble(prop.getProperty("centerX"));
			Double y = Double.parseDouble(prop.getProperty("centerY"));
			return new Point2D.Double(x, y);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Integer getBaseColumn() {
		try {
			return (int) (Integer.parseInt(prop.getProperty("baseColumn")) *
					Math.pow(2, getQuot()));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public Integer getBaseRow() {
		try {
			return (int) (Integer.parseInt(prop.getProperty("baseRow")) *
					Math.pow(2, getQuot()));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public Integer getQuot() {
		try {
			return Integer.parseInt(prop.getProperty("quot"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0;
		}
	}


	@Override
	public String getCity() {
		return prop.getProperty("city");
	}

	@Override
	public long getInterpolateStartTime() {
		return Long.parseLong(prop.getProperty("interpolateStartTime"));
	}

	@Override
	public long getInterpolateInterval() {

		return Long.parseLong(prop.getProperty("interpolateInterval"));
	}

	@Override
	public String getResultsJunctionsDetectionAlgPath() {
		
		return prop.getProperty("resultsJunctionsDetectionAlg");
	}
	
	@Override
	public String getSortedRoadData() {
		return OSUtils.correctThePath(prop.getProperty("sortedRoadData"));
	}

	@Override
	public String getSortedRoadSpeeds() {
		return OSUtils.correctThePath(prop.getProperty("sortedRoadSpeeds"));
	}

	@Override
	public double getAreaMinLat() {
		return Double.parseDouble(prop.getProperty("minALat"));
	}

	@Override
	public double getAreaMaxLat() {
		return Double.parseDouble(prop.getProperty("maxALat"));
	}

	@Override
	public double getAreaMinLon() {
		return Double.parseDouble(prop.getProperty("minALon"));
	}

	@Override
	public double getAreaMaxLon() {
		return Double.parseDouble(prop.getProperty("maxALon"));
	}

	@Override
	public String getCongestionGraphFilename() {
		return OSUtils.correctThePath(prop.getProperty("congestionGraphFileName"));
	}

	@Override
	public String getPRGraphFilename() {
		return OSUtils.correctThePath(prop.getProperty("PRGraphFileName"));
	}
}
