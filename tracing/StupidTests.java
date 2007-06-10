/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import math3d.JacobiFloat;

public class StupidTests {


	public static void eigenTest() {

		float [][] transform = new float[3][3];
		transform[0][0] = 1; transform[0][1] = 2; transform[0][2] = 0;
		transform[1][0] = 2; transform[1][1] = 1; transform[1][2] = 0;
		transform[2][0] = 0; transform[2][1] = 0; transform[2][2] = 1;
		
		JacobiFloat jc = new JacobiFloat(transform,200);

		float [] evals = jc.getEigenValues();
		float [][] evecs = jc.getEigenVectors();
		
		System.out.println( "eval0: " + evals[0] );
		System.out.println( "eval1: " + evals[1] );
		System.out.println( "eval2: " + evals[2] );
		
		System.out.println( "evec0: " + evecs[0][0] + ", " + evecs[0][1] + ", " + evecs[0][2] );
		System.out.println( "evec1: " + evecs[1][0] + ", " + evecs[1][1] + ", " + evecs[1][2] );
		System.out.println( "evec2: " + evecs[2][0] + ", " + evecs[2][1] + ", " + evecs[2][2] );
		
		// The other way round (below) seems to be correct:
		
		System.out.println( "other evec0: " + evecs[0][0] + ", " + evecs[1][0] + ", " + evecs[2][0] );
		System.out.println( "other evec1: " + evecs[0][1] + ", " + evecs[1][1] + ", " + evecs[2][1] );
		System.out.println( "other evec2: " + evecs[0][2] + ", " + evecs[1][2] + ", " + evecs[2][2] );

	}

	public static ImagePlus xAxisTube( int side ) {

		int mid = side / 2;

		ImageStack stack = new ImageStack( side, side );
		
		for( int i = 0; i < side; ++i ) {
			byte [] b = new byte[side*side];
			if( i == mid )
				for( int j = 0; j < side; ++j )
					b[ mid * side + j ] = (byte)255;
			ByteProcessor bp = new ByteProcessor(side,side);
			bp.setPixels( b );
			stack.addSlice( null, bp );
		}
		
		ImagePlus newImage = new ImagePlus( "Test X Axis", stack );

		return newImage;
	}

	public static ImagePlus yAxisTube( int side ) {
		
		int mid = side / 2;

		ImageStack stack2 = new ImageStack( side, side );

		for( int i = 0; i < side; ++i ) {
			byte [] b = new byte[side*side];
			if( i == mid )
				for( int j = 0; j < side; ++j )
					b[ j * side + mid ] = (byte)255;
			ByteProcessor bp = new ByteProcessor(side,side);
			bp.setPixels( b );
			stack2.addSlice( null, bp );
		}
		
		ImagePlus newImage = new ImagePlus( "Test Y Axis", stack2 );

		return newImage;
	}

	public static ImagePlus zAxisTube( int side ) {

		int mid = side / 2;

		ImageStack stack = new ImageStack( side, side );

		for( int i = 0; i < side; ++i ) {
			byte [] b = new byte[side*side];
			b[ mid * side + mid ] = (byte)255;
			ByteProcessor bp = new ByteProcessor(side,side);
			bp.setPixels( b );
			stack.addSlice( null, bp );
		}
		
		ImagePlus newImage = new ImagePlus( "Test Z Axis", stack );

		return newImage;

	}

