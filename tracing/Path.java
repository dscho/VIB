/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple Neurite Tracer".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import java.awt.*;

import ij.gui.*;
import ij.*;
import ij.process.*;

import pal.math.*;

import stacks.ThreePanes;

import ij3d.Image3DUniverse;
import ij3d.Content;
import ij3d.Pipe;
import javax.vecmath.Color3f;

import javax.swing.tree.DefaultMutableTreeNode;

import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;

/* This class represents a list of points, and has methods for drawing
 * them onto ThreePanes-style image canvases. */

public class Path implements Comparable {

	public int compareTo(Object o) {
		Path casted = (Path)o;
		if( id == casted.id )
			return 0;
		if( id < casted.id )
			return -1;
		else
			return 1;
	}

	/* The path's ID should be assigned by the PathAndFillManager
	   when it's added: */
	private int id = -1;
	public int getID() {
		return id;
	}
	void setID( int id ) {
		this.id = id;
	}

	static final boolean verbose = Simple_Neurite_Tracer.verbose;

	boolean selected;

	Path startJoins;
	int startJoinsIndex = -1;

	Path endJoins;
	int endJoinsIndex = -1;

	public static final int PATH_START = 0;
	public static final int PATH_END = 1;

	// Paths should always be given a name (since the name
	// identifies them to the 3D viewer)...
	String name;

	public void setName(String newName) {
		this.name = newName;
	}

	public void setDefaultName() {
		this.name = "Path "+id;
	}

	public String getName() {
		if( name == null )
			throw new RuntimeException("In Path.getName() for id "+id+", name was null");
		return name;
	}

	/* This is a symmetrical relationship, showing all the other
	   paths this one is joined to... */
	ArrayList<Path> somehowJoins;

	/* We sometimes impose a tree structure on the Path graph,
	   which is largely for display purposes.  When this is done,
	   we regerated this list.  This should always be a subset of
	   'somehowJoins'... */
	ArrayList<Path> children;

	public void setChildren( Set<Path> pathsLeft ) {
		// Set the children of this path in a breadth first fashion:
		children.clear();
		Iterator<Path> ci = somehowJoins.iterator();
		while( ci.hasNext() ) {
			Path c = ci.next();
			if( pathsLeft.contains(c) ) {
				children.add(c);
				pathsLeft.remove(c);
			}
		}
		ci = children.iterator();
		while( ci.hasNext() ) {
			Path c = ci.next();
			c.setChildren( pathsLeft );
		}
	}

/*
	public DefaultMutableTreeNode getNode( ) {
		DefaultMutableTreeNode thisNode = new DefaultMutableTreeNode( this );
		for( int i = 0; i < children.size(); ++i ) {
			DefaultMutableTreeNode childNode = new DefaultMutableTreeNode( children.get(i) );
			thisNode.add( childNode );
		}
		return thisNode;
	}
*/

	public double getRealLength( ) {
		double totalLength = 0;
		for( int i = 1; i < points; ++i  ) {
			double xdiff = (x_positions[i] - x_positions[i-1]) * x_spacing;
			double ydiff = (y_positions[i] - y_positions[i-1]) * y_spacing;
			double zdiff = (z_positions[i] - z_positions[i-1]) * z_spacing;
			totalLength += Math.sqrt(
				xdiff * xdiff +
				ydiff * ydiff +
				zdiff * zdiff );
		}
		return totalLength;
	}

	public String getRealLengthString( ) {
		return String.format( "%.4f", getRealLength() );
	}

	boolean primary = false;
	void setPrimary( boolean primary ) {
		this.primary = primary;
	}
	boolean getPrimary( ) {
		return primary;
	}

	/* We call this if we're going to delete the path represented
	   by this object */

	void disconnectFromAll( ) {
		/* This path can be connected to other ones either if:
		      - this starts on other
		      - this ends on other
		      - other starts on this
		      - other ends on this
		   In any of these cases, we need to also remove this
		   from other's somehowJoins and other from this's
		   somehowJoins.
		*/
		Iterator<Path> i = somehowJoins.iterator();
		while( i.hasNext() ) {
			Path other = i.next();
			if( other.startJoins != null && other.startJoins == this ) {
				other.startJoins = null;
				other.startJoinsIndex = -1;
			}
			if( other.endJoins != null && other.endJoins == this ) {
				other.endJoins = null;
				other.endJoinsIndex = -1;
			}
			int indexInOtherSomehowJoins = other.somehowJoins.indexOf( this );
			if( indexInOtherSomehowJoins >= 0 )
				other.somehowJoins.remove( indexInOtherSomehowJoins );
		}
		somehowJoins.clear();
		startJoins = null;
		startJoinsIndex = -1;
		endJoins = null;
		endJoinsIndex = -1;
	}

