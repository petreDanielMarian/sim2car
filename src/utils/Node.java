package utils;

public interface Node {
	/* Returns the coordinate from the horizontal axe 
	 * - Cartesian system : returns a coordinate from OX axe
	 * - Geographic system: returns longitude  
	 */ 
	public Object getX();

	/* Returns the coordinate from the vertical axe
	 * - Cartesian system : returns a coordinate from OY axe
	 * - Geographic system: returns latitude    
	 */ 
	public Object getY();
}
