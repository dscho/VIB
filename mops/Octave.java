package mops;

import java.util.List;
import java.util.ArrayList;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.measure.Calibration;

import vib.InterpolatedImage;

public class Octave {

	float[] sigma;
	private float[] sigma_diff;
	final float k;

	InterpolatedImage[] img;
	InterpolatedImage[] dog;
	
	final public int getWidth(){ return img[ 0 ].getWidth(); }
	final public int getHeight(){ return img[ 0 ].getWidth(); }
	final public int getDepth(){ return img[ 0 ].getWidth(); }

	public Octave( InterpolatedImage image, float[] sigma, float[] sigma_diff )
	{
		k = ( float )Math.pow( 2.0, 1.0 / ( sigma.length - 3 ) );
		this.sigma = sigma;
		this.sigma_diff = sigma_diff;
		img = new InterpolatedImage[ sigma.length ];
		img[ 0 ] = image;
		img[ img.length - 1 ] = Filter.gauss(
			img[ 0 ], sigma_diff[sigma_diff.length - 1]);
	}
	
	public InterpolatedImage resample()
	{
		ImagePlus imp = NewImage.createFloatImage(
				"",
				getWidth() / 2 + getWidth() % 2,
				getHeight() / 2 + getHeight() % 2,
				getDepth() / 2 + getDepth() % 2,
				NewImage.FILL_BLACK );
		Calibration c = img[ 0 ].getImage().getCalibration().copy();
		c.pixelWidth *= 2;
		c.pixelHeight *= 2;
		c.pixelDepth *= 2;
		imp.setCalibration( c );
		InterpolatedImage tmp = new InterpolatedImage( imp );
		int w = tmp.getWidth(), h = tmp.getHeight(), d = tmp.getDepth();
		
		for ( int z = 0; z < d; z++ )
			for ( int y = 0; y < h; y++ )
				for ( int x = 0; x < w; x++)
					tmp.setFloat( x, y, z, img[ img.length - 1 ].getNoCheckFloat(
						x*2, y*2, z*2 ) );
		
		return tmp;
	}

	public void dog() {
		int steps = sigma_diff.length;
		dog = new InterpolatedImage[steps - 1];
		for(int i = 1; i < steps - 1; i++) {
			img[ i ] = Filter.gauss(img[ 0 ], sigma_diff[i]);
			dog[ i - 1 ] = Filter.sub( img[ i ], img[ i - 1 ] );
		}
	}

	public void clear()
	{
		this.dog = null;
		this.img = null;
	}
}