	void setStartJoin( Path other, int indexInOther ) {
		setJoin( PATH_START, other, indexInOther );
	}

	void setEndJoin( Path other, int indexInOther ) {
		setJoin( PATH_END, other, indexInOther );
	}

	/* This should be the only method that links one path to
	   another */
	void setJoin( int startOrEnd, Path other, int indexInOther ) {
		if( other == null ) {
			throw new RuntimeException("BUG: setJoin now should never take a null other path");
		}
		if( startOrEnd == PATH_START ) {
			// If there was an existing path, that's an error:
			if( startJoins != null )
				throw new RuntimeException("BUG: setJoin for START should not replace another join");
			startJoins = other;
			startJoinsIndex = indexInOther;
		} else if( startOrEnd == PATH_END ) {
			if( endJoins != null )
				throw new RuntimeException("BUG: setJoin for END should not replace another join");
			endJoins = other;
			endJoinsIndex = indexInOther;
		} else {
			IJ.error( "BUG: unknown first parameter to setJoin" );
		}
		// Also update the somehowJoins list:
		if( somehowJoins.indexOf(other) < 0 ) {
			somehowJoins.add(other);
		}
		if( other.somehowJoins.indexOf(this) < 0 ) {
			other.somehowJoins.add(this);
		}
	}

	double x_spacing;
	double y_spacing;
	double z_spacing;
	String spacing_units;

	Path( double x_spacing, double y_spacing, double z_spacing, String spacing_units ) {
		this.x_spacing = x_spacing;
		this.y_spacing = y_spacing;
		this.z_spacing = z_spacing;
		this.spacing_units = spacing_units;
		points = 0;
		maxPoints = 128;
		x_positions = new int[maxPoints];
		y_positions = new int[maxPoints];
		z_positions = new int[maxPoints];
		somehowJoins = new ArrayList<Path>();
		children = new ArrayList<Path>();
	}

	Path( double x_spacing, double y_spacing, double z_spacing, String spacing_units, int reserve ) {
		this.x_spacing = x_spacing;
		this.y_spacing = y_spacing;
		this.z_spacing = z_spacing;
		this.spacing_units = spacing_units;
		points = 0;
		maxPoints = reserve;
		x_positions = new int[maxPoints];
		y_positions = new int[maxPoints];
		z_positions = new int[maxPoints];
		somehowJoins = new ArrayList<Path>();
		children = new ArrayList<Path>();
	}

	public int points;
	public int maxPoints;

	public int x_positions[];
	public int y_positions[];
	public int z_positions[];

	public int size( ) {
		return points;
	}

	public void getPoint( int i, int [] p ) {

		if( (i < 0) || i >= size() ) {
			p[0] = p[1] = p[2] = -1;
			return;
		}

		p[0] = x_positions[i];
		p[1] = y_positions[i];
		p[2] = z_positions[i];
	}

/* FIXME:
	@Override
	public Path clone() {

		Path result = new Path( points );

		System.arraycopy( x_positions, 0, result.x_positions, 0, points );
		System.arraycopy( y_positions, 0, result.y_positions, 0, points );
		System.arraycopy( z_positions, 0, result.z_positions, 0, points );
		result.points = points;
		result.startJoins = startJoins;
		result.startJoinsIndex = startJoinsIndex;
		result.endJoins = endJoins;
		result.endJoinsIndex = endJoinsIndex;

		if( radiuses != null ) {
			this.radiuses = new double[radiuses.length];
			System.arraycopy( radiuses, 0, result.radiuses, 0, radiuses.length );
		}
		if( tangents_x != null ) {
			this.tangents_x = new double[tangents_x.length];
			System.arraycopy( tangents_x, 0, result.tangents_x, 0, tangents_x.length );
		}
		if( tangents_y != null ) {
			this.tangents_y = new double[tangents_y.length];
			System.arraycopy( tangents_y, 0, result.tangents_y, 0, tangents_y.length );
		}
		if( tangents_z != null ) {
			this.tangents_z = new double[tangents_z.length];
			System.arraycopy( tangents_z, 0, result.tangents_z, 0, tangents_z.length );
		}

		return result;
	}
*/

