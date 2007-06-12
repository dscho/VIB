package tracing;

class GraphNode implements Comparable {

	public int id;

	public int x;
	public int y;
	public int z;

	public String material_name;

	/* These few for the path finding... */
	public GraphNode previous;
	public double g; // cost of the path so far (up to and including this node)
	public double h; // heuristic esimate of the cost of going from here to the goal
	/* ... end of path */

	void setFrom( GraphNode other ) {
		this.id = other.id;
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
		this.material_name = other.material_name;
		this.previous = other.previous;
		this.g = other.g;
		this.h = other.h;
	}       

	// -----------------------------------------------------------------

	double f() {
		return g + h;
	}
	
	public int compareTo( Object other ) {
		GraphNode g = (GraphNode)other;
		return Double.compare( f(), g.f() );
	}

	public boolean equals( Object other ) {
		// System.out.println("  equals called "+id);
		return this.id == ((GraphNode)other).id;
	}

	public int hashCode() {
		// System.out.println("  hashcode called "+id);
		return this.id;
	}

	// -----------------------------------------------------------------

	public boolean nearTo( int within, int other_x, int other_y, int other_z ) {
		int xdiff = other_x - x;
		int ydiff = other_y - y;
		int zdiff = other_z - z;
		long distance_squared = xdiff * xdiff + ydiff * ydiff + zdiff * zdiff;
		long within_squared = within * within;
		return distance_squared <= within_squared;
	}

	public String toDotName( ) {
		return material_name + " (" + id + ")";
	}

	public String toCollapsedDotName( ) {
		if( material_name.equals("Exterior") )
			return material_name + " (" + id + ")";
		else
			return material_name;
	}

}