	public static void generateEigenSizeOfSmallest(  SimpleNeuriteTracer_ tracerPlugin ) {
	
		int width = tracerPlugin.width;
		int height = tracerPlugin.height;
		int depth = tracerPlugin.depth;

		IJ.showStatus( "Now going through the image..." );
		
		IJ.showProgress( 0.0 );
		
		int either_side = 2;
		
		ImageStack eigenStack = new ImageStack( width - 2 * either_side, height - 2 * either_side );
			
		for( int z = 0; z < depth; ++z ) {
			
			byte [] small = new byte[ (width - 2 * either_side) * (height - 2 * either_side) ];
			byte [] ratio = new byte[ (width - 2 * either_side) * (height - 2 * either_side) ];
			byte [] simil = new byte[ (width - 2 * either_side) * (height - 2 * either_side) ];
				
			ColorProcessor cp = new ColorProcessor( width - 2 * either_side, height - 2 * either_side );
			
			for( int y = 0; y < height; ++y ) {
				for( int x = 0; x < width; ++x ) {
					
					if( x < either_side || x >= (width - either_side) )
						continue;
					if( y < either_side || y >= (height - either_side) )
						continue;
					if( z < either_side || z >= (depth - either_side) )
						continue;
					
					EigenResultsDouble er;
					try {
						er = tracerPlugin.hessianAnalyzer.analyzeAtPoint( x, y, z, either_side, 1.0f, false );
					} catch( Exception exception ) {
						IJ.error("Caught an exception while calculating the Hessian: "+exception);
						return;
					}
					
					int v = (int) ( 255.0 - Math.abs(er.sortedValues[0]) );
					if( v < 0 )
						v = 0;
					if( v > 255 )
						v = 255;
					
					small[ (y - either_side) * (width - 2 * either_side) + (x - either_side) ] = (byte) v;
					
					int r = 0;
					
					if( Math.abs(er.sortedValues[0]) >= 0.00001 ) {
						
						r = (int) ( 100.0 * (Math.abs(er.sortedValues[1]) / Math.abs( er.sortedValues[0])) );
						if( r < 0 )
							r = 0;
						if( r > 255 )
							r = 255;
						
					}
					
					ratio[ (y - either_side) * (width - 2 * either_side) + (x - either_side) ] = (byte) r;
					
					int s = 0;
					
					if( Math.abs(er.sortedValues[2]) >= 0.00001 ) {
						
						double diff = Math.abs( Math.abs(er.sortedValues[1]) - Math.abs(er.sortedValues[2]) );
						
						double proportion = 1 - (diff / Math.abs(er.sortedValues[2]));
						
						s = (int)( proportion * 255 );
						
						}
					
						simil[ (y - either_side) * (width - 2 * either_side) + (x - either_side) ] = (byte) s;
						
					}
			}
				
			cp.setRGB( ratio, small, simil );
				eigenStack.addSlice( null, cp );
				
				System.out.println( "slice: " + z + " (" + depth + ")" );
				
				IJ.showProgress( z / depth );
		}
			
		ImagePlus eigenImagePlus = new ImagePlus( "smallness of smallest eigenvalue", eigenStack );
			eigenImagePlus.show();
			
			IJ.showProgress( 100.0 );
			
	}