	PointInImage lastPoint( ) {
		if( points < 1 )
			return null;
		else
			return new PointInImage( x_positions[points-1],
						 y_positions[points-1],
						 z_positions[points-1] );
	}

	void expandTo( int newMaxPoints  ) {

		int [] new_x_positions = new int[newMaxPoints];
		int [] new_y_positions = new int[newMaxPoints];
		int [] new_z_positions = new int[newMaxPoints];
		System.arraycopy( x_positions,
				  0,
				  new_x_positions,
				  0,
				  points );
		System.arraycopy( y_positions,
				  0,
				  new_y_positions,
				  0,
				  points );
		System.arraycopy( z_positions,
				  0,
				  new_z_positions,
				  0,
				  points );
		x_positions = new_x_positions;
		y_positions = new_y_positions;
		z_positions = new_z_positions;
		maxPoints = newMaxPoints;
	}

	void add( Path other ) {

		if( other == null ) {
			IJ.log("BUG: Trying to add null Path" );
			return;
		}

		if( maxPoints < (points + other.points) ) {
			expandTo( points + other.points );
		}

		int toSkip = 0;

		/* We may want to skip some points at the beginning of
		   the next path if they're the same as the last point
		   on this path: */

		if( points > 0 ) {
			int last_x = x_positions[points-1];
			int last_y = y_positions[points-1];
			int last_z = z_positions[points-1];
			while((other.x_positions[toSkip] == last_x) &&
			      (other.y_positions[toSkip] == last_y) &&
			      (other.z_positions[toSkip] == last_z)) {
				++toSkip;
			}
		}

		System.arraycopy( other.x_positions,
				  toSkip,
				  x_positions,
				  points,
				  other.points - toSkip );

		System.arraycopy( other.y_positions,
				  toSkip,
				  y_positions,
				  points,
				  other.points - toSkip );

		System.arraycopy( other.z_positions,
				  toSkip,
				  z_positions,
				  points,
				  other.points - toSkip );

		if( endJoins != null )
			throw new RuntimeException("BUG: we should never be adding to a path that already endJoins");

		if( other.endJoins != null ) {
			setEndJoin( other.endJoins, other.endJoinsIndex );
			other.disconnectFromAll();
		}

		points = points + (other.points - toSkip);
	}

	void unsetPrimaryForConnected( HashSet<Path> pathsExplored ) {
		Iterator<Path> i = somehowJoins.iterator();
		while( i.hasNext() ) {
			Path p = i.next();
			if( pathsExplored.contains(p) )
				continue;
			p.setPrimary(false);
			pathsExplored.add(p);
			p.unsetPrimaryForConnected(pathsExplored);
		}
	}

	Path reversed( ) {
		Path c = new Path( x_spacing, y_spacing, z_spacing, spacing_units, points );
		c.points = points;
		for( int i = 0; i < points; ++i ) {
			c.x_positions[i] = x_positions[ (points-1) - i ];
			c.y_positions[i] = y_positions[ (points-1) - i ];
			c.z_positions[i] = z_positions[ (points-1) - i ];
		}
		return c;
	}

	void addPoint( int x, int y, int z ) {
		if( points >= maxPoints ) {
			expandTo( (int)( maxPoints * 1.2 + 1 ) );
		}
		x_positions[points] = x;
		y_positions[points] = y;
		z_positions[points++] = z;
	}

	public void drawPathAsPoints( ImageCanvas canvas, Graphics g, java.awt.Color c, int plane ) {
		drawPathAsPoints( canvas, g, c, plane, 0, -1 );
	}


