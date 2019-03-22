package controller.network;

import java.util.Vector;

import model.Entity;
import model.parameters.Globals;

public class NetworkUtils {

	/**
	 * Create the network interface for current car object
	 * @param type - network interface type
	 * @param car - Car object
	 * @return
	 */
	public static NetworkInterface activateNetworkInterface( NetworkType type, Entity entity)
	{
		switch (type) {
			case Net_WiFi:
				return new NetworkWiFi(entity);
		default:
			return null;
		}
	}
	
	/**
	 * @param netInterfaces - String with active Network Interfaces
	 */
	public static void parseNetInterfaces(String netInterfaces)
	{
		String networkInterfaces[] = netInterfaces.split(",");
		Globals.activeNetInterfaces = new Vector<NetworkType>();
		for( String networkInterface : networkInterfaces )
		{
			Globals.activeNetInterfaces.add( NetworkType.valueOf("Net_"+networkInterface));
		}
	}
}
