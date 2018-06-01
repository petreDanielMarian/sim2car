package model.OSMgraph;

/**
 * A cell in the array of points near a Way
 */
public class Cell {
	public final long cellNr;
	public final long trafficEntityId;
	
	public Cell(long cellNr, long trafficEntityId) {
		this.cellNr = cellNr;
		this.trafficEntityId = trafficEntityId;
	}
	
	@Override
	public String toString() {
		return "<Cell: cellIndex = " + cellNr + ", entityId = " + trafficEntityId + ">";
	}
}
