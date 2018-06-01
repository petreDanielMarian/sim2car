package model;

import java.io.Serializable;

public class PeanoKey implements Comparable<PeanoKey>, Serializable {
	private static final long serialVersionUID = 6973673048808005956L;

	/** The peano key value array is obtained by interleaving the point's
	 * latitude and longitude digits: 10 (longitude) + 9 (latitude) 
	 */
	private byte[] value = new byte[19];
	private long streetIndex;

	public PeanoKey(MapPoint p) {
		int i;
		byte[] latitude = parseCoord(p.lat, 9);
		byte[] longitude = parseCoord(p.lon, 10);
		for (i = 0; i < 9; i++) {
		    this.value[2 * i] = longitude[i];
		    this.value[2 * i + 1] = latitude[i];
		}
		this.value[2 * i] = longitude[i];
		this.setStreetIndex(p.wayId);
	}
	
	public PeanoKey(double lat, double lon, long streetIndex) {
		int i;
		byte[] latitude = parseCoord(lat, 9);
		byte[] longitude = parseCoord(lon, 10);
		for (i = 0; i < 9; i++) {
		    this.value[2 * i] = longitude[i];
		    this.value[2 * i + 1] = latitude[i];
		}
		this.value[2 * i] = longitude[i];
		this.setStreetIndex(streetIndex);
	}

	
	@Override
	public int compareTo(PeanoKey o) {
		int i = 0;
		while (i < value.length && this.value[i] == o.value[i])
			i++;
		if (i < value.length)
				return this.value[i] - o.value[i];
		else
			return 0;
	}


	private byte[] parseCoord(double coord, int digitCount) {
		/* digitno can be 10 for longitude or 9 for latitude */
		byte[] result = new byte[digitCount];
		int coef = 0;
		if (coord > 0) {
			result[0] = '+';
			coef = 1;
		} else {
			result[0] = '-';
			coord = -coord;
			coef = -1;
		}
		
		int c = (int)(coord * 1e6);
		for (int i = digitCount - 1; i > 0; i--){
			result[i] = (byte)((c%10)*coef);
			c /= 10;
		}
		return result;
	}
	
	/**
	 * Used for searching the sorted peano keys vector, for cars in a
	 * certain range.
	 */
	public void setToUpperCorner(PeanoKey reference) {
		boolean trig = false;
		for (int i = 0; i < value.length; i++) {
			if (trig)
				value[i] = 9;
			else if (value[i] != reference.value[i])
				trig = true;
		}
	}
	public void setToLowerCorner(PeanoKey reference)
	{
		boolean trig = false;
		for (int i = 0; i < value.length; i++) {
			if (trig)
				value[i] = -9;
			else if (value[i] != reference.value[i])
				trig = true;
		}
	}

	public long getStreetIndex() {
		return streetIndex;
	}

	public void setStreetIndex(long streetIndex) {
		this.streetIndex = streetIndex;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		sb.append((char)this.value[0]);
		sb.append((char)this.value[1]);
		for (int i = 2; i < value.length; i++)
			sb.append((char)(Math.abs(this.value[i])+'0'));
		sb.append(" -> ");
		sb.append(this.streetIndex);
		sb.append("]");
		
		return sb.toString();
	}
}
