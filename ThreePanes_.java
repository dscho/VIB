/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

import ij.*;
import ij.gui.ImageCanvas;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;

public class ThreePanes_ implements PlugIn {

	ThreePanes threePanes;

	public void run( String argument ) {

		ImagePlus currentImage = WindowManager.getCurrentImage();

		if( currentImage == null ) {
			IJ.error( "There's no current image to crop." );
			return;
		}

		try {
			threePanes = new ThreePanes(
				Class.forName("ThreePanesCanvas"),
				currentImage );
		} catch( ClassNotFoundException e ) {
			IJ.error("Couldn't find the class ThreePanesCanvas");
		}
	}

}
