/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import java.util.ArrayList;

/* This class represents what's called a "Path" in the interface: a
 * non-branching sequence of adjacent pixels.  It's made up of
 * Connection objects, which represent an automatically found path
 * between two manually selected points.  The reason that we don't
 * just add points to a single Connection object is that in the future
 * we might want to undo bit-by-bit.  Well, that may not be a very
 * good reason for them being separate classes, in fact, but it's not
 * worth changing at the moment. */

public class SegmentedConnection {
	
	public SegmentedConnection() {
		connections = new ArrayList< Connection >();
	}
	
	ArrayList< Connection > connections;
	
	/* If you hold down shift when marking the start point, that
	   indicates that it's a join to another neurite (e.g. at a
	   branch).  In practice I don't actually use this, and just infer
	   joins from proximity... */
	
	boolean join_at_start = false;
	
	public void startsAtJoin( boolean join_at_start ) {
		this.join_at_start = join_at_start;
	}
	
	public void addConnection( Connection c ) {
		connections.add( c );
	}

	public int size() {
		return connections.size();
	}
	
}
