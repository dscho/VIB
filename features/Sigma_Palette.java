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
import ij.gui.StackWindow;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;

import stacks.ThreePaneCrop;
import ij.plugin.filter.Duplicater;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Image;
import java.awt.Scrollbar;
import java.awt.Label;
import java.awt.event.*;

import util.Limits;

public class Sigma_Palette extends Thread implements PlugIn {

	public static interface SigmaPaletteListener {
		public void newSigmaSelected( double sigma );
		public void newMaximum( double max );
		public void sigmaPaletteClosing( );
	}

	public static class PaletteStackWindow extends StackWindow {

		Sigma_Palette owner;
		Label label;
		Scrollbar maxValueScrollbar;

		private void addExtraScrollbar( double defaultMaxValue ) {
			label = new Label("");
			add(label);
			updateLabel( defaultMaxValue );
			maxValueScrollbar = new Scrollbar( Scrollbar.HORIZONTAL, (int)defaultMaxValue, 1, 1, 350 );
			maxValueScrollbar.addAdjustmentListener(
				new AdjustmentListener()  {
					public void adjustmentValueChanged(AdjustmentEvent e) {
						int newValue = e.getValue();
						updateLabel( newValue );
						maxChanged( newValue );
					}
				} );
			add(maxValueScrollbar);
			pack();
		}

		private void updateLabel( double maxValue ) {
			int intMaxValue = (int)Math.round(maxValue);
			label.setText("Adjust maximum value: "+intMaxValue);
		}

		private void maxChanged( double newValue ) {
			if( owner != null ) {
				owner.setMax( newValue );
			}
		}

		public void windowClosing(WindowEvent e) {
			if( owner != null && owner.listener != null ) {
				owner.listener.sigmaPaletteClosing();
			}
			super.windowClosing(e);
		}

		public PaletteStackWindow(ImagePlus imp) {
			super(imp);
			addExtraScrollbar(80);
		}

		public PaletteStackWindow(ImagePlus imp, ImageCanvas ic, Sigma_Palette owner, double defaultMax ) {
			super(imp,ic);
			this.owner = owner;
			addExtraScrollbar(defaultMax);
		}
	}

	public static class PaletteCanvas extends ImageCanvas {

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

		int sigmaIndexFromMouseEvent( MouseEvent e ) {
			int sx = e.getX();
			int sy = e.getY();
			int ox = offScreenX(sx);
			int oy = offScreenY(sy);
			int sigmaX = ox / (owner.croppedWidth + 1);
			int sigmaY = oy / (owner.croppedHeight + 1);
			int sigmaIndex = sigmaY * sigmasAcross + sigmaX;
			if( sigmaIndex >= 0 && sigmaIndex < owner.sigmaValues.length )
				return sigmaIndex;
			else
				return -1;
		}

		public void mouseMoved(MouseEvent e) {
			int sigmaIndex = sigmaIndexFromMouseEvent( e );
			if( sigmaIndex >= 0 ) {
				double sigmaValue = owner.sigmaValues[sigmaIndex];
				IJ.showStatus("\u03C3 = "+sigmaValue);
			} else {
				IJ.showStatus("No  \u03C3 (unused entry)");
			}
		}

