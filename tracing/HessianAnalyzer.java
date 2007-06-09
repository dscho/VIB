/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import math3d.JacobiFloat;

import util.Arrow;
import util.ArrowDisplayer;

import java.awt.Color;

public class HessianAnalyzer {
	
	byte [][] slices_data;
	
	int width, height, depth;
	double x_spacing, y_spacing, z_spacing;
	
	ArrowDisplayer arrowDisplayer;
	
	public void setArrowDisplayer( ArrowDisplayer arrowDisplayer ) {
		this.arrowDisplayer = arrowDisplayer;
	}
	
	public HessianAnalyzer( byte [][] slices_data, int width, int height, int depth,
				double x_spacing,
				double y_spacing,
				double z_spacing ) {
		
		arrowDisplayer = null;
		
		this.width = width;
		this.height = height;
		this.depth = depth;
		
		this.x_spacing = x_spacing;
		this.y_spacing = y_spacing;
		this.z_spacing = z_spacing;
		
		this.slices_data = slices_data;
		
	}
	
	/* Calculate the hessian at a point in the image... */
	
	float [][] hessian( int [] values,
			    int either_side,
			    double sx,     // sigma_x
			    double sy,     // sigma_y
			    double sz ) {  // sigma_z
		
		int diameter = either_side * 2 + 1;
		int points = diameter * diameter * diameter;
		
		/* We assume (for the purposes of calculating the second
		   derivatives) that the continuous function at the centre
		   point is actually made up from the sum of Gaussians centred
		   at that and the surrounding points.
		*/
		
		double gxx = 0;
		double gyy = 0;
		double gzz = 0;
		
		double gxy = 0;
		double gxz = 0;
		double gyz = 0;
		
		if( points == values.length ) {

/*            
            for( int i = 0; i < points; ++i ) {
                
                int x_grid = (i % diameter) - either_side;
                int y_grid = ((i % (diameter*diameter)) / diameter) - either_side;
                int z_grid = (i / (diameter*diameter)) - either_side;

                int A = values[i];

                double x = - x_grid * x_spacing;
                double y = - y_grid * y_spacing;
                double z = - z_grid * z_spacing;

                double sx2 = sx*sx;
                double sy2 = sy*sy;
                double sz2 = sz*sz;

            }
*/

			for( int i = 0; i < points; ++i ) {
				
				int x_grid = (i % diameter) - either_side;
				int y_grid = ((i % (diameter*diameter)) / diameter) - either_side;
				int z_grid = (i / (diameter*diameter)) - either_side;
				
				int A = values[i];
				
				double x = - x_grid * x_spacing;
				double y = - y_grid * y_spacing;
				double z = - z_grid * z_spacing;
				
				double sx2 = sx*sx;
				double sy2 = sy*sy;
				double sz2 = sz*sz;
				
				double squares_product = sx2*sy2*sz2;
				
				double in_exp = (- sx2*sy2*z*z - sx2*sz2*y*y - sy2*sz2*x*x) / squares_product;
				
				gxx +=
					(4*A*x*x - 2*A*sx2) * Math.exp( in_exp ) / ( sx2*sx2 );
				
				gxy +=
					4*A*x*y * Math.exp( in_exp ) / ( sx2*sy2 );
				
				gxz +=
					4*A*x*z * Math.exp( in_exp ) / ( sx2*sz2 );
				
				gyy +=
					(4*A*y*y - 2*A*sy2) * Math.exp( in_exp ) / ( sy2*sy2 );
				
				gyz +=
					4*A*y*z * Math.exp( in_exp ) / ( sy2*sz2 );
				
				gzz +=
					(4*A*z*z - 2*A*sz2) * Math.exp( in_exp ) / ( sz2*sz2 );
				
			}
			
			float [][] hessian = new float[3][3];
			
			hessian[0][0] = (float)gxx;
			hessian[0][1] = (float)gxy;
			hessian[0][2] = (float)gxz;
			
			hessian[1][0] = (float)gxy;
			hessian[1][1] = (float)gyy;
			hessian[1][2] = (float)gyz;
			
			hessian[2][0] = (float)gxz;
			hessian[2][1] = (float)gyz;
			hessian[2][2] = (float)gzz;
			
			return hessian;
			
		} else
			return null;
		
	}
	
