/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* This plugin looks for connected regions with the same value in 8
 * bit images, and optionally displays images with just each of those
 * connected regions.  (Otherwise the useful information is just printed out.)
 */

/* TODO:
     - change this so that the "connectedness" measure can be more
       flexible, e.g. connected regions where the value is above
       some threshold.
 */

package util;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import java.util.Collections;
import java.util.Iterator;
import java.util.ArrayList;
import amira.AmiraParameters;
import ij.measure.Calibration;
import java.awt.image.ColorModel;

public class Find_Connected_Regions implements PlugIn {

	/* An inner class to make the results list sortable. */
	
	private class Region implements Comparable {

		Region( int value, String materialName, int points ) {
			this.value = value;
			this.materialName = materialName;
			this.points = points;
		}

		int points;
		String materialName;
		int value;
		
		public int compareTo(Object otherRegion) {
			Region o = (Region) otherRegion;
			return (points < o.points) ? -1 : ((points > o.points) ? 1 : 0);
		}

		public String toString() {
			String materialBit = "";
			if( materialName != null ) {
				materialBit = " ("+materialName+")";
			}
			return "Region of value "+value+materialBit+" containing "+points+" points";
		}
	}
	
	private static final byte IN_QUEUE = 1;
	private static final byte ADDED = 2;
	
