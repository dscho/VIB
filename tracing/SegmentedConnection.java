/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import ij.ImagePlus;
import ij.IJ;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.gui.StackWindow;

import pal.math.*;

import java.util.ArrayList;
import java.util.Iterator;

/* This class represents what's called a "Path" in the interface: a
 * non-branching sequence of adjacent pixels.  It's made up of
 * Connection objects, which represent an automatically found path
 * between two manually selected points.  The reason that we don't
 * just add points to a single Connection object is that in the future
 * we might want to undo bit-by-bit.  Well, that may not be a very
 * good reason for them being separate classes, in fact, but it's not
 * worth changing at the moment. */

/*
 * This class also stores fitted radiuses and tangents to the path at
 * each point except the first and last.
 */

public class SegmentedConnection implements Cloneable {

	public SegmentedConnection() {
		connections = new ArrayList< Connection >();
	}

	public SegmentedConnection clone() {

		SegmentedConnection result = new SegmentedConnection();

		for( Iterator i = connections.iterator();
		     i.hasNext();
			) {

			Connection c = (Connection)i.next();
			result.connections.add(c.clone());

		}
			
		return result;
	}
	
	ArrayList< Connection > connections;
	
	/* If you hold down control when marking the start point, that
	   indicates that it's a join to the currently selected path. */
	
	SegmentedConnection branchesOff;
	
	public void startsWithJoin( SegmentedConnection branchesOff ) {
		this.branchesOff = branchesOff;
	}
	
	public void addConnection( Connection c ) {
		connections.add( c );
	}

	public int size() {
		return connections.size();
	}

	public int numberOfPoints() {
		int result = 0;
		int segments_in_path = connections.size();
		for( int j = 0; j < segments_in_path; ++j ) {
			Connection connection = (Connection)connections.get(j);
			result += connection.size();
		}
		return result;
	}


	public void getPoint( int i, int [] point ) {
		
		if( (i < 0) || i >= numberOfPoints() ) {
			point[0] = point[1] = point[2] = -1;
			return;
		}

		int currentPoint = 0;
		
		int segments_in_path = connections.size();
		for( int j = 0; j < segments_in_path; ++j ) {
			Connection connection = (Connection)connections.get(j);
			for( int k = 0; k < connection.points; ++k ) {
				if( i == currentPoint ) {
					point[0] = connection.x_positions[k];
					point[1] = connection.y_positions[k];
					point[2] = connection.z_positions[k];
					return;
				}
				++ currentPoint;
			}
		}

	}
}
