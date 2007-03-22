/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package stacks;

import ij.*;
import ij.process.ByteProcessor;
import ij.gui.*;

import java.lang.reflect.*;

import java.io.*;

class ThreePanes {
	
 	public static final int XY_PLANE = 0;
	public static final int XZ_PLANE = 1;
	public static final int ZY_PLANE = 2;
	
	protected ImagePlus xy;
	protected ImagePlus xz;
	protected ImagePlus zy;
	
	protected ThreePanesCanvas xy_canvas;
	protected ThreePanesCanvas xz_canvas;
	protected ThreePanesCanvas zy_canvas;
	
	protected ImageCanvas original_xy_canvas;
	
	protected StackWindow xy_window;
	protected StackWindow xz_window;
	protected StackWindow zy_window;
	
	public void findPointInStack( int x_in_pane, int y_in_pane, int plane, int [] point ) {
		
		switch( plane ) {
			
		case ThreePanes.XY_PLANE:
		{
			point[0] = x_in_pane;
			point[1] = y_in_pane;
			point[2] = xy.getCurrentSlice( ) - 1;
		}
		break;
		
		case ThreePanes.XZ_PLANE:
		{
			point[0] = x_in_pane;
			point[1] = xz.getCurrentSlice( ) - 1;
			point[2] = y_in_pane;
		}
		break;
		
		case ThreePanes.ZY_PLANE:
		{
			point[0] = zy.getCurrentSlice( ) - 1;
			point[1] = y_in_pane;
			point[2] = x_in_pane;
		}
		break;
		
		}        
		
	}

	public ThreePanesCanvas createCanvas( ImagePlus imagePlus, int plane ) {
		return new ThreePanesCanvas( imagePlus, this, plane );
	}
	
	public void mouseMovedTo( int off_screen_x, int off_screen_y, int in_plane ) {
		
		int point[] = new int[3];
		
		findPointInStack( off_screen_x, off_screen_y, in_plane, point );
		
		xy_canvas.setCrosshairs( point[0], point[1], point[2], true /* in_plane != XY_PLANE */ );
		xz_canvas.setCrosshairs( point[0], point[1], point[2], true /* in_plane != XZ_PLANE */ );
		zy_canvas.setCrosshairs( point[0], point[1], point[2], true /* in_plane != ZY_PLANE */ );
		
		setSlicesAllPanes( point[0], point[1], point[2] );
	}
	
	public void setSlicesAllPanes( int new_x, int new_y, int new_z ) {
		
		xy.setSlice( new_z + 1 );
		xz.setSlice( new_y + 1 );
		zy.setSlice( new_x + 1 );
	}
	
	public void closeAndReset( ) {
		zy.close();
		xz.close();
		xy_window = new StackWindow( xy, original_xy_canvas );
	}
	
	public ThreePanes( ImagePlus imagePlus ) {
		
		xy = imagePlus;
		
		int type = xy.getType();
		if (type != ImagePlus.GRAY8) {
			IJ.error("This doesn't currently work on 8 bit stacks.");
			return;
		}
		
		original_xy_canvas = imagePlus.getWindow().getCanvas();		
		
		int width = xy.getWidth();
		int height = xy.getHeight();
		int depth = xy.getStackSize();
		int zy_width = depth;
		int zy_height = height;
		ImageStack zy_stack = new ImageStack( zy_width, zy_height );
		
		int xz_width = width;
		int xz_height = depth;
		ImageStack xz_stack = new ImageStack( xz_width, xz_height );
		
		/* Just load in the complete stack for simplicity's
		 * sake... */
		
		byte [][] slices_data = new byte[depth][];
		
		ImageStack xy_stack=xy.getStack();
		for( int z = 0; z < depth; ++z ) {
			slices_data[z] = (byte []) xy_stack.getPixels( z + 1 );
		}
		
		// Create the ZY slices:
		
		for( int x_in_original = 0; x_in_original < width; ++x_in_original ) {
			
			byte [] sliceBytes = new byte[ zy_width * zy_height ];
			
			for( int z_in_original = 0; z_in_original < depth; ++z_in_original ) {
				for( int y_in_original = 0; y_in_original < height; ++y_in_original ) {
					
					int x_in_left = z_in_original;
					int y_in_left = y_in_original;
					
					sliceBytes[ y_in_left * zy_width + x_in_left ] =
						slices_data[ z_in_original ][ y_in_original * width + x_in_original ];
				}
			}
			
			ByteProcessor bp = new ByteProcessor( zy_width, zy_height );
			bp.setPixels( sliceBytes );
			zy_stack.addSlice( null, bp );
			
			IJ.showProgress( x_in_original / (2.0 * width) );
		}
		
		zy = new ImagePlus( "ZY planes of " + xy.getShortTitle(), zy_stack );        
		
		// Create the XZ slices:
		
		for( int y_in_original = 0; y_in_original < height; ++y_in_original ) {
			
			byte [] sliceBytes = new byte[ xz_width * xz_height ];
			
			for( int z_in_original = 0; z_in_original < depth; ++z_in_original ) {
				
				// Now we can copy a complete row from
				// the original image to the XZ slice:
				
				int y_in_top = z_in_original;
				
				System.arraycopy( slices_data[z_in_original],
						  y_in_original * width,
						  sliceBytes,
						  y_in_top * xz_width,
						  width );
				
			}
			
			ByteProcessor bp = new ByteProcessor( xz_width, xz_height );
			bp.setPixels( sliceBytes );
			xz_stack.addSlice( null, bp );
			
			IJ.showProgress( 0.5 + (y_in_original / (double)height) );
		}
		
		xz = new ImagePlus( "XZ planes of " + xy.getShortTitle(), xz_stack );
		
		IJ.showProgress( 1.0 ); // Removes the progress indicator
		
		System.gc();
			       		
		xy_canvas = createCanvas( xy, XY_PLANE );
		xz_canvas = createCanvas( xz, XZ_PLANE );
		zy_canvas = createCanvas( zy, ZY_PLANE );
					
		xy_window = new StackWindow( xy, xy_canvas );
		xz_window = new StackWindow( xz, xz_canvas );
		zy_window = new StackWindow( zy, zy_canvas );
	}
}
