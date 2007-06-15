/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

class FillerNode implements Comparable {
	
	// This is basically a Dijkstra all-points shortest paths
	// search node.  Similar to AStarNode, but without the
	// heuristic.

	public int x;
	public int y;
	public int z;
	
	public float g; // cost of the path so far (up to and including this node)
	
	private FillerNode predecessor;
	
	public FillerNode( int x, int y, int z,
			  float g,
			  FillerNode predecessor ) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.g = g;
		this.predecessor = predecessor;
	}
	
	public boolean equals( Object other ) {
		FillerNode o = (FillerNode) other;
		// System.out.println( "  equals called between " + this + " and other " + other );
		// System.out.println( "  equals called between " );
		return (x == o.x) && (y == o.y) && (z == o.z);
	}
	
	public int hashCode( ) {
		// System.out.println( "  hashCode called on " + this );
		// System.out.println( "  hashCode called on " );
		return x + (1024 * y) + (1024 * 1024 * z);
	}
	
	public void setFrom( FillerNode another ) {
		this.x = another.x;
		this.y = another.y;
		this.z = another.z;
		this.g = another.g;
		this.predecessor = another.predecessor;
	}
	
	public int compareTo( Object other ) {
		
		FillerNode o = (FillerNode) other;
		
		// System.out.println( "  compareTo called between " + this + " and other " + other );        
		// System.out.println( "  compareTo called between " );
		
		int compare_g_result = 0;
		if( g > o.g )
			compare_g_result = 1;
		else if( g < o.g )
			compare_g_result = -1;
		
		if( compare_g_result != 0 ) {
			
			return compare_g_result;
			
		} else {
			
			// Annoyingly, we need to distinguish between nodes with the
			// same priority, but which are at different locations.
			
			int x_compare = 0;
			if( x > o.x )
				x_compare = 1;
			if( x < o.x )
				x_compare = -1;
			
			if( x_compare != 0 )
				return x_compare;
			
			int y_compare = 0;
			if( y > o.y )
				y_compare = 1;
			if( y < o.y )
				y_compare = -1;
			
			if( y_compare != 0 )
				return y_compare;
			
			int z_compare = 0;
			if( z > o.z )
				z_compare = 1;
			if( z < o.z )
				z_compare = -1;
			
			return z_compare;
			
		}
	}
	
	public String toString( ) {
		return "("+x+","+y+","+z+") g: "+g;
	}
		
	public FillerNode getPredecessor( ) {
		return predecessor;
	}

	
}