	public void drawPathAsPoints( ImageCanvas canvas, Graphics g, java.awt.Color c, int plane, int z, int either_side ) {

		/* This is slightly ugly because we have to use
		   ImageCanvas.screenX and .screenY to find whether to
		   actually draw on the Graphics in case we're zoomed. */

		/* In addition, if this is a start or end point we
		   want to represent that with a circle or a square
		   (depending on whether that's a branch or not.) */

		g.setColor( c );

		int pixel_size = (int)canvas.getMagnification();
		if( pixel_size < 1 )
			pixel_size = 1;

		int spotExtra = pixel_size;
		int spotDiameter = pixel_size * 3;

		switch( plane ) {

		case ThreePanes.XY_PLANE:
		{
			for( int i = 0; i < points; ++i ) {
				if( (either_side >= 0) && (Math.abs(z_positions[i] - z) > either_side) )
					continue;

				int x = canvas.screenX(x_positions[i]);
				int y = canvas.screenY(y_positions[i]);

				if( ((i == 0) && (startJoins == null)) ||
				    ((i == points - 1) && (endJoins == null)) ) {
					// Then draw it as a rectangle...
					g.fillRect( x - spotExtra, y - spotExtra, spotDiameter, spotDiameter );
				} else if( ((i == 0) && (startJoins != null)) ||
					   ((i == points - 1) && (endJoins != null)) ) {
					// The draw it as an oval...
					g.fillOval( x - spotExtra, y - spotExtra, spotDiameter, spotDiameter );
				} else {
					// Just draw normally...
					g.fillRect( x, y, spotExtra, spotExtra );
				}
			}
		}
		break;

		case ThreePanes.XZ_PLANE:
		{
			for( int i = 0; i < points; ++i ) {
				if( (either_side >= 0) && (Math.abs(z_positions[i] - z) > either_side) )
					continue;

				int x = canvas.screenX(x_positions[i]);
				int y = canvas.screenY(z_positions[i]);

				if( ((i == 0) && (startJoins == null)) ||
				    ((i == points - 1) && (endJoins == null)) ) {
					// Then draw it as a rectangle...
					g.fillRect( x - spotExtra, y - spotExtra, spotDiameter, spotDiameter );
				} else if( ((i == 0) && (startJoins != null)) ||
					   ((i == points - 1) && (endJoins != null)) ) {
					// The draw it as an oval...
					g.fillOval( x - spotExtra, y - spotExtra, spotDiameter, spotDiameter );
				} else {
					// Just draw normally...
					g.fillRect( x, y, spotExtra, spotExtra );
				}
			}
		}
		break;

		case ThreePanes.ZY_PLANE:
		{
			for( int i = 0; i < points; ++i ) {
				if( (either_side >= 0) && (Math.abs(z_positions[i] - z) > either_side) )
					continue;

				int x = canvas.screenX(z_positions[i]);
				int y = canvas.screenY(y_positions[i]);

				if( ((i == 0) && (startJoins == null)) ||
				    ((i == points - 1) && (endJoins == null)) ) {
					// Then draw it as a rectangle...
					g.fillRect( x - spotExtra, y - spotExtra, spotDiameter, spotDiameter );
				} else if( ((i == 0) && (startJoins != null)) ||
					   ((i == points - 1) && (endJoins != null)) ) {
					// The draw it as an oval...
					g.fillOval( x - spotExtra, y - spotExtra, spotDiameter, spotDiameter );
				} else {
					// Just draw normally...
					g.fillRect( x, y, spotExtra, spotExtra );
				}
			}
		}
		break;

		}

	}

	// ------------------------------------------------------------------------
	// FIXME: adapt these for Path rather than SegmentedConnection, down to EOFIT

	class CircleAttempt implements MultivariateFunction, Comparable {

		double min;
		double [] best;
		double [] initial;

		byte [] data;
		int maxValueInData;
		int side;

		public CircleAttempt(double [] start, byte [] data, int maxValueInData, int side ) {

			this.data = data;
			this.maxValueInData = maxValueInData;
			this.side = side;

			min = Double.MAX_VALUE;
			initial = start;
		}

		public int compareTo(Object other) {
			CircleAttempt o = (CircleAttempt)other;
			if (min < o.min)
				return -1;
			else if (min > o.min)
				return +1;
			else
				return 0;
		}

		public int getNumArguments( ) {
			return 3;
		}

		public double getLowerBound( int n ) {
			return 0;
		}

		public double getUpperBound( int n ) {
			return side;
		}

