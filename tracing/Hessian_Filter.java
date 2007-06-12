/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import ij.*;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;
import ij.measure.Calibration;
import ij.gui.GenericDialog;

public class Hessian_Filter implements PlugIn {
	
	public void run(String argument) {
		
		ImagePlus currentImage = WindowManager.getCurrentImage();

		if( currentImage == null ) {
			IJ.error( "There's no current image to crop." );
			return;
		}

		if( currentImage.getStackSize() <= 1 ) {
			IJ.error( "This plugin is only for image stacks of more than one slice." );
			return;
		}

		if( currentImage.getType() != ImagePlus.GRAY8 ) {
			IJ.error("This plugin only works on 8 bit images at the moment.");
			return;
		}

		int width = currentImage.getWidth();
		int height = currentImage.getHeight();
		int depth = currentImage.getStackSize();

		GenericDialog gd = new GenericDialog("Hessian Analysis of: "+currentImage.getTitle());

		gd.addNumericField("Standard deviation of Gaussian: ",
				   1.2,
				   5);

		gd.addNumericField("Radius of kernel: ",
				   1,
				   3);
		
		gd.addCheckbox("Absolute values in image? ", true);

		System.out.println("Going to show the dialog...");

		gd.showDialog();

		if( gd.wasCanceled() )
			return;

		float gaussianSD = (float)gd.getNextNumber();
		if( gaussianSD < 0 ) {
			IJ.error("The standard deviation of the Gaussian must be positive");
			return;
		}
		
		double eitherSideDouble = gd.getNextNumber();

		if( (eitherSideDouble != Math.floor(eitherSideDouble)) || eitherSideDouble < 1 ) {
			IJ.error("The radius must be a positive integer");
			return;
		}

		int eitherSide = (int)eitherSideDouble;

		boolean useAbsoluteValues = gd.getNextBoolean();
		
		double x_spacing = 1;
		double y_spacing = 1;
		double z_spacing = 1;

		Calibration c = currentImage.getCalibration();
		if( c != null ) {
			x_spacing = c.pixelWidth;
			y_spacing = c.pixelHeight;
			z_spacing = c.pixelDepth;
		}

		byte [][] slice_data = new byte[depth][width*height];
		ImageStack stack = currentImage.getStack();

		for(int z = 0; z < depth; ++z ) {
			slice_data[z] = (byte[])stack.getPixels(z+1);
		}

		HessianAnalyzer analyzer = new HessianAnalyzer(
			slice_data,
			width,
			height,
			depth,
			x_spacing,
			y_spacing,
			z_spacing );
		
		ImageStack smallestStack = new ImageStack(width,height);
		ImageStack middleStack = new ImageStack(width,height);
		ImageStack largestStack = new ImageStack(width,height);

		int x, y, z;

		try {
		
			// for( z = 0; z < depth; ++z ) {
			for( z = 43; z < depth; ++z ) {
				
				if( z == 47 )
					break;
			
				System.out.println("Doing slice index "+z+"; depth: "+depth);
				
				float [] smallestSlice = new float[width * height];
				float [] middleSlice = new float[width * height];
				float [] largestSlice = new float[width * height];
				
				for( y = 0; y < height; ++y ) {
					
					for( x = 0; x < width; ++x ) {
						
						EigenResultsDouble results;
						results = 
							analyzer.analyzeAtPoint(
								x, y, z,
								eitherSide,
								gaussianSD,
								false );
						
						double smallest = results.sortedValues[0];
						double middle = results.sortedValues[1];
						double largest = results.sortedValues[2];
						
						if( useAbsoluteValues ) {
							smallestSlice[x+width*y] = (float)Math.abs(smallest);
							middleSlice[x+width*y] = (float)Math.abs(middle);
							largestSlice[x+width*y] = (float)Math.abs(largest);
						} else {
							smallestSlice[x+width*y] = (float)smallest;
							middleSlice[x+width*y] = (float)middle;
							largestSlice[x+width*y] = (float)largest;
						}
						
					}
				}
				
				FloatProcessor smallestProcessor = new FloatProcessor(width,height);
				FloatProcessor middleProcessor = new FloatProcessor(width,height);
				FloatProcessor largestProcessor = new FloatProcessor(width,height);
				
				smallestProcessor.setPixels(smallestSlice);
				middleProcessor.setPixels(middleSlice);
				largestProcessor.setPixels(largestSlice);
				
				smallestStack.addSlice( null, smallestProcessor );
				middleStack.addSlice( null, middleProcessor );
				largestStack.addSlice( null, largestProcessor );
				
			}
			
			String titleSuffix = (useAbsoluteValues ? "(abs)" : "(non-abs)") +
				" sd"+gaussianSD+"r"+eitherSide;
			
			ImagePlus smallestImage = new ImagePlus( "smallest"+titleSuffix, smallestStack );
			ImagePlus middleImage = new ImagePlus( "middle"+titleSuffix, middleStack );
			ImagePlus largestImage = new ImagePlus( "largest"+titleSuffix, largestStack );
			
			smallestImage.show();
			middleImage.show();
			largestImage.show();
			
		} catch( Exception exception ) {
			IJ.error("Caught an exception while calculating the Hessian: "+exception);
			return;
		}
	}
}
