/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
  This file is part of the ImageJ plugin "Show_Colour_Surfaces".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package isosurface;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.text.*;
import ij.measure.Calibration;
import ij.io.*;

import ij3d.Image3DUniverse;
import ij3d.Content;
import javax.vecmath.Color3f;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;

import java.util.HashMap;

/* 
  This plugin should be used with 8-bit indexed colour images where
  each colour represents a different material.  Each of this materials
  will be displayed as a surface in the 3D viewer.
 */

public class Show_Colour_Surfaces implements PlugIn {

	/* If backgroundColorIndex is -1, then ask for the background colour. */
	public void displayAsSurfaces( Image3DUniverse univ, ImagePlus image, int backgroundColorIndex ) {
		if( image == null ) {
			IJ.error( "Show_Colour_Surfaces.displayAsSurfaces was passed a null 'image'" );
			return;
		}
		if( univ == null ) {
			IJ.error( "Show_Colour_Surfaces.displayAsSurfaces was passed a null 'univ'" );
			return;
		}
		int type = image.getType();
		if( type != ImagePlus.COLOR_256 ) {
			IJ.error( "Show_Colour_Surfaces only works with 8-bit indexed color images." );
			return;
		}
		ImageStack stack = image.getStack();
		IndexColorModel cm = (IndexColorModel)stack.getColorModel();
		if( cm == null ) {
			IJ.error( "The color model for this image stack was null" );
			return;
		}
		int colours = cm.getMapSize();
		byte [] reds = new byte[colours];
		byte [] greens = new byte[colours];
		byte [] blues = new byte[colours];
		cm.getReds( reds );
		cm.getBlues( blues );
		cm.getGreens( greens );
		if( backgroundColorIndex < 0 ) {
			GenericDialog gd = new GenericDialog("Show Colour Surfaces");
			gd.addNumericField( "Index of background colour (from 0 to "+
					    (colours-1)+" inclusive):", 0, 3 );
			gd.showDialog();
			if(gd.wasCanceled())
				return;
			backgroundColorIndex = (int)gd.getNextNumber();
		}
		if( backgroundColorIndex < 0 || backgroundColorIndex >= colours ) {
			IJ.error("The background colour must have an index from 0 to "+(colours-1)+" inclusive");
			return;
		}
		int width = image.getWidth();
		int height = image.getHeight();
		int depth = image.getStackSize();
		Calibration calibration = image.getCalibration();
		double maxSide = Math.max( Math.abs( width * calibration.pixelWidth ), 
					   Math.max( Math.abs( height * calibration.pixelHeight ),
						     Math.abs( depth * calibration.pixelDepth ) ) );
		int resamplingFactor = 1;
		while( (maxSide / resamplingFactor) > 512 ) {
			resamplingFactor *= 2;
		}
		System.out.println("Got resampling factor: "+2);
		HashMap<Integer,Boolean> coloursUsedInImage = new HashMap();
		for( int c = 0; c < colours; ++c ) {
			coloursUsedInImage.put( c, false );
		}
		for( int z = 0; z < depth; ++z ) {
			byte [] pixels = (byte[])stack.getPixels(z+1);
			for( int i = 0; i < pixels.length; ++i ) {
				int v = pixels[i] & 0xFF;
				coloursUsedInImage.put( v, true );
			}
		}
		for( int i = 0; i < colours; ++i ) {
			boolean used = coloursUsedInImage.get( i );
			if( ! used ) {
				System.out.println("Skipping colour index "+i+", since it's not used in the image");
				continue;
			}
			if( i == backgroundColorIndex )
				continue;
			Color3f c = new Color3f( (reds[i] & 0xFF) / 255.0f,
						 (greens[i] & 0xFF) / 255.0f,
						 (blues[i] & 0xFF) / 255.0f );
			byte v = (byte)i;
			// Make a new ImagePlus with just this colour:
			ImageStack newStack = new ImageStack( width, height );
			for( int z = 0; z < depth; ++z ) {
				byte [] originalPixels = (byte[])stack.getPixels(z+1);
				byte [] newBytes = new byte[originalPixels.length];
				for( int j = 0; j < originalPixels.length; ++j ) {
					if( originalPixels[j] == v )
						newBytes[j] = (byte)255;
				}
				ByteProcessor bp = new ByteProcessor(width,height);
				bp.setPixels(newBytes);
				newStack.addSlice("",bp);
			}
			ImagePlus colourImage = new ImagePlus("Image for colour index: "+i,newStack);
			// FIXME: It might be worth smoothing here to
			// improve the look of the surfaces:
			boolean [] channels = { true, true, true };
			Content content = univ.addContent(colourImage,
							  c,
							  colourImage.getTitle(),
							  10, // threshold
							  channels,
							  resamplingFactor,
							  Content.SURFACE);
			content.setLocked(true);
			// c.setTransparency(0.5f);
			colourImage.close();
		}
	}

	public void run( String ignored ) {
		ImagePlus image = IJ.getImage();
		if( image == null ) {
			IJ.error("There is no image to view.");
			return;
		}
		Image3DUniverse univ = new Image3DUniverse(512, 512);
		univ.show();
		GUI.center(univ.getWindow());
		displayAsSurfaces( univ, image, 0 );
	}

}
