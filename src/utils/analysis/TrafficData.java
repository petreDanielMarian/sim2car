package utils.analysis;

/***
 * 
 * @author Andreea
 *
 */
public class TrafficData {
	private double avgNA = 0.0;
	private double avgTL = 0.0;
	private double avgDTL = 0.0;
	
	public double getAvgNA() {
		return avgNA;
	}
	public void setAvgNA(double avgNA) {
		this.avgNA = avgNA;
	}
	public double getAvgTL() {
		return avgTL;
	}
	public void setAvgTL(double avgTL) {
		this.avgTL = avgTL;
	}
	public double getAvgDTL() {
		return avgDTL;
	}
	public void setAvgDTL(double avgDTL) {
		this.avgDTL = avgDTL;
	}
	
	public String getData() {
		return avgNA + " " + avgTL + " " + avgDTL;
	}
}
