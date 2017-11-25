package utils.tracestool.parameters;

import model.parameters.Globals;
import model.parameters.MapConfig;
import model.parameters.MapConfiguration;

public class GenericParams {
	/* map configuration */
	public static MapConfig mapConfig =  MapConfiguration.getInstance(Globals.propertiesFile);
}