		public double evaluate(double [] x) {
			// System.out.println("evaluate called with: "+x[0]+","+x[1]+","+x[2]);
			double badness = evaluateCircle(x[0],x[1],x[2]);
			// System.out.println("   gave: "+badness);

			if (badness < min) {
				best = x.clone();
				min = badness;
			}

			return badness;
		}

		public double evaluateCircle( double x, double y, double r ) {

			double badness = 0;

			for( int i = 0; i < side; ++i ) {
				for( int j = 0; j < side; ++j ) {
					int value = data[j*side+i] & 0xFF;
					if( r * r > ((i - x) * (i - x)  + (j - y) * (j - y)) )
						badness += (maxValueInData - value) * (maxValueInData - value);
					else
						badness += value * value;
				}
			}

			if( (x + r) >= side ) {
				badness += maxValueInData * maxValueInData;
			}
			if( (x + r) < 0 ) {
				badness += maxValueInData * maxValueInData;
			}
			if( (y + r) >= side ) {
				badness += maxValueInData * maxValueInData;
			}
			if( (y + r) < 0 ) {
				badness += maxValueInData * maxValueInData;
			}

			badness /= (side * side);

			return badness;
		}

	}


	public void optimizeCircle( double start_x, double start_y, double start_r ) {

	}

	public Path fitCircles( int side, Simple_Neurite_Tracer plugin, boolean display ) {

		Path fitted = new Path( x_spacing, y_spacing, z_spacing, spacing_units );

		// if (verbose) System.out.println("Generating normal planes stack.");

		int totalPoints = size();

		if( verbose )
			System.out.println("There are: "+totalPoints+ " in the stack.");

		int last_x = -1;
		int last_y = -1;
		int last_z = -1;

		int second_last_x = -1;
		int second_last_y = -1;
		int second_last_z = -1;

		double x_spacing = plugin.x_spacing;
		double y_spacing = plugin.y_spacing;
		double z_spacing = plugin.z_spacing;

		if( verbose )
			System.out.println("Using spacing: "+x_spacing+","+y_spacing+","+z_spacing);

		int width = plugin.width;
		int height = plugin.height;
		int depth = plugin.depth;

		ImageStack stack = new ImageStack( side, side );

		// We assume that the first and the last in the stack are fine;

		double [] centre_x_positions = new double[totalPoints];
		double [] centre_y_positions = new double[totalPoints];
		double [] rs = new double[totalPoints];

		double [] ts_x = new double[totalPoints];
		double [] ts_y = new double[totalPoints];
		double [] ts_z = new double[totalPoints];

		for( int i = 0; i < size(); ++i ) {

			IJ.showProgress( i / (float)totalPoints );

			int x = x_positions[i];
			int y = y_positions[i];
			int z = z_positions[i];

			if( verbose )
				System.out.println("Considering point: "+last_x+","+last_y+","+last_z);

			if( (last_x < 0) || (second_last_x < 0) ) {

				if( last_x >= 0 ) {

					/* Then this is the first real
					   point.  We won't generate a
					   normal plane, since we
					   can't trust the normal
					   vector(well, maybe) but add
					   an empty slice so
					   everything is in sync. */

					fitted.addPoint( last_x, last_y, last_z );

					if( verbose )
						System.out.println("Adding empty slice.");

					byte [] empty = new byte[side*side];
					ByteProcessor bp = new ByteProcessor( side, side );
					bp.setPixels(empty);
					stack.addSlice(null,bp);

				}


			} else {

				/* Then the last two points were
				   valid, so assume the tanget vector
				   at last_* is the difference
				   between this point and
				   second_last_x... */

				int x_diff = x - second_last_x;
				int y_diff = y - second_last_y;
				int z_diff = z - second_last_z;

				// These are returned; they *are*
				// scaled with the _scaling variables

				double [] x_basis_in_plane = new double[3];
				double [] y_basis_in_plane = new double[3];

				byte [] normalPlane = plugin.squareNormalToVector(
					side,
					x_spacing,   // step is in the same units as the _spacing, etc. variables.
					last_x,      // These are are *not* yet scaled in z
					last_y,      // They're just sample point differences
					last_z,
					x_diff,
					y_diff,
					z_diff,
					x_basis_in_plane,
					y_basis_in_plane );

				/* Now at this stage, try to optimize
				   a circle in there... */

				// n.b. thes aren't normalized
				ts_x[i] = x_diff * x_spacing;
				ts_y[i] = y_diff * y_spacing;
				ts_z[i] = z_diff * z_spacing;

				ConjugateDirectionSearch optimizer = new ConjugateDirectionSearch();
				// optimizer.prin = 2; // debugging information on
				optimizer.step = side / 4.0;

				double [] startValues = new double[3];
				startValues[0] = (side - 1) / 2.0;
				startValues[1] = (side - 1) / 2.0;
				startValues[2] = 3.0;

				if( verbose )
					System.out.println("start search at: "+startValues[0]+","+startValues[1]+" with radius: "+startValues[2]);

				int maxValueInSquare = 0;
				for( int j = 0; j < (side * side); ++j ) {
					int value = normalPlane[j]&0xFF;
					if( value > maxValueInSquare )
						maxValueInSquare = value;
				}

				CircleAttempt attempt = new CircleAttempt(
					startValues,
					normalPlane,
					maxValueInSquare,
					side );

				optimizer.optimize( attempt, startValues, 2, 2 );

				if( verbose )
					// System.out.println("u is: "+u[0]+","+u[1]+","+u[2]);
					System.out.println("search optimized to: "+startValues[0]+","+startValues[1]+" with radius: "+startValues[2]);

				centre_x_positions[i] = startValues[0];
				centre_y_positions[i] = startValues[1];
				rs[i] = startValues[2];

				// Now we calculate the real co-ordinates of the new centre:

				double x_from_centre_in_plane = startValues[0] - (side / 2.0);
				double y_from_centre_in_plane = startValues[1] - (side / 2.0);

				if( verbose )
					System.out.println("vector to new centre from original: "+x_from_centre_in_plane+","+y_from_centre_in_plane);

				double centre_real_x = (last_x * x_spacing);
				double centre_real_y = (last_y * y_spacing);
				double centre_real_z = (last_z * z_spacing);

				if( verbose )
					System.out.println("original centre in real co-ordinates: "+centre_real_x+","+centre_real_y+","+centre_real_z);

				// FIXME: I really think these should be +=, but it seems clear from the results that I've got a sign wrong somewhere :(

				centre_real_x -= x_basis_in_plane[0] * x_from_centre_in_plane + y_basis_in_plane[0] * y_from_centre_in_plane;
				centre_real_y -= x_basis_in_plane[1] * x_from_centre_in_plane + y_basis_in_plane[1] * y_from_centre_in_plane;
				centre_real_z -= x_basis_in_plane[2] * x_from_centre_in_plane + y_basis_in_plane[2] * y_from_centre_in_plane;

				if( verbose )
					System.out.println("adjusted original centre in real co-ordinates: "+centre_real_x+","+centre_real_y+","+centre_real_z);

				int x_in_image = (int)Math.round( centre_real_x / x_spacing );
				int y_in_image = (int)Math.round( centre_real_y / y_spacing );
				int z_in_image = (int)Math.round( centre_real_z / z_spacing );

				if( verbose )
					System.out.println("gives in image co-ordinates: "+x_in_image+","+y_in_image+","+z_in_image);

				if( x_in_image < 0 ) x_in_image = 0; if( x_in_image >= width) x_in_image = width - 1;
				if( y_in_image < 0 ) y_in_image = 0; if( y_in_image >= height) y_in_image = height - 1;
				if( z_in_image < 0 ) z_in_image = 0; if( z_in_image >= depth) z_in_image = depth - 1;

				if( verbose )
					System.out.println("addingPoint: "+x_in_image+","+y_in_image+","+z_in_image);

				fitted.addPoint( x_in_image, y_in_image, z_in_image );

				if( verbose )
					System.out.println("Adding a real slice.");

				ByteProcessor bp = new ByteProcessor( side, side );
				bp.setPixels(normalPlane);
				stack.addSlice(null,bp);

			}

			second_last_x = last_x;
			second_last_y = last_y;
			second_last_z = last_z;

			last_x = x;
			last_y = y;
			last_z = z;
		}

		// Add an extra empty slice for the final one:

		if (verbose) System.out.println("Adding empty slice at the end.");

		byte [] empty = new byte[side*side];
		ByteProcessor bp = new ByteProcessor( side, side );
		bp.setPixels(empty);
		stack.addSlice(null,bp);

		IJ.showProgress( 1.0 );

		fitted.setFittedCircles( ts_x,
					 ts_y,
					 ts_z,
					 rs                );

		if( display ) {

			ImagePlus imp = new ImagePlus( "normal stack", stack );

			NormalPlaneCanvas normalCanvas = new NormalPlaneCanvas(
				imp,
				plugin,
				centre_x_positions,
				centre_y_positions,
				rs,
				fitted  );

			new StackWindow( imp, normalCanvas );

			imp.show();

		}

		return fitted;
	}

