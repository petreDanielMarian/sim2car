package utils;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import model.parameters.Globals;

public class Proxy {
	
	/**
	 * checks if Proxy settings are present
	 */
	public static void checkForProxySettings() {
		
		if (Globals.useHttpProxy.compareTo("true") == 0)
			useHttpProxy();
	}
	
	/**
	 * set up connection through HTTP-Proxy
	 */
	public static void useHttpProxy() {
		
		System.setProperty("proxySet", Globals.useHttpProxy);
		System.setProperty("http.proxyHost", Globals.httpProxyHost);
		System.setProperty("http.proxyPort", Globals.httpProxyPort);
		Authenticator.setDefault(new Authenticator() {
		    protected PasswordAuthentication getPasswordAuthentication() {

		        return new PasswordAuthentication(Globals.httpProxyUser,Globals.httpProxyPassword.toCharArray());
		    }
		});
	}
}
