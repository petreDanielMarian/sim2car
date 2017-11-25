package model.tiles;

import java.awt.Point;

/**
 * Abstract class to manage a tile on the map.
 */
public abstract class GenericTile {

	/* Coordinates */
	public Point id;
	/* Tile's size */
	public double size;
	/* Number of replicas already in existence */
	public long replicaCount;

	/* TODO What is this? */
	public long etheric;
	/* Medium distance between this tile and the cars who have it. */
	public double dmed;

	public GenericTile() {
		id = null;
		size = replicaCount = 0;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 67 * hash + (this.id != null ? this.id.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GenericTile)) {
			return false;
		}
		return this.id.equals(((GenericTile) o).id);
	}

	@Override
	public String toString() {
		return id.toString();
	}
}