	public void run(String ignored) {
		
		GenericDialog gd = new GenericDialog("Find Connected Regions Options");
		gd.addCheckbox("Allow diagonal connections?", false);
		gd.addCheckbox("Display an image for each region?", true);
		
		gd.showDialog();
		if(gd.wasCanceled())
			return;		
		
		boolean diagonal = gd.getNextBoolean();
		boolean display = gd.getNextBoolean();
		
		ImagePlus imagePlus = IJ.getImage();
		if (imagePlus == null) {
			IJ.error("No image to operate on.");
			return;
		}
		
		int type = imagePlus.getType();
		
		if (!(ImagePlus.GRAY8 == type || ImagePlus.COLOR_256 == type)) {
			IJ.error("This plugin only works on 8 bit images.");
			return;
		}
		
		int width = imagePlus.getWidth();
		int height = imagePlus.getHeight();
		int depth = imagePlus.getStackSize();

		if( width * height * depth > Integer.MAX_VALUE ) {
			IJ.error("This stack is too large for this plugin (must have less than "+Integer.MAX_VALUE+" points.");
			return;
		}

		String [] materialList = null;

		AmiraParameters parameters = null;
		if( AmiraParameters.isAmiraLabelfield(imagePlus) ) {
			parameters = new AmiraParameters(imagePlus);
			materialList = parameters.getMaterialList();
		}

		ArrayList<Region> results = new ArrayList<Region>();
	
		ImageStack stack = imagePlus.getStack();
		
		byte[][] sliceData = new byte[depth][];
		for (int z = 0; z < depth; ++z) {
			ByteProcessor bp = (ByteProcessor) stack.getProcessor(z+1);
			sliceData[z] = (byte[]) bp.getPixelsCopy();
		}

		// Preserve the calibration and colour lookup tables
		// for generating new images of each individual
		// region.

		Calibration calibration = imagePlus.getCalibration();
		
		ColorModel cm = null;
		if( ImagePlus.COLOR_256 == type ) {
			cm = stack.getColorModel();
		}
			
		while (true) {
			
			// Find one pixel that's non-zero:
			int initial_x = -1;
			int initial_y = -1;
			int initial_z = -1;
			int foundValue = 0;
			
			for (int z = 0; z < depth && foundValue == 0; ++z) {
				for (int y = 0; y < height && foundValue == 0; ++y) {
					for (int x = 0; x < width; ++x) {
						int value = sliceData[z][y * width + x] & 0xFF;
						if (value != 0) {
							initial_x = x;
							initial_y = y;
							initial_z = z;
							foundValue = value;
							break;
						}
					}
				}
			}
			
			if (foundValue == 0)
				break;
			
			int v = foundValue;
			String materialName = null;
			if( materialList != null )
				materialName = materialList[v];
			
			int pointsInQueue = 0;
			int queueArrayLength = 1024;			
			int queue[] = new int[queueArrayLength];
						
			byte [] pointState = new byte[depth*width * height];
			int i = width * (initial_z * height + initial_y) + initial_x;
			pointState[i] = IN_QUEUE;
			queue[pointsInQueue++] = i;
			
			int pointsInThisRegion = 0;
			
			while(pointsInQueue > 0)  {
				
				int nextIndex=queue[--pointsInQueue];

				int currentPointStateIndex = nextIndex;
				int pz = nextIndex / (width * height);
				int currentSliceIndex = nextIndex % (width * height);
				int py = currentSliceIndex / width;
				int px = currentSliceIndex % width;

				pointState[currentPointStateIndex] = ADDED;
				sliceData[pz][currentSliceIndex] = 0;
				++ pointsInThisRegion;
				
				int x_unchecked_min = px - 1;
				int y_unchecked_min = py - 1;
				int z_unchecked_min = pz - 1;
				
				int x_unchecked_max = px + 1;
				int y_unchecked_max = py + 1;
				int z_unchecked_max = pz + 1;
				
				int x_min = (x_unchecked_min < 0) ? 0 : x_unchecked_min;
				int y_min = (y_unchecked_min < 0) ? 0 : y_unchecked_min;
				int z_min = (z_unchecked_min < 0) ? 0 : z_unchecked_min;
				
				int x_max = (x_unchecked_max >= width) ? width - 1 : x_unchecked_max;			    
				int y_max = (y_unchecked_max >= height) ? height - 1 : y_unchecked_max;
				int z_max = (z_unchecked_max >= depth) ? depth - 1 : z_unchecked_max;

				for( int z = z_min; z <= z_max; ++z ) {
					for( int y = y_min; y <= y_max; ++y ) {
						for( int x = x_min; x <= x_max; ++x ) {
							
							// If we're not including diagonals,
							// skip those points.
							
							if( (! diagonal) &&
							    (x == x_unchecked_min || x == x_unchecked_max) &&
							    (y == y_unchecked_min || y == y_unchecked_max) &&
							    (z == z_unchecked_min || z == z_unchecked_max) )
								continue;
							
							int newSliceIndex = y * width + x;
							int newPointStateIndex =  width * (z * height + y) + x;
							
							int neighbourValue = sliceData[z][newSliceIndex] & 0xFF;
							if( neighbourValue != v)
								continue;
							
							if( 0 == pointState[newPointStateIndex] ) {
								pointState[newPointStateIndex] = IN_QUEUE;
								if( pointsInQueue == queueArrayLength ) {
									int newArrayLength = (int)(queueArrayLength * 1.2);
									int [] newArray = new int[newArrayLength];
									System.arraycopy(queue,0,newArray,0,pointsInQueue);
									queue = newArray;
									queueArrayLength = newArrayLength;
								}
								queue[pointsInQueue++] = newPointStateIndex;
							}
						}
					}
				}
			}			
			
			// So now pointState should have no IN_QUEUE
			// status points...

			Region region = new Region(v,materialName,pointsInThisRegion);

			results.add( region );

			byte replacementValue = (byte)( (cm == null) ? 255 : v );

			if( display ) {
				
				ImageStack newStack = new ImageStack(width,height);
				for( int z = 0; z < depth; ++z ) {
					byte [] sliceBytes = new byte[width*height];
					for( int y = 0; y < height; ++y )
						for( int x = 0; x < width; ++x ) {
							
							byte status = pointState[width*(z*height+y)+x];
							
							if( status == IN_QUEUE )  {
								System.out.println("BUG: point "+x+","+y+","+z+" is still marked as IN_QUEUE");
							}
							
							if( status == ADDED ) {
								sliceBytes[y*width+x] = replacementValue;
							}
						}
					
					ByteProcessor bp = new ByteProcessor(width,height);
					bp.setPixels(sliceBytes);
					newStack.addSlice("", bp);
				}
				
				if( ImagePlus.COLOR_256 == type ) {
					if( cm != null ) {
						newStack.setColorModel( cm );
					}
				}

				ImagePlus newImagePlus = new ImagePlus( region.toString(), newStack );
				
				if( calibration != null ) {
					imagePlus.setCalibration(calibration);
				}

				if( parameters != null )
					parameters.setParameters(imagePlus,true);
				
				newImagePlus.show();
			}
		}

		Collections.sort(results,Collections.reverseOrder());
		
		for( Iterator<Region> it = results.iterator(); it.hasNext(); )
			System.out.println(it.next().toString());				

	}
}
