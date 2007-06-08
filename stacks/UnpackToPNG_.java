/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package stacks;

import util.BatchOpener;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.LookUpTable;
import ij.plugin.PlugIn;

import java.io.File;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.text.DecimalFormat;
import javax.imageio.ImageIO;

public class UnpackToPNG_ implements PlugIn {

	public UnpackToPNG_( ) {
		
	}
	
	public void run( String pluginArguments ) {

		System.out.println("in macro!");
		
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
		

		String filename = Macro.getValue(
			macroArguments,
			"inputFilename",
			"");
		
		if( filename.equals("") ) {
			IJ.error("No macro parameter inputFilename supplied");
			return;
		}
		
		String destinationDirectory = Macro.getValue(
			macroArguments,
			"destinationDirectory",
			"");
		
		if( destinationDirectory.equals("") ) {
			IJ.error("No macro parameter destinationDirectory supplied");
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
				DecimalFormat f5 = new DecimalFormat("00");
				
				String outputFileName = f2.format(i) + "-" +
					f5.format("00000")+".png";
				
				outputFileName = destinationDirectory +
					File.pathSeparator + outputFileName;
				
				imp.setSlice(z+1);

				try {
					writeImage( imp, outputFileName, -1 );
				} catch( Exception e ) {
					System.err.println("Caught an exception: "+e);
				}
				
			}
			
		}
		
	}

	void writeImage(ImagePlus imp, String path, int transparentColorIndex ) throws Exception {
		int width = imp.getWidth();
		int height = imp.getHeight();
		BufferedImage bi = null;
		if( imp.getType() == ImagePlus.GRAY8 ) {
			bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		} else if( imp.getType() == ImagePlus.COLOR_256 ) {
			LookUpTable lut = imp.createLut();
			if( lut == null ) {
				IJ.error("createLut() returned null for a COLOR_256 image");
				return;
			} 
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
			bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		}
		Graphics2D g = (Graphics2D)bi.getGraphics();
		g.drawImage(imp.getImage(), 0, 0, null);
		File f = new File(path);
		ImageIO.write(bi, "png", f);
		
	}
	
}
