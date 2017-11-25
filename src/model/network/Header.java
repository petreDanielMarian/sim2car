package model.network;

public class Header {

	private long sourceId;
	private long destId;
	private long TTL;
	private long creationTime;
	private int length;
	
	public long getSourceId() {
		return sourceId;
	}

	public void setSourceId(long sourceId) {
		this.sourceId = sourceId;
	}
	
	public long getDestId() {
		return destId;
	}

	public void setDestId(long destId) {
		this.destId = destId;
	}
	
	public long getTTL() {
		return TTL;
	}
	
	public long getLength() {
		return length;
	}
	
	
	
	public void setTTL(long TTL) {
		this.TTL = TTL;
	}
	
	public long getCreationTime() {
		return creationTime;
	}
	
	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}
	
	public void setLength(int length) {
		this.length = length;
	}
	
}
