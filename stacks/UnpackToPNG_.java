/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package stacks;

import util.BatchOpener;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.Macro;
import ij.LookUpTable;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.Image;
import java.text.DecimalFormat;
import javax.imageio.ImageIO;

import java.util.HashSet;
import java.util.Iterator;

import amira.AmiraParameters;

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
		
		ImagePlus [] imps = BatchOpener.openFromFile(
			filename );

		if( imps == null ) {
			IJ.error("Couldn't open the file: "+filename);
			return;
		}

		if( AmiraParameters.isAmiraLabelfield(imps[0]) ) {
			try {
				unpackAmiraLabelFieldToPNGs(imps[0],destinationDirectory);
			} catch( IOException e ) {
				IJ.error( "There was an IOException while unpacking the label file: "+e);
			}
			return;
		}

		for( int i = 0; i < imps.length; ++i ) {

			if( i == 0 )
				continue;

			ImagePlus imp = imps[i];

			int stackDepth = imp.getStackSize();
			
			for( int z = 0; z < stackDepth; ++z ) {

				DecimalFormat f2 = new DecimalFormat("00");
				DecimalFormat f5 = new DecimalFormat("00000");
				
				String outputFileName = f2.format(i) + "-" +
					f5.format(z)+".png";
				
				outputFileName = destinationDirectory +
					File.separator + outputFileName;
				
				try {
					writeImage( imp, z, outputFileName, -1 );
				} catch( Exception e ) {
					System.err.println("Caught an exception: "+e);
				}
				
			}
			
		}
		
	}

	void unpackAmiraLabelFieldToPNGs(ImagePlus labelFileImp,
					 String destinationDirectory) throws IOException {

		if( (labelFileImp.getType() != ImagePlus.GRAY8) &&
		    (labelFileImp.getType() != ImagePlus.COLOR_256) ) {
			IJ.error("The label file appeared not to be 8 bit (!)");
			return;
		}

		AmiraParameters ap = new AmiraParameters(labelFileImp);
	       
		int materialCount = ap.getMaterialCount();

		String [] materialNames = ap.getMaterialList();
		
		int stackDepth = labelFileImp.getStackSize();

		int width=labelFileImp.getWidth();
		int height=labelFileImp.getHeight();

		// Write a material index:

		String jsonIndexFileName = destinationDirectory + File.separator + "material-index.json";

		PrintStream ps = new PrintStream(jsonIndexFileName);

		ps.println("[");

		byte [] reds =   new byte[materialCount];
		byte [] greens = new byte[materialCount];
		byte [] blues =  new byte[materialCount];
		for( int i = 0; i < materialCount; ++i ) {
			double [] color = ap.getMaterialColor(i);
			reds[i] = (byte)(color[0] * 255);
		        greens[i] = (byte)(color[1] * 255);
			blues[i] = (byte)(color[2] * 255);
			
			ps.print("  [ \"" + ap.getMaterialName(i) + "\", [ " +
				   (reds[i]&0xFF) + ", " + (greens[i]&0xFF) + ", " +
				   (blues[i]&0xFF) + " ] ]" );
			
			if( i != (materialCount - 1) )
				ps.println(",");
			else
				ps.println("");
		}

		ps.println("]");
		ps.close();

		// Write the dimensions too...

		String dimensionsFileName = destinationDirectory + File.separator + "dimensions.json";

		ps = new PrintStream(dimensionsFileName);
		ps.println("[ "+width+", "+height+", "+stackDepth+" ]");
		ps.close();

		long [] pixelCountsForMaterial = new long[materialCount];
		float [] xSumsForMaterial = new float[materialCount];
		float [] ySumsForMaterial = new float[materialCount];
		float [] zSumsForMaterial = new float[materialCount];

		IndexColorModel cm = new IndexColorModel(8,materialCount,reds,greens,blues,0 /* the transparent color */ );
			
		for( int z = 0; z < stackDepth; ++z ) {

			DecimalFormat f2 = new DecimalFormat("00");
			DecimalFormat f5 = new DecimalFormat("00000");
			
			String outputFileNameStem = f2.format(0) + "-" +
				f5.format(z)+".png";
			
			outputFileNameStem = destinationDirectory +
				File.separator + outputFileNameStem;
			
			ImageStack stack = labelFileImp.getStack();
			ImageProcessor imageProcessor = stack.getProcessor(z+1);
			
			byte [] pixels = (byte [])imageProcessor.getPixelsCopy();

			/* Actually we don't really need to create
			   this Hashset, but never mind... */

			HashSet materialsInThisSlice = new HashSet();

			for(int i = 0; i<pixels.length; ++i ) {

				int intValue = pixels[i]&0xFF;
				Integer value = new Integer(intValue);
				materialsInThisSlice.add(value);
				
				++ pixelCountsForMaterial[intValue];
				int x = (i % width);
				int y = (i / width);
				xSumsForMaterial[intValue] += x;
				ySumsForMaterial[intValue] += y;
				zSumsForMaterial[intValue] += z;

			}

			// First just write all the labels out in one PNG:

			{
				BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm);
			
				String outputFileName = outputFileNameStem + "-all.png";

				Graphics2D g = (Graphics2D)bi.getGraphics();
				Image imageToDraw = imageProcessor.createImage();
				g.drawImage(imageToDraw, 0, 0, null);
				File f = new File(outputFileName);
				ImageIO.write(bi, "png", f);
			}

			byte [] emptySliceData = new byte[pixels.length];
			for( int i = 0; i < pixels.length; ++i )
				emptySliceData[i] = 0;
			
			ByteProcessor emptyBP = new ByteProcessor( width, height );
			emptyBP.setColorModel(cm);
			emptyBP.setPixels( emptySliceData );
			
			for(int material=1; material<materialCount; ++material ) {
				
				DecimalFormat dfm = new DecimalFormat("000");
				String outputFileName=outputFileNameStem+"-"+
					dfm.format(material)+"-"+
					ap.getMaterialName(material)+".png";

				BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm);
				Graphics2D g = (Graphics2D)bi.getGraphics();				
						
				if( materialsInThisSlice.contains(new Integer(material)) ) {

					byte [] sliceData = new byte[pixels.length];
					for( int i = 0; i < pixels.length; ++i ) {
						if( (pixels[i]&0xFF) == material )
							sliceData[i] = (byte)material;
						else
							sliceData[i] = 0;
					}

					ByteProcessor singleMaterialBP = new ByteProcessor(width,height);
					singleMaterialBP.setColorModel(cm);
					singleMaterialBP.setPixels(sliceData);
					Image imageToDraw = singleMaterialBP.createImage();
					g.drawImage(imageToDraw, 0, 0, null);
					File f = new File(outputFileName);
					ImageIO.write(bi, "png", f);

				} else {
					
					Image imageToDraw = emptyBP.createImage();
					g.drawImage(imageToDraw, 0, 0, null);
					File f = new File(outputFileName);
					ImageIO.write(bi, "png", f);					
				}
			}
		}
		

		String centresFileName = destinationDirectory + File.separator + "centres.json";

		ps = new PrintStream(centresFileName);

		ps.println( "[" );

		for( int i = 0; i < materialCount; ++i ) {

			xSumsForMaterial[i] /= pixelCountsForMaterial[i];
			ySumsForMaterial[i] /= pixelCountsForMaterial[i];
			zSumsForMaterial[i] /= pixelCountsForMaterial[i];
			
			ps.print("[ "+((int)xSumsForMaterial[i])+
				 ", "+((int)ySumsForMaterial[i])+
				 ", "+((int)zSumsForMaterial[i])+" ]");
			
			if( i != (materialCount - 1) )
				ps.println(",");
			else
				ps.println("");
		}

		ps.println( "]" );
		ps.close();


	}

	/* This is basically an enhanced version of the method in PNG_Writer. */

	void writeImage(ImagePlus imp, int slice, String path, int transparentColorIndex ) throws Exception {
		int width = imp.getWidth();
		int height = imp.getHeight();
		BufferedImage bi = null;
		if( (imp.getType() == ImagePlus.GRAY8) || (imp.getType() == ImagePlus.COLOR_256)  ) {
			LookUpTable lut = imp.createLut();
			if( (lut != null) && (lut.getMapSize() > 0) ) {
				int size = lut.getMapSize();
				byte [] reds = lut.getReds();
				byte [] greens = lut.getGreens();
				byte [] blues = lut.getBlues();
				IndexColorModel cm = null;
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
				bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			}
		} else if( imp.getType() == ImagePlus.COLOR_256 ) {
			LookUpTable lut = imp.createLut();
		} else {
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
