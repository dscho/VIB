/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;

public class Mask_Of_Nearby_Points implements PlugIn {
	
	public void run(String ignored) {
		
		ImagePlus imagePlus = IJ.getImage();
		if (imagePlus == null) {
			IJ.error("No image to operate on.");
			return;
		}
		
		Calibration c = imagePlus.getCalibration();
		
		int type = imagePlus.getType();
		
		if ( ! (ImagePlus.GRAY8 == type || ImagePlus.COLOR_256 == type) ) {
			IJ.error("The image must be either 8 bit for this plugin.");
			return;
		}
		
		int width = imagePlus.getWidth();
		int height = imagePlus.getHeight();
		int depth = imagePlus.getStackSize();
		
		if (width * height * depth > Integer.MAX_VALUE) {
			IJ.error("This stack is too large for this plugin (must have less than " + Integer.MAX_VALUE + " points.");
			return;
		}
		
		double defaultDistance = c.pixelWidth * 3;
		
		GenericDialog gd = new GenericDialog("Mask Of Nearby Points Options");
		
		gd.addNumericField("Add to mask points within distance ("+c.getUnits()+")", defaultDistance, 4);
		gd.addNumericField("... of points with value at least: ", 128, 0);
		
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		
		double withinDistance = gd.getNextNumber();
		double minimumValue = gd.getNextNumber();
		
		int valuesEitherSideInX = (int)( withinDistance / c.pixelWidth );
		int valuesEitherSideInY = (int)( withinDistance / c.pixelHeight );
		int valuesEitherSideInZ = (int)( withinDistance / c.pixelDepth );
		
		float [][][] kernel = new float[2*valuesEitherSideInZ+1][2*valuesEitherSideInY+1][2*valuesEitherSideInX+1];
		for( int z = -valuesEitherSideInZ; z <= valuesEitherSideInZ; ++z ) {
			for( int y = -valuesEitherSideInY; y <= valuesEitherSideInY; ++y ) {
				for( int x = -valuesEitherSideInX; x <= valuesEitherSideInX; ++x ) {
					
					double xdiff = x * c.pixelWidth;
					double ydiff = y * c.pixelHeight;
					double zdiff = z * c.pixelDepth;
					
					float distance = (float) Math.sqrt(xdiff*xdiff+ydiff*ydiff+zdiff*zdiff);
					
					kernel[z+valuesEitherSideInZ][y+valuesEitherSideInY][x+valuesEitherSideInX] = distance;

				}
			}
		}

		ImageStack stack=imagePlus.getStack();
		
		byte [][] sliceData = new byte[depth][];
		for( int z = 0; z < depth; ++z ) {
			ByteProcessor bp = (ByteProcessor) stack.getProcessor(z+1);
			sliceData[z] = (byte[]) bp.getPixelsCopy();
		}

		
		float [][] distancesToNearestPoints = new float[depth][];
		for( int z = 0; z < depth; ++z ) {
			distancesToNearestPoints[z] = new float[width*height];
			for( int y = 0; y < height; ++y )
				for( int x = 0; x < width; ++x )
					distancesToNearestPoints[z][y*width+x] = Float.MAX_VALUE;
		}
		
		for( int z = 0; z < depth; ++z ) {
			IJ.showProgress( z / (double)depth );
			for( int y = 0; y < height; ++y ) {
				for( int x = 0; x < width; ++x ) {
					
					int value = sliceData[z][y*width+x] & 0xFF;
					
					if( value < minimumValue )
						continue;
					
					int min_x = - valuesEitherSideInX;
					int min_y = - valuesEitherSideInY;
					int min_z = - valuesEitherSideInZ;
					
					int max_x = valuesEitherSideInX;
					int max_y = valuesEitherSideInY;
					int max_z = valuesEitherSideInZ;
					
					if( x < valuesEitherSideInX )
						min_x = -x;
					if( y < valuesEitherSideInY )
						min_y = -y;
					if( z < valuesEitherSideInZ )
						min_z = -z;
					
					// if x is width - 1 then
					// max_x should end up as 0
					// it's only necessary to adjust max_x if valuesEitherSideInX is greater than 0
					//    
					
					if( x >= (width - valuesEitherSideInX) )
						max_x = (width - 1) - x;
					if( y >= (height - valuesEitherSideInY) )
						max_y = (height - 1) - y;
					if( z >= (depth - valuesEitherSideInZ) )
						max_z = (depth - 1) - z;

					for (int kernel_z = min_z; kernel_z <= max_z; ++kernel_z) {
						for (int kernel_y = min_y; kernel_y <= max_y; ++kernel_y) {
							for (int kernel_x = min_x; kernel_x <= max_x; ++kernel_x) {
								float newDistance = kernel[kernel_z + valuesEitherSideInZ][kernel_y + valuesEitherSideInY][kernel_x + valuesEitherSideInX];
								int offset_i = (y+kernel_y) * width + (x+kernel_x);
								int offset_z = z + kernel_z;
								if (newDistance < distancesToNearestPoints[offset_z][offset_i]) {
									distancesToNearestPoints[offset_z][offset_i] = newDistance;
								}
							}
						}
					}
				}
			}
		}

/*	
		ImageStack distanceStack = new ImageStack(width,height);
			
		for( int z = 0; z < depth; ++z ) {
			IJ.showProgress( z / (double)depth );
			FloatProcessor fp = new FloatProcessor(width,height);
			fp.setPixels(distancesToNearestPoints[z]);
			distanceStack.addSlice("",fp);
		}
		IJ.showProgress(1.0);

		ImagePlus distanceImagePlus = new ImagePlus("distances",distanceStack);
		distanceImagePlus.show();
*/
			
		ImageStack newStack = new ImageStack(width,height);
			
		for( int z = 0; z < depth; ++z ) {
			IJ.showProgress( z / (double)depth );
			byte [] newSlice = new byte[width*height];
			for( int y = 0; y < height; ++y ) {
				for( int x = 0; x < width; ++x ) {
					if( distancesToNearestPoints[z][y*width+x] <= withinDistance ) {
						newSlice[y*width+x] = (byte)255;
					}
				}
			}
			ByteProcessor bp = new ByteProcessor(width,height);
			bp.setPixels(newSlice);
			newStack.addSlice("",bp);
		}
		IJ.showProgress(1.0);

		ImagePlus newImagePlus = new ImagePlus("within distance "+withinDistance+" of values over "+minimumValue,newStack);
		newImagePlus.show();

	}
}
