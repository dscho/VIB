/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package stacks;

import util.BatchOpener;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.Macro;
import ij.LookUpTable;
import ij.plugin.PlugIn;

import java.io.File;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.Image;
import java.text.DecimalFormat;
import javax.imageio.ImageIO;

public class UnpackToPNG_ implements PlugIn {

	public UnpackToPNG_( ) {
		
	}
	
	public void run( String pluginArguments ) {

		String realArguments = null;

		String macroArguments = Macro.getOptions();
				
		if( (macroArguments == null) || (macroArguments.equals("")) ) {

			if( (pluginArguments == null) || (pluginArguments.equals("")) ) {
				IJ.error("No parameters supplied either as macro options or a plugin argument.");
				return;
			} else {
				realArguments = pluginArguments;
			}

		} else { 
			realArguments = macroArguments;
		}
		
		System.out.println("in macro, with realArguments: "+realArguments);

		String filename = Macro.getValue(
			realArguments,
			"filename",
			"");
		
		if( filename.equals("") ) {
			IJ.error("No macro parameter filename supplied");
			return;
		}
		
		String destinationDirectory = Macro.getValue(
			macroArguments,
			"directory",
			"");
		
		if( destinationDirectory.equals("") ) {
			IJ.error("No macro parameter directory supplied");
			return;
		}	

		System.out.println("Got input filename: '"+filename+"'");
		System.out.println("Got destination directory: '"+destinationDirectory+"'");
		
		ImagePlus [] imps = BatchOpener.openFromFile(
			filename );

		System.out.println("Got "+imps.length+" channels");
		
		for( int i = 0; i < imps.length; ++i ) {
			
			ImagePlus imp = imps[0];

			System.out.println("ImagePlus "+i+" is: "+imp);
			
			int stackDepth = imp.getStackSize();
			
			for( int z = 0; z < stackDepth; ++z ) {

				DecimalFormat f2 = new DecimalFormat("00");
				DecimalFormat f5 = new DecimalFormat("00000");
				
				String outputFileName = f2.format(i) + "-" +
					f5.format(z)+".png";
				
				outputFileName = destinationDirectory +
					File.separator + outputFileName;
				
				try {
					System.out.println("Writing image to: "+outputFileName);
					writeImage( imp, z, outputFileName, -1 );
				} catch( Exception e ) {
					System.err.println("Caught an exception: "+e);
				}
				
			}
			
		}
		
	}

	/* This is basically an enhanced version of the method in PNG_Writer. */

	void writeImage(ImagePlus imp, int slice, String path, int transparentColorIndex ) throws Exception {
		int width = imp.getWidth();
		int height = imp.getHeight();
		BufferedImage bi = null;
		if( (imp.getType() == ImagePlus.GRAY8) || (imp.getType() == ImagePlus.COLOR_256)  ) {
			LookUpTable lut = imp.createLut();
			IndexColorModel cm = null;
			if( (lut != null) && (lut.getMapSize() > 0) ) {
				System.out.println("Writing COLOR_256 PNG.");
				int size = lut.getMapSize();
				byte [] reds = lut.getReds();
				byte [] greens = lut.getGreens();
				byte [] blues = lut.getBlues();
				IndexColorModel cm;
				if( transparentColorIndex < 0 )
					cm = new IndexColorModel(8,size,reds,greens,blues);
				else
					cm = new IndexColorModel(8,size,reds,greens,blues,transparentColorIndex);
				bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm);
			} else {
				if( (lut == null) && (imp.getType() == ImagePlus.COLOR_256) ) {
					IJ.error("createLut() returned null for a COLOR_256 image");
					return;
				} 
				System.out.println("Writing TYPE_BYTE_GRAY PNG.");
				bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			}
		} else if( imp.getType() == ImagePlus.COLOR_256 ) {
			LookUpTable lut = imp.createLut();
		} else {
			System.out.println("Writing a full RGB color PNG.");
			bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		}
		Graphics2D g = (Graphics2D)bi.getGraphics();
		ImageStack stack = imp.getStack();
		ImageProcessor imageProcessor = stack.getProcessor(slice+1);
		Image imageToDraw = imageProcessor.createImage();
		g.drawImage(imageToDraw, 0, 0, null);
		File f = new File(path);
		ImageIO.write(bi, "png", f);
	}
}