	private double [] radiuses;

	private double [] tangents_x;
	private double [] tangents_y;
	private double [] tangents_z;

	public boolean hasCircles() {
		return radiuses != null;
	}

	public void setFittedCircles( double [] tangents_x,
				      double [] tangents_y,
				      double [] tangents_z,
				      double [] radiuses ) {

		this.tangents_x = new double[tangents_x.length];
		System.arraycopy( tangents_x, 0, this.tangents_x, 0, tangents_x.length );
		this.tangents_y = new double[tangents_y.length];
		System.arraycopy( tangents_y, 0, this.tangents_y, 0, tangents_y.length );
		this.tangents_z = new double[tangents_z.length];
		System.arraycopy( tangents_z, 0, this.tangents_z, 0, tangents_z.length );
		this.radiuses = new double[radiuses.length];
		System.arraycopy( radiuses, 0, this.radiuses, 0, radiuses.length );

	}

	@Override
	public String toString() {
		String pathName;
		String name = getName();
		if( name == null )
			name = "Path " + id;
		name += " [" + getRealLengthString( ) + " " + spacing_units + "]";
		if( startJoins != null ) {
			name += ", starts on " + startJoins.getName();
		}
		if( endJoins != null ) {
			name += ", ends on " + endJoins.getName();
		}
		return name;
	}

/*
	@Override
	public String toString() {
		int n = size();
		String result = "";
		if( name != null )
			result += "\"" + name + "\" ";
		result += n + " points";
		if( n > 0 ) {
			result += " from " + x_positions[0] + ", " + y_positions[0] + ", " + z_positions[0];
			result += " to " + x_positions[n-1] + ", " + y_positions[n-1] + ", " + z_positions[n-1];
		}
		return result;
	}
*/


