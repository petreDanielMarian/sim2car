package utils;

import java.io.File;
import java.util.regex.Matcher;


public class OSUtils {


	private static final String OS = System.getProperty("os.name").toLowerCase();

	private static String pathSeparator = Matcher.quoteReplacement(File.separator);
	
	
	public static boolean isWindows() {
		
		return OS.indexOf("win") >= 0;
	}
	
	public static boolean isUnix() {
		/* Unix, Linux or AIX */
		return (OS.indexOf("nix") >= 0
			|| OS.indexOf("nux") >= 0
			|| OS.indexOf("aix") > 0);
	}
	
	/**
	 * 
	 * @param path - path to a given file
	 * @return String - the path with the correct OS path separator
	 */
	public static String correctThePath(String path) {
		
		if (OSUtils.isWindows()) {
			
			if (path.indexOf("/") >= 0)
				path = path.replaceAll("/", pathSeparator);
			
		}
		
		if (OSUtils.isUnix()) {
			
			if (path.indexOf("\\") >= 0)
				path = path.replaceAll(Matcher.quoteReplacement("\\"), pathSeparator);
			
		}
		
		return path;
	}
	
	/**
	 * 
	 * @return String - OS path separator
	 */
	public static String getPathSeparator() {

		return pathSeparator;
	}

	public static String getPropertiesFilePath() {
		return (isUnix() ? "src" + pathSeparator : "");
	}
	

}