		public void mouseClicked(MouseEvent e) {
			int oldSelectedSigmaIndex = owner.getSelectedSigmaIndex();
			int sigmaIndex = sigmaIndexFromMouseEvent( e );
			if( sigmaIndex >= 0 ) {
				if( sigmaIndex == oldSelectedSigmaIndex )
					owner.setSelectedSigmaIndex( -1 );
				else
					owner.setSelectedSigmaIndex( sigmaIndex );
			}
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
			for( int i = 0; i <= sigmasAcross; ++i ) {
				int x = i * (croppedWidth + 1);
				int screen_x = screenX(x);
				g.drawLine( screen_x, screenY(0), screen_x, screenY(height-1) );
			}

			// Draw the horizontal lines:
			for( int j = 0; j <= sigmasDown; ++j ) {
				int y = j * (croppedHeight + 1);
				int screen_y = screenY(y);
				g.drawLine( screenX(0), screen_y, screenX(width-1), screen_y );
			}

			// If there's a selected sigma, highlight that in green:
			int selectedSigmaIndex = owner.getSelectedSigmaIndex();

			if( selectedSigmaIndex >= 0 && selectedSigmaIndex < owner.sigmaValues.length ) {
				g.setColor( java.awt.Color.GREEN );
				int sigmaY = selectedSigmaIndex / sigmasAcross;
				int sigmaX = selectedSigmaIndex % sigmasAcross;
				int leftX   = screenX( sigmaX * (croppedWidth + 1) );
				int rightX  = screenX( (sigmaX + 1) * (croppedWidth + 1) );
				int topY    = screenY( sigmaY * (croppedHeight + 1) );
				int bottomY = screenY( (sigmaY + 1) * (croppedHeight + 1) );
				g.drawLine( leftX, topY, rightX, topY );
				g.drawLine( leftX, topY, leftX, bottomY );
				g.drawLine( leftX, bottomY, rightX, bottomY );
				g.drawLine( rightX, bottomY, rightX, topY );
			}
		}
	}

	double [] sigmaValues;

	int croppedWidth;
	int croppedHeight;
	int croppedDepth;

	SigmaPaletteListener listener;

	public void setListener( SigmaPaletteListener newListener ) {
		listener = newListener;
	}

	ImagePlus paletteImage;

	double max;
	public double getMax( ) {
		return max;
	}

	public void setMax( double max ) {
		this.max = max;
		if( paletteImage != null ) {
			paletteImage.getProcessor().setMinAndMax(0,max);
			paletteImage.updateAndDraw();
		}
		if( listener != null )
			listener.newMaximum( max );
	}

	int selectedSigmaIndex = -1;
	public int getSelectedSigmaIndex( ) {
		return selectedSigmaIndex;
	}

	public void setSelectedSigmaIndex( int selectedSigmaIndex ) {
		this.selectedSigmaIndex = selectedSigmaIndex;
		if( listener != null && selectedSigmaIndex >= 0 )
			listener.newSigmaSelected( sigmaValues[selectedSigmaIndex] );
		paletteImage.updateAndDraw();
	}

	int x_min, x_max, y_min, y_max, z_min, z_max;
	HessianEvalueProcessor hep;
	double defaultMax;
	int sigmasAcross;
	int sigmasDown;
	int initial_z;

	public void makePalette( ImagePlus image,
				 int x_min,
				 int x_max,
				 int y_min,
				 int y_max,
				 int z_min,
				 int z_max,
				 HessianEvalueProcessor hep,
				 double [] sigmaValues,
				 double defaultMax,
				 int sigmasAcross,
				 int sigmasDown,
				 int initial_z ) {

		this.image = image;

		this.x_min = x_min;
		this.x_max = x_max;
		this.y_min = y_min;
		this.y_max = y_max;
		this.z_min = z_min;
		this.z_max = z_max;

		this.hep = hep;

		this.sigmaValues = sigmaValues;
		this.defaultMax = defaultMax;

		this.sigmasAcross = sigmasAcross;
		this.sigmasDown = sigmasDown;

		this.initial_z = initial_z;

		int originalWidth = image.getWidth();
		int originalHeight = image.getHeight();
		int originalDepth = image.getStackSize();

		start();

	}