	public void generateEigenMeasures( byte [][] slices_data,
					   int width,
					   int height,
					   int depth,
					   double x_spacing,
					   double y_spacing,
					   double z_spacing,	   
					   int either_side ) {

		HessianAnalyzer hessianAnalyzer = new HessianAnalyzer( slices_data,
								       width,
								       height,
								       depth,
								       x_spacing,
								       y_spacing,
								       z_spacing );

		IJ.showStatus( "Now going through the image..." );
		
		IJ.showProgress( 0.0 );
		
		ImageStack eigenStack = new ImageStack( width - 2 * either_side, height - 2 * either_side );
		
		for( int z = 0; z < depth; ++z ) {
			
			byte [] small = new byte[ (width - 2 * either_side) * (height - 2 * either_side) ];
			byte [] ratio = new byte[ (width - 2 * either_side) * (height - 2 * either_side) ];
			byte [] simil = new byte[ (width - 2 * either_side) * (height - 2 * either_side) ];
			
			ColorProcessor cp = new ColorProcessor( width - 2 * either_side, height - 2 * either_side );
			
			for( int y = 0; y < height; ++y ) {
				for( int x = 0; x < width; ++x ) {
					
					if( x < either_side || x >= (width - either_side) )
						continue;
					if( y < either_side || y >= (height - either_side) )
						continue;
					if( z < either_side || z >= (depth - either_side) )
						continue;
					
					EigenResultsDouble er;
					try {
						er = hessianAnalyzer.analyzeAtPoint( x, y, z, either_side, 1.0f, false );
					}  catch( Exception exception ) {
						IJ.error("Caught an exception while calculating the Hessian: "+exception);
						return;
					}
					
					int v = (int) ( 255.0 - Math.abs(er.sortedValues[0]) );
					if( v < 0 )
						v = 0;
					if( v > 255 )
						v = 255;
					
					small[ (y - either_side) * (width - 2 * either_side) + (x - either_side) ] = (byte) v;
					
					int r = 0;
					
					if( Math.abs(er.sortedValues[0]) >= 0.00001 ) {
						
						r = (int) ( 100.0 * (Math.abs(er.sortedValues[1]) / Math.abs( er.sortedValues[0])) );
						if( r < 0 )
							r = 0;
						if( r > 255 )
							r = 255;
						
					}
					
					ratio[ (y - either_side) * (width - 2 * either_side) + (x - either_side) ] = (byte) r;
					
					int s = 0;
					
					if( Math.abs(er.sortedValues[2]) >= 0.00001 ) {
						
						double diff = Math.abs( Math.abs(er.sortedValues[1]) - Math.abs(er.sortedValues[2]) );
						
						double proportion = 1 - (diff / Math.abs(er.sortedValues[2]));
						
						s = (int)( proportion * 255 );
						
					}
					
					simil[ (y - either_side) * (width - 2 * either_side) + (x - either_side) ] = (byte) s;
					
				}
			}
			
			cp.setRGB( ratio, small, simil );
			eigenStack.addSlice( null, cp );
			
			System.out.println( "slice: " + z + " (" + depth + ")" );
			
			IJ.showProgress( z / depth );
		}
		
		ImagePlus eigenImagePlus = new ImagePlus( "smallness of smallest eigenvalue", eigenStack );
		eigenImagePlus.show();
		
		IJ.showProgress( 100.0 );
		
	}

// ------------------------------------------------------------------------
						
/*
        for( int z = 0; z < depth; ++z ) {

            System.gc();

            byte [] changes_minus_1 = null;
            byte [] changes_current = null;
            byte [] changes_plus_1 = null;

            for( int y = 0; y < height; ++y ) {

                for( int x = 0; x < width; ++x ) {

                    double

                    // So at this point, calculate the matrix in
                    // doubles and eigenvalues.




                    // delta x
                    if( x == width - 1 ) {
                        // FIXME: the boundary case; assume it's the
                        // same as the last one...
                        change_in_change_x =
                            (short) ( (int) (pixels_current[ y * width + x ] & 0xFF) -
                                      (int) (pixels_current[ y * width + (x - 1) ] & 0xFF) );
                    } else {
                        change_in_change_x =
                            (short) ( (int) (pixels_current[ y * width + (x + 1) ] & 0xFF) -
                                      (int) (pixels_current[ y * width + x ] & 0xFF) );
                    }

                    // delta y
                    if( y == height - 1 ) {
                        // FIXME: the boundary case; assume it's the
                        // same as the last one...
                        change_in_change_y =
                            (short) ( (int) (pixels_current[ y * width + x ] & 0xFF) -
                                      (int) (pixels_current[ (y - 1) * width + x ] & 0xFF) );
                    } else {
                        change_in_change_y =
                            (short) ( (int) (pixels_current[ (y + 1) * width + x ] & 0xFF) -
                                      (int) (pixels_current[ y * width + x ] & 0xFF) );
                    }

                    // delta z
                    if( z == depth - 1 ) {
                        // FIXME: the boundary case; assume it's the
                        // same as the last one...
                        change_in_change_z =
                            (short) ( (int) (pixels_current[ y * width + x ] & 0xFF) -
                                      (int) (pixels_minus_1[ y * width + x ] & 0xFF ) );
                    } else {
                        change_in_change_z =
                            (short) ( (int) (pixels_plus_1[ y * width + x ] & 0xFF) -
                                      (int) (pixels_current[ y * width + x ] & 0xFF) );
                    }





                    short change_in_value_x;
                    short change_in_value_y;
                    short change_in_value_z;





                    int delta_x_index_in_div_slice_index = y * (width * 3) + x * 3;
                    int delta_y_index_in_div_slice_index = y * (width * 3) + x * 3 + 1;
                    int delta_z_index_in_div_slice_index = y * (width * 3) + x * 3 + 2;

                    div[z][delta_x_index_in_div_slice_index] = change_in_value_x;
                    div[z][delta_y_index_in_div_slice_index] = change_in_value_y;
                    div[z][delta_z_index_in_div_slice_index] = change_in_value_z;

                }

            }

        }
*/
	
}