	public EigenResultsDouble analyzeAtPoint( int x, int y, int z, int either_side, boolean interactive ) {
		
		int i;
		
		if( x < either_side || x >= (width - either_side) )
			return null;
		if( y < either_side || y >= (height - either_side) )
			return null;
		if( z < either_side || z >= (depth - either_side) )
			return null;
		
		int diameter = either_side * 2 + 1;
		int points = diameter * diameter * diameter;
		
		int [] values_around_point = new int[points];
		
		byte [][] relevant_pixels = new byte[diameter][];
		
		for( i = 0; i < diameter; ++i ) {        
			relevant_pixels[i] = slices_data[ z + (i - either_side) ];
			// relevant_pixels[i] = (byte []) original_stack.getPixels( z + (i-1) );
		}
		
		for( i = 0; i < points; ++i ) {
			
			int x_grid = (i % diameter) - either_side;
			int y_grid = ((i % (diameter * diameter)) / diameter) - either_side;
			int z_grid = (i / (diameter * diameter)) - either_side;
			
			byte [] pixels;
			
			values_around_point[i] = (int)( relevant_pixels[z_grid + either_side][ (y + y_grid) * width + (x + x_grid) ] & 0xFF );
			
		}
		
		float [][] hessian = hessian( values_around_point,
					      either_side,
					      x_spacing / 2,
					      y_spacing / 2,
					      z_spacing / 2 );
		
		JacobiFloat jc = new JacobiFloat( hessian );
		
		float[] eigenValuesFloat=jc.getEigenValues();
		float[][] eigenVectorMatrixFloat=jc.getEigenVectors();
		
		// Remarkably, I think that the bit down to // --- is quicker
		// than just sorting them.
		
		int smallestEValueIndex = 0;
		for( i = 1; i < 3; ++i ) {
			if( Math.abs(eigenValuesFloat[i]) < Math.abs(eigenValuesFloat[smallestEValueIndex]) ) {
				smallestEValueIndex = i;
			}
		}
		
		int largestEValueIndex = 2;
		for( i = 0; i < 2; ++i ) {
			if( Math.abs(eigenValuesFloat[i]) > Math.abs(eigenValuesFloat[largestEValueIndex]) ) {
				largestEValueIndex = i;
			}
		}
		int middleEValueIndex = 1;
		for( i = 0; i < 3; ++i ) {
			if( (i != smallestEValueIndex) && (i != largestEValueIndex) ) {
				middleEValueIndex = i;
			}
		}
		
		if( interactive ) {
			
			for( i = 0; i < 3; ++i )
				System.out.println( "evalue [" + i + "]: " + eigenValuesFloat[i] );
			
			System.out.println( "small " + smallestEValueIndex +
					    ", middle " + middleEValueIndex +
					    ", large " + largestEValueIndex );
			
		}
		
		// -------------------------------------------------------------
		
		double [] valuesSorted=new double[3];
		
		valuesSorted[0] = eigenValuesFloat[smallestEValueIndex];
		valuesSorted[1] = eigenValuesFloat[middleEValueIndex];
		valuesSorted[2] = eigenValuesFloat[largestEValueIndex];
		
		double[][] vectorsSorted=new double[3][3];
		
		vectorsSorted[0][0] = eigenVectorMatrixFloat[0][smallestEValueIndex];
		vectorsSorted[0][1] = eigenVectorMatrixFloat[1][smallestEValueIndex];
		vectorsSorted[0][2] = eigenVectorMatrixFloat[2][smallestEValueIndex];
		
		vectorsSorted[1][0] = eigenVectorMatrixFloat[0][middleEValueIndex];
		vectorsSorted[1][1] = eigenVectorMatrixFloat[1][middleEValueIndex];
		vectorsSorted[1][2] = eigenVectorMatrixFloat[2][middleEValueIndex];
		
		vectorsSorted[2][0] = eigenVectorMatrixFloat[0][largestEValueIndex];
		vectorsSorted[2][1] = eigenVectorMatrixFloat[1][largestEValueIndex];
		vectorsSorted[2][2] = eigenVectorMatrixFloat[2][largestEValueIndex];
		
		// -------------------------------------------------------------
		
		if( interactive ) {
			
			System.out.println( "smallest evalue: " + valuesSorted[0] );
			
			System.out.println( "   v_x: " + vectorsSorted[0][0] );
			System.out.println( "   v_y: " + vectorsSorted[0][1] );
			System.out.println( "   v_z: " + vectorsSorted[0][2] );
			
			System.out.println( "middle evalue: " + valuesSorted[1] );
			
			System.out.println( "   v_x: " + vectorsSorted[1][0] );
			System.out.println( "   v_y: " + vectorsSorted[1][1] );
			System.out.println( "   v_z: " + vectorsSorted[1][2] );
			
			System.out.println( "largest evalue: " + valuesSorted[2] );
			
			System.out.println( "   v_x: " + vectorsSorted[2][0] );
			System.out.println( "   v_y: " + vectorsSorted[2][1] );
			System.out.println( "   v_z: " + vectorsSorted[2][2] );
			
			if( arrowDisplayer != null ) {
				
				Arrow a = new Arrow( Color.RED, x, y, z,
						     vectorsSorted[0][0],
						     vectorsSorted[0][1],
						     vectorsSorted[0][2],
						     100  );
				
				arrowDisplayer.setNewArrow( a );
			}
		}
		
		EigenResultsDouble er = new EigenResultsDouble();
		er.sortedValues = valuesSorted;
		er.sortedVectors = vectorsSorted;
		
		return er;
		
		/*

        float[][] second_derivatives =
            new float[3][3];
    
        double delta_x;
        double delta_y;
        double delta_z;

        double delta_x_plus_e_x;
        double delta_y_plus_e_x;
        double delta_z_plus_e_x;

        double delta_x_plus_e_y;
        double delta_y_plus_e_y;
        double delta_z_plus_e_y;

        double delta_x_plus_e_z;
        double delta_y_plus_e_z;
        double delta_z_plus_e_z;

        if( x == width - 1 )
            x = width - 2;

        if( y == height - 1 )
            y = height - 2;

        if( z == depth - 1 )
            z = depth - 2;

        delta_x = div[ z ][ y * (width * 3) + x ] / x_spacing;

        delta_x_plus_e_x = div[ z ][ y * (width * 3) + (x + 3) ] / x_spacing;
        delta_x_plus_e_y = div[ z ][ (y + 1) * (width * 3) + x ] / y_spacing;
        delta_x_plus_e_z = div[ z + 1 ][ y * (width * 3) + x ] / z_spacing;

        delta_y = div[ z ][ y * (width * 3) + x + 1 ] / y_spacing;
        delta_y_plus_e_x = div[ z ][ y * (width * 3) + (x + 3) + 1 ] / y_spacing;
        delta_y_plus_e_y = div[ z ][ (y + 1) * (width * 3) + x + 1 ] / y_spacing;
        delta_y_plus_e_z = div[ z + 1 ][ y * (width * 3) + x + 1 ] / y_spacing;

        delta_z = div[ z - 1 ][ y * (width * 3) + x + 2 ] / z_spacing;
        delta_z_plus_e_x = div[ z ][ y * (width * 3) + (x + 3) + 2 ] / z_spacing;
        delta_z_plus_e_y = div[ z ][ (y + 1) * (width * 3) + x + 2 ] / z_spacing;
        delta_z_plus_e_z = div[ z + 1 ][ y * (width * 3) + x + 2 ] / z_spacing;

        double change_in_delta_x_wrt_x = (delta_x_plus_e_x - delta_x) / x_spacing;
        double change_in_delta_y_wrt_x = (delta_y_plus_e_x - delta_y) / x_spacing;
        double change_in_delta_z_wrt_x = (delta_z_plus_e_x - delta_z) / x_spacing;
        
        double change_in_delta_x_wrt_y = (delta_x_plus_e_y - delta_x) / y_spacing;
        double change_in_delta_y_wrt_y = (delta_y_plus_e_y - delta_y) / y_spacing;
        double change_in_delta_z_wrt_y = (delta_z_plus_e_y - delta_z) / y_spacing;
        
        double change_in_delta_x_wrt_z = (delta_x_plus_e_z - delta_x) / z_spacing;
        double change_in_delta_y_wrt_z = (delta_y_plus_e_z - delta_y) / z_spacing;
        double change_in_delta_z_wrt_z = (delta_z_plus_e_z - delta_z) / z_spacing;

        System.out.println( "dxx: " + change_in_delta_x_wrt_x );
        System.out.println( "dyx: " + change_in_delta_y_wrt_x );
        System.out.println( "dzx: " + change_in_delta_z_wrt_x );

        System.out.println( "dxy: " + change_in_delta_x_wrt_y );
        System.out.println( "dyy: " + change_in_delta_y_wrt_y );
        System.out.println( "dzy: " + change_in_delta_z_wrt_y );

        System.out.println( "dxz: " + change_in_delta_x_wrt_z );
        System.out.println( "dyz: " + change_in_delta_y_wrt_z );
        System.out.println( "dzz: " + change_in_delta_z_wrt_z );

        second_derivatives[0][0] = (float)change_in_delta_x_wrt_x;
        second_derivatives[0][1] = (float)change_in_delta_y_wrt_x;
        second_derivatives[0][2] = (float)change_in_delta_z_wrt_x;

        second_derivatives[1][0] = (float)change_in_delta_x_wrt_y;
        second_derivatives[1][1] = (float)change_in_delta_y_wrt_y;
        second_derivatives[1][2] = (float)change_in_delta_z_wrt_y;

        second_derivatives[2][0] = (float)change_in_delta_x_wrt_z;
        second_derivatives[2][1] = (float)change_in_delta_y_wrt_z;
        second_derivatives[2][2] = (float)change_in_delta_z_wrt_z;

        */
	}
	
}
