/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

class AStarNode implements Comparable {
	
	public int x;
	public int y;
	public int z;
	
	public float g; // cost of the path so far (up to and including this node)
	public float h; // heuristic esimate of the cost of going from here to the goal
	
	public float f;
	
	private AStarNode predecessor;
	
	public AStarNode( int x, int y, int z,
			  float g, float h,
			  AStarNode predecessor ) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.g = g;
		this.h = h;
		this.f = g + h;
		this.predecessor = predecessor;
	}
	
	public boolean equals( Object other ) {
		AStarNode o = (AStarNode) other;
		// System.out.println( "  equals called between " + this + " and other " + other );
		// System.out.println( "  equals called between " );
		return (x == o.x) && (y == o.y) && (z == o.z);
	}
	
	public int hashCode( ) {
		// System.out.println( "  hashCode called on " + this );
		// System.out.println( "  hashCode called on " );
		return x + (1024 * y) + (1024 * 1024 * z);
	}
	
	public void setFrom( AStarNode another ) {
		this.x = another.x;
		this.y = another.y;
		this.z = another.z;
		this.g = another.g;
		this.h = another.h;
		this.f = another.f;
		this.predecessor = another.predecessor;
	}
	
/*
    public void setFrom( int x, int y, int z,
                         float g, float h,
                         AStarNode predecessor ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.g = g;
        this.h = h;
        this.f = g + h;
        this.predecessor = predecessor;
    }
*/
	
	public int compareTo( Object other ) {
		
		AStarNode o = (AStarNode) other;
		
		// System.out.println( "  compareTo called between " + this + " and other " + other );        
		// System.out.println( "  compareTo called between " );
		
		int compare_f_result = 0;
		if( f > o.f )
			compare_f_result = 1;
		else if( f < o.f )
			compare_f_result = -1;
		
		if( compare_f_result != 0 ) {
			
			return compare_f_result;
			
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
		return "("+x+","+y+","+z+") h: "+h+" g: "+g+" f: "+f;
	}
	
	public Path asPath( ) {
		Path creversed = new Path();
		AStarNode p = this;
		do {
			creversed.addPoint( p.x, p.y, p.z );
			p = p.predecessor;
		} while( p != null );
		return creversed.reversed();
	}
	
	public AStarNode getPredecessor( ) {
		return predecessor;
	}
	
	public Path asPathReversed( ) {
		Path result = new Path();
		AStarNode p = this;
		do {
			result.addPoint( p.x, p.y, p.z );
			p = p.predecessor;
		} while( p != null );
		return result;
	}
	
}
