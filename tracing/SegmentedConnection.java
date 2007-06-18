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

public class SegmentedConnection implements Cloneable {

	private double [] radiuses;

	private double [] normals_x;
	private double [] normals_y;
	private double [] normals_z;

	public boolean hasCircles() {
		return radiuses != null;
	}
	
	public void setFittedCircles( double [] normals_x,
				      double [] normals_y,
				      double [] normals_z,
				      double [] radiuses ) {
		
		this.normals_x = new double[normals_x.length];
		System.arraycopy( normals_x, 0, this.normals_x, 0, normals_x.length );
		this.normals_y = new double[normals_y.length];
		System.arraycopy( normals_y, 0, this.normals_y, 0, normals_y.length );
		this.normals_z = new double[normals_z.length];
		System.arraycopy( normals_z, 0, this.normals_z, 0, normals_z.length );
		this.radiuses = new double[radiuses.length];
		System.arraycopy( radiuses, 0, this.radiuses, 0, radiuses.length );

	}

	public SegmentedConnection() {
		connections = new ArrayList< Connection >();
	}

	public SegmentedConnection clone() {

		SegmentedConnection result = new SegmentedConnection();

		if( radiuses != null ) {
			this.radiuses = new double[radiuses.length];
			System.arraycopy( radiuses, 0, result.radiuses, 0, radiuses.length );
		}
		if( normals_x != null ) {
			this.normals_x = new double[normals_x.length];
			System.arraycopy( normals_x, 0, result.normals_x, 0, normals_x.length );
		}
		if( normals_y != null ) {
			this.normals_y = new double[normals_y.length];
			System.arraycopy( normals_y, 0, result.normals_y, 0, normals_y.length );
		}
		if( normals_z != null ) {
			this.normals_z = new double[normals_z.length];
			System.arraycopy( normals_z, 0, result.normals_z, 0, normals_z.length );
		}
			
		for( Iterator i = connections.iterator();
		     i.hasNext();
			) {

			Connection c = (Connection)i.next();
			result.connections.add(c.clone());

		}
			
		return result;
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

	public int numberOfPoints() {
		int result = 0;
		int segments_in_path = connections.size();
		for( int j = 0; j < segments_in_path; ++j ) {
			Connection connection = (Connection)connections.get(j);
			result += connection.size();
		}
		return result;
	}

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
				best = (double[])x.clone();
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
	
	public SegmentedConnection fitCircles( int side, SimpleNeuriteTracer_ plugin, boolean display ) {

		boolean verbose = false;

		SegmentedConnection fitted = new SegmentedConnection();

		// System.out.println("Generating normal planes stack.");

		int totalPoints = numberOfPoints();

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

		int currentPoint = 0;

		// We assume that the first and the last in the stack are fine; 

		double [] centre_x_positions = new double[totalPoints];
		double [] centre_y_positions = new double[totalPoints];
		double [] radiuses = new double[totalPoints];

		double [] normals_x = new double[totalPoints];
		double [] normals_y = new double[totalPoints];
		double [] normals_z = new double[totalPoints];
		
		int segments_in_path = connections.size();
		for( int j = 0; j < segments_in_path; ++j ) {
			Connection connection = (Connection)connections.get(j);
			Connection fittedConnection = new Connection(connection.points);
			for( int k = 0; k < connection.points; ++k ) {

				IJ.showProgress( currentPoint / (float)totalPoints );

				int x = connection.x_positions[k];
				int y = connection.y_positions[k];
				int z = connection.z_positions[k];

				if( verbose )
					System.out.println("Considering point: "+last_x+","+last_y+","+last_z);

				if( (last_x < 0) || (second_last_x < 0) ) {
					
					if( last_x >= 0 ) {
						// Then this is the
						// first real point.
						// We won't generate a
						// normal plane, since
						// we can't trust the
						// normal vector(well,
						// maybe) but add an
						// empty slice so
						// everything is in
						// sync.

						fittedConnection.addPoint( last_x, last_y, last_z );

						if( verbose )
							System.out.println("Adding empty slice.");
						
						byte [] empty = new byte[side*side];
						ByteProcessor bp = new ByteProcessor( side, side );
						bp.setPixels(empty);
						stack.addSlice(null,bp);

						++currentPoint;

					}
					

				} else {

					/* Then the last two points
					   were valid, so assume the
					   tanget vector at last_* is
					   the difference between this
					   point and
					   second_last_x... */
					
					int x_diff = x - second_last_x;
					int y_diff = y - second_last_y;
					int z_diff = z - second_last_z;

					// These are returned; they
					// *are* scaled with the _scaling variables

					double [] x_basis_in_plane = new double[3]; 
					double [] y_basis_in_plane = new double[3];

					byte [] normalPlane = plugin.squareNormalToVector(
						side,
						x_spacing,   // step is in the same units as the _spacing, etc. variables.
						last_x,     // These are are *not* yet scaled in z
						last_y,     // They're just sample point differences
						last_z,
						x_diff,
						y_diff,
						z_diff,
						x_basis_in_plane,
						y_basis_in_plane );

					/* Now at this stage, try to
					   optimize a circle in
					   there... */

					// n.b. thes aren't normalized
					normals_x[currentPoint] = x_diff * x_spacing;
					normals_y[currentPoint] = y_diff * y_spacing;
					normals_z[currentPoint] = z_diff * z_spacing;
					
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
					for( int i = 0; i < (side * side); ++i ) {
						int value = normalPlane[i]&0xFF;
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

					centre_x_positions[currentPoint] = startValues[0];
					centre_y_positions[currentPoint] = startValues[1];
					radiuses[currentPoint] = startValues[2];

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
						
					fittedConnection.addPoint( x_in_image, y_in_image, z_in_image );
					
					if( verbose )
						System.out.println("Adding a real slice.");

					ByteProcessor bp = new ByteProcessor( side, side );
					bp.setPixels(normalPlane);
					stack.addSlice(null,bp);

					++ currentPoint;

				}

				second_last_x = last_x;
				second_last_y = last_y;
				second_last_z = last_z;
				
				last_x = x;
				last_y = y;
				last_z = z;

			}

			fitted.addConnection( fittedConnection );

		}

		// Add an extra empty slice for the final one:

		System.out.println("Adding empty slice at the end.");
						
		byte [] empty = new byte[side*side];
		ByteProcessor bp = new ByteProcessor( side, side );
		bp.setPixels(empty);
		stack.addSlice(null,bp);

		IJ.showProgress( 1.0 );

		fitted.setFittedCircles( normals_x,
					 normals_y,
					 normals_z,
					 radiuses );

		if( display ) {

			ImagePlus imp = new ImagePlus( "normal stack", stack );

			NormalPlaneCanvas normalCanvas = new NormalPlaneCanvas(
				imp,
				plugin,
				centre_x_positions,
				centre_y_positions,
				radiuses,
				fitted );

			new StackWindow( imp, normalCanvas );

			imp.show();

		}

		return fitted;

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
