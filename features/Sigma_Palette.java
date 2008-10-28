/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package features;

import ij.ImageJ;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;
import ij.process.ByteProcessor;
import ij.plugin.PlugIn;
import ij.gui.Roi;
import ij.gui.ImageCanvas;

import stacks.ThreePaneCrop;
import ij.plugin.filter.Duplicater;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Image;

public class Sigma_Palette implements PlugIn {

	public class PaletteCanvas extends ImageCanvas {

		Sigma_Palette owner;

		int croppedWidth, croppedHeight, sigmasAcross, sigmasDown;

		protected PaletteCanvas( ImagePlus imagePlus, Sigma_Palette owner, int croppedWidth, int croppedHeight, int sigmasAcross, int sigmasDown ) {
			super(imagePlus);
			this.owner = owner;
			this.croppedWidth = croppedWidth;
			this.croppedHeight = croppedHeight;
			this.sigmasAcross = sigmasAcross;
			this.sigmasDown = sigmasDown;
		}

		/* Keep another Graphics for double-buffering: */

		private int backBufferWidth;
		private int backBufferHeight;

		private Graphics backBufferGraphics;
		private Image backBufferImage;

		private void resetBackBuffer() {

			if(backBufferGraphics!=null){
				backBufferGraphics.dispose();
				backBufferGraphics=null;
			}

			if(backBufferImage!=null){
				backBufferImage.flush();
				backBufferImage=null;
			}
		
			backBufferWidth=getSize().width;
			backBufferHeight=getSize().height;

			backBufferImage=createImage(backBufferWidth,backBufferHeight);
			backBufferGraphics=backBufferImage.getGraphics();
		}

		public void paint(Graphics g) {
		
			if(backBufferWidth!=getSize().width ||
			   backBufferHeight!=getSize().height ||
			   backBufferImage==null ||
			   backBufferGraphics==null)
				resetBackBuffer();
		
			super.paint(backBufferGraphics);
			drawOverlay(backBufferGraphics);
			g.drawImage(backBufferImage,0,0,this);
		}
	
		protected void drawOverlay( Graphics g ) {
		
			g.setColor( java.awt.Color.MAGENTA );

			int width = imp.getWidth();
			int height = imp.getHeight();

			// Draw the vertical lines:
			for( int i = 1; i < sigmasAcross; ++i ) {
				int x = i * croppedWidth + 1;
				int screen_x = screenX(x);
				g.drawLine( screen_x, screenY(0), screen_x, screenY(height-1) );
			}

			// Draw the horizontal lines:
			for( int j = 1; j < sigmasDown; ++j ) {
				int y = j * croppedHeight + 1;
				int screen_y = screenY(y);
				g.drawLine( screenX(0), screen_y, screenX(width-1), screen_y );
			}
		}
	}

	public float makePalette( ImagePlus original,
				  int x_min,
				  int x_max,
				  int y_min,
				  int y_max,
				  int z_min,
				  int z_max,
				  HessianEvalueProcessor hep,
				  float [] sigmaValues,
				  int sigmasAcross,
				  int sigmasDown ) {

		int originalWidth = original.getWidth();
		int originalHeight = original.getHeight();
		int originalDepth = original.getStackSize();

		ImagePlus cropped = ThreePaneCrop.performCrop( original, x_min, x_max, y_min, y_max, z_min, z_max, false );

		int croppedWidth  = (x_max - x_min) + 1;
		int croppedHeight = (y_max - y_min) + 1;
		int croppedDepth  = (z_max - z_min) + 1;

		Duplicater duplicater = new Duplicater();

		if( sigmaValues.length > sigmasAcross * sigmasDown ) {
			IJ.error( "A "+sigmasAcross+"x"+sigmasDown+" layout is not large enough for "+sigmaValues+" + 1 images" );
			return -1;
		}

		int paletteWidth = croppedWidth * sigmasAcross + (sigmasAcross - 1);
		int paletteHeight = croppedHeight * sigmasDown + (sigmasDown - 1);

		ImageStack newStack = new ImageStack( paletteWidth, paletteHeight );
		for( int z = 0; z < croppedDepth; ++z ) {
			ByteProcessor bp = new ByteProcessor( paletteWidth, paletteHeight );
			newStack.addSlice("",bp);
		}
		ImagePlus paletteImage = new ImagePlus("palette",newStack);
		new PaletteCanvas( paletteImage, this, croppedWidth, croppedHeight, sigmasAcross, sigmasDown );

		paletteImage.show();

		return -1;
	}

	public void run( String ignoredArguments ) {

		ImagePlus image = IJ.getImage();
		if( image == null ) {
			IJ.error("There is no current image");
			return;
		}

		Roi roi = image.getRoi();
		if( roi == null ) {
			IJ.error("There is no current point selection");
			return;
		}

		if( roi.getType() != Roi.POINT ) {
			IJ.error("You must have a point selection");
			return;
		}

		Polygon p = roi.getPolygon();

		if(p.npoints != 1) {
			IJ.error("You must have exactly one point selected");
			return;
		}

		ImageProcessor processor = image.getProcessor();
		/*
		  Calibration cal = imp.getCalibration();
		  ip.setCalibrationTable(cal.getCTable());
		*/

		int x = p.xpoints[0];
		int y = p.ypoints[0];
		int z = image.getCurrentSlice() - 1;

		int either_side = 10;

		int x_min = x - either_side;
		int x_max = x + either_side;
		int y_min = y - either_side;
		int y_max = y + either_side;
		int z_min = z - either_side;
		int z_max = z + either_side;

		int originalWidth = image.getWidth();
		int originalHeight = image.getHeight();
		int originalDepth = image.getStackSize();

		if( x_min < 0 )
			x_min = 0;
		if( y_min < 0 )
			y_min = 0;
		if( z_min < 0 )
			z_min = 0;
		if( x_max >= originalWidth )
			x_max = originalWidth - 1;
		if( y_max >= originalHeight )
			y_max = originalHeight - 1;
		if( z_max >= originalDepth )
			z_max = originalDepth - 1;

		float [] sigmas = { 2.0f, 3.0f };

		makePalette( image, x_max, x_max, y_min, y_min, z_min, z_max, new TubenessProcessor(2,true), sigmas, 3, 4 );


	}
}