	public void run( ) {

		ImagePlus cropped = ThreePaneCrop.performCrop( image, x_min, x_max, y_min, y_max, z_min, z_max, false );

		croppedWidth  = (x_max - x_min) + 1;
		croppedHeight = (y_max - y_min) + 1;
		croppedDepth  = (z_max - z_min) + 1;

		if( sigmaValues.length > sigmasAcross * sigmasDown ) {
			IJ.error( "A "+sigmasAcross+"x"+sigmasDown+" layout is not large enough for "+sigmaValues+" + 1 images" );
			return;
		}

		int paletteWidth = croppedWidth * sigmasAcross + (sigmasAcross + 1);
		int paletteHeight = croppedHeight * sigmasDown + (sigmasDown + 1);

		ImageStack newStack = new ImageStack( paletteWidth, paletteHeight );
		for( int z = 0; z < croppedDepth; ++z ) {
			FloatProcessor fp = new FloatProcessor( paletteWidth, paletteHeight );
			newStack.addSlice("",fp);
		}
		paletteImage = new ImagePlus("Pick Sigma and Maximum",newStack);
		setMax(defaultMax);

		PaletteCanvas paletteCanvas = new PaletteCanvas( paletteImage, this, croppedWidth, croppedHeight, sigmasAcross, sigmasDown );
		new PaletteStackWindow( paletteImage, paletteCanvas, this, defaultMax );

		paletteImage.setSlice( (initial_z - z_min) + 1 );

		for( int sigmaIndex = 0; sigmaIndex < sigmaValues.length; ++sigmaIndex ) {
			int sigmaY = sigmaIndex / sigmasAcross;
			int sigmaX = sigmaIndex % sigmasAcross;
			int offsetX = sigmaX * (croppedWidth + 1) + 1;
			int offsetY = sigmaY * (croppedHeight + 1) + 1;
			double sigma = sigmaValues[sigmaIndex];
			hep.setSigma(sigma);
			ImagePlus processed = hep.generateImage(cropped);
			copyIntoPalette( processed, paletteImage, offsetX, offsetY );
			paletteImage.updateAndDraw();
		}
	}

	public void copyIntoPalette( ImagePlus smallImage, ImagePlus paletteImage, int offsetX, int offsetY ) {
		int largerWidth = paletteImage.getWidth();
		int largerHeight = paletteImage.getHeight();
		int depth = paletteImage.getStackSize();
		if( depth != smallImage.getStackSize() )
			throw new RuntimeException("In copyIntoPalette(), depths don't match");
		int smallWidth = smallImage.getWidth();
		int smallHeight = smallImage.getHeight();
		float [] limits = Limits.getStackLimits( smallImage );
		ImageStack paletteStack = paletteImage.getStack();
		ImageStack smallStack = smallImage.getStack();
		// Make sure the minimum and maximum are sensible in the small stack:
		for( int z = 0; z < depth; ++z ) {
			float [] smallPixels = (float[])smallStack.getProcessor(z+1).getPixels();
			float [] palettePixels = (float[])paletteStack.getProcessor(z+1).getPixels();
			for( int y = 0; y < smallHeight; ++y ) {
				int smallIndex = y * smallWidth;
				System.arraycopy( smallPixels, smallIndex, palettePixels, (offsetY + y) * largerWidth + offsetX, smallWidth );
			}
		}
	}

	ImagePlus image;

	public void run( String ignoredArguments ) {

		image = IJ.getImage();
		if( image == null ) {
			IJ.error("There is no current image");
			return;
		}

		Calibration calibration = image.getCalibration();
                double minimumSeparation = 1;
                if( calibration != null )
                        minimumSeparation = Math.min(calibration.pixelWidth,
                                                     Math.min(calibration.pixelHeight,
                                                              calibration.pixelDepth));

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

		int x = p.xpoints[0];
		int y = p.ypoints[0];
		int z = image.getCurrentSlice() - 1;

		int either_side = 40;

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

		double [] sigmas = new double[9];
		for( int i = 0; i < sigmas.length; ++i ) {
			sigmas[i] = ((i + 1) * minimumSeparation) / 2;
		}

		makePalette( image, x_min, x_max, y_min, y_max, z_min, z_max, new TubenessProcessor(true), sigmas, 4, 3, 3, z );
	}
}