	Content content3D;

	public void removeFrom3DViewer(Image3DUniverse univ) {
		univ.removeContent(getName());
	}

	public Content addTo3DViewer(Image3DUniverse univ) {
		return addTo3DViewer( univ, null );
	}

	public Content addTo3DViewer(Image3DUniverse univ, Color c) {

		if(points <= 1) {
			content3D = null;
			return null;
		}

		double [] x_points_d = new double[points];
		double [] y_points_d = new double[points];
		double [] z_points_d = new double[points];
		double [] diameters = new double[points];

		for(int i=0; i<points; ++i) {
			x_points_d[i] = x_spacing * x_positions[i];
			y_points_d[i] = y_spacing * y_positions[i];
			z_points_d[i] = z_spacing * z_positions[i];
			diameters[i] = x_spacing * 3;
		}

		double [][][] allPoints = Pipe.makeTube(x_points_d,
							y_points_d,
							z_points_d,
							diameters,
							4,       // resample - 1 means just "use mean distance between points", 3 is three times that, etc.
							12);     // "parallels" (12 means cross-sections are dodecagons)
		if( allPoints == null )
			return null;

		java.util.List triangles = Pipe.generateTriangles(allPoints,
								  1); // scale

		String title = getName();

		univ.resetView();

		univ.addMesh(triangles,
			     c == null ? new Color3f(Color.magenta) : new Color3f(c),
			     title,
			     // 1f,  // scale
			     1); // threshold

		content3D = univ.getContent(title);
		content3D.setLocked(true);
		return content3D;
	}

	public void setSelected(boolean newSelectedStatus) {
		if( newSelectedStatus != selected ) {
			selected = newSelectedStatus;
			if( content3D != null ) {
				if( selected )
					content3D.setColor(new Color3f(Color.green));
				else
					content3D.setColor(new Color3f(Color.magenta));
			}
		}
	}

}
