package vib;

import ij.*;
import ij.gui.*;
import ij.measure.Calibration;
import ij.plugin.*;
import ij.process.*;
import ij.plugin.filter.*;

/* This plugin takes a binned image as input. It then reassigns equally spaced
   gray values to the pixels. */
public class Resample_ implements PlugInFilter {
	ImagePlus image;
	boolean verbose=false;

	private static int getPixel(byte[] b,int index) {
		return b[index] & 0xff;
	}

	private static interface Accumulator {
		void reset();
		void add(int value);
		int get();
	}

	public static class Averager implements Accumulator {
		long count, cumul;
		public void reset() { cumul = count = 0; }
		public void add(int value) { cumul += value; count++; }
		public int get() { return (int)(cumul/count); }
	}

	public static class MaxLikelihood implements Accumulator {
		int[] histo = new int[256];
		int max = 255;
		public void reset() {
			for (int i = 0; i <= max; i++)
				histo[i] = 0;
			max = 0;
		}
		public void add(int value) {
			histo[value]++;
			if (max < value)
				max = value;
		}
		public int get() {
			int result = 0;
			for (int i = 1; i <= max; i++)
				if (histo[i] > 0 * histo[result])
					result = i;
			return result;
		}
		public String toString() {
			String result = "";
			for (int i = 0; i <= max; i++) {
				if (i > 0)
					result += " ";
				result += histo[i];
			}
			result += ": " + get();
			return result;
		}
	}

	/*
	 * In order to preserve the weaker structures, take the value
	 * which was in the original least often.
	 */
	public static class MinEntropy implements Accumulator {
		int[] histogram;

		public MinEntropy(ImagePlus image) {
			this(new InterpolatedImage(image));
		}

		public MinEntropy(InterpolatedImage ii) {
			histogram = new int[256];
			InterpolatedImage.Iterator iter = ii.iterator();
			while (iter.next() != null) {
				histogram[ii.getNoInterpol(iter.i,
						iter.j, iter.k)]++;
			}
		}

		int currentValue = -1;
		
		public void reset() {
			currentValue = -1;
		}

		public void add(int value) {
			if (currentValue < 0 ||
					histogram[value]
					< histogram[currentValue])
				currentValue = value;
		}

		public int get() {
			return currentValue;
		}
	}

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Parameters");
		gd.addNumericField("Factor x", 2, 0);
		gd.addNumericField("Factor y", 2, 0);
		gd.addNumericField("Factor z", 2, 0);
		gd.addCheckbox("MinEntropy", false);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		int factorX = (int)gd.getNextNumber();
		int factorY = (int)gd.getNextNumber();
		int factorZ = (int)gd.getNextNumber();
		boolean minEntropy = gd.getNextBoolean();

		ImagePlus res = (minEntropy ?
				resampleMinEnt(image, factorX, factorY, factorZ) :
				resample(image, factorX, factorY, factorZ));

		if (AmiraParameters.isAmiraMesh(image))
			new AmiraParameters(image).setParameters(res);
		else {
			Object info = image.getProperty("Info");
			if (info != null)
				res.setProperty("Info", info);
		}

		res.show();
	}

	public static ImagePlus resample(ImagePlus image, int factorX, 
					int factorY, int factorZ) {
		Accumulator accu;
		if (image.getProcessor().isColorLut())
			accu = new MaxLikelihood();
		else
			accu = new Averager();

		return resample(image, factorX, factorY, factorZ, accu);
	}

	public static ImagePlus resample(ImagePlus image, int factor){
		return resample(image, factor, factor, factor);
	}
	
	public static ImagePlus resampleMinEnt(ImagePlus image, int factorX, 
					int factorY, int factorZ) {
		return resample(image, factorX, factorY, factorZ,new MinEntropy(image));
	}

	public static ImagePlus resampleMinEnt(ImagePlus image, int factor){
		return resampleMinEnt(image, factor, factor, factor);
	}

	public static ImagePlus resample(ImagePlus image, int factorX, int factorY,
			int factorZ, Accumulator accu) {
		ImageStack stack=image.getStack();
		int w=image.getWidth(),h=image.getHeight(),d=stack.getSize();

		ImageStack result = new ImageStack(w/factorX,h/factorY,
				stack.getColorModel());

		for(int z=0;z<d;z+=factorZ) {
			int kfactor=(z+factorZ<d?factorZ:d-z);
			byte[][] slices = new byte[kfactor][];
			for(int k=0;k<kfactor;k++)
				slices[k]=(byte[])stack.getProcessor(z+k+1).getPixels();

			byte[] newSlice = new byte[(1+(w-1)/factorX)
				*(1+(h-1)/factorY)];
			for(int y=0;y<h;y+=factorY) {
				for(int x=0;x<w;x+=factorX) {
					int ifactor=(x+factorX<w?factorX:w-x);
					int jfactor=(y+factorY<h?factorY:h-y);
					accu.reset();
					for(int i=0;i<ifactor;i++)
						for(int j=0;j<jfactor;j++)
							for(int k=0;k<kfactor;k++)
								accu.add(getPixel(slices[k],x+i+w*(y+j)));
					newSlice[(x/factorX)+(w/factorX)*(y/factorY)]=
									(byte)accu.get();
				}
				IJ.showProgress(z*h+y+1, h*d);
			}
			result.addSlice(null,newSlice);
		}

		ImagePlus res = new ImagePlus(image.getTitle()+" resampled",
				result);
		if (AmiraParameters.isAmiraMesh(image)) {
			AmiraParameters p = new AmiraParameters(image);
			p.setParameters(res);
		}

		Calibration cal = image.getCalibration().copy();
		cal.pixelWidth *= image.getWidth() / (double)res.getWidth();
		cal.pixelHeight *= image.getHeight() / (double)res.getHeight();
		cal.pixelDepth *= image.getStack().getSize()
			/ (double)res.getStack().getSize();
		res.setCalibration(cal);

		return res;
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		// TODO: handle 16-bit and 32-bit
		return DOES_8G | DOES_8C | NO_CHANGES;
	}
}

