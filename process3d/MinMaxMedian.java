package process3d;

import java.util.Arrays;
import java.awt.image.ColorModel;

import ij.process.FloatProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import ij.ImagePlus;
import ij.ImageStack;
import ij.IJ;

/**
 * This class implements the minimum, maximum and median filter.
 * The kernel size is fixed with a diameter of 3 pixels. This 
 * makes sense since all three filters are computationally very
 * expensive. Computational complexity is related to the third power 
 * of the diameter, so 2-fold diameter means 8-fold computation time.
 */
public class MinMaxMedian {

	/** Constant representing the minimum filter. */
	public static final int MINIMUM = 0;
	/** Constant representing the maximum filter. */
	public static final int MAXIMUM = 1;
	/** Constant representing the median filter. */
	public static final int MEDIAN  = 2;

	private static int method = MEDIAN;

	private static int w, h, d;
	private static int diameter = 3;
	private static Slide[] slices_in, slices_out;
	private static boolean isByte, isFloat;

	/** 
	 * Main method which iterates through the stack and calls 
	 * convolvePoint() for each voxel.
	 */
	public static ImagePlus convolve(ImagePlus image, int method) {
		
		if(method < 0 || method > 3) {
			IJ.error("Neither MIN nor MAX nor MEDIAN chosen");
			return null;
		}
		// Determine dimensions of the image
		w = image.getWidth(); h = image.getHeight();
		d = image.getStackSize();
		
		// Adjust minimum and maximum indices (because of filter size)
		int min = diameter/2;
		int max_x = w - diameter/2;
		int max_y = h - diameter/2;
		int max_z = d - diameter/2;
	
		// determine image type
		isByte = image.getProcessor().getPixels() instanceof byte[];
		isFloat = image.getProcessor().getPixels() instanceof float[];
		ColorModel cm = image.getProcessor().getColorModel();
		if(!isFloat && !isByte) {
			IJ.error("Float or byte image expected");
			return null;
		}

		// initialize slices_in and slices_out
		slices_in = new Slide[d];
		slices_out = new Slide[d];
		for(int i = 0; i < d; i++) {
			if(isByte)
				slices_in[i] = 
					new ByteSlide((byte[])image.getStack()
					.getProcessor(i+1).getPixels(), cm);
			else
				slices_in[i] = 
					new FloatSlide((float[])image.getStack()
					.getProcessor(i+1).getPixels(), cm);
			slices_out[i] = slices_in[i].getEmptySlide();
		}


		// convolve
		for(int z = min; z < max_z; z++) {
			IJ.showProgress(z, max_z);
			for(int y = min; y < max_y; y++) {
				for(int x = min; x < max_x; x++) {
					slices_out[z].setValue(x, y, 
					  slices_out[z].convolvePoint(z, y, x));
				}
			}
		}
		
		// create output image
		ImageStack stack = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			stack.addSlice("", slices_out[z].getProcessor());
		}
		ImagePlus result = new ImagePlus("", stack);
		result.setCalibration(image.getCalibration());
		return result;
	}
	
	/**
	 * Abstract class which warpes either a byte or float array.
	 */
	private abstract static class Slide {
		protected ColorModel cm;
		/** Constructor */
		Slide(ColorModel cm) {
			this.cm = cm;
		}
		/** Returns the pixel value at x, y */
		abstract Object getValue(int x, int y);
		/** Sets the specified value at x, y */
		abstract void setValue(int x, int y, Object value);
		/** Retrieve a corresponding empty ImageProcessor. */
		abstract ImageProcessor getEmptyProcessor();
		/** Retrieve the ImageProcessor for this slide. */
		abstract ImageProcessor getProcessor();
		/** Returns an empty slide of this type */
		abstract Slide getEmptySlide();
		/** Convolve the specified point */
		abstract Object convolvePoint(int z, int y, int x);
	}	

	/**
	 * Class which warps a float array representing one slide.
	 */
	private static class FloatSlide extends Slide {
		float[] slide;
		FloatSlide(float[] slide, ColorModel cm) {
			super(cm);
			this.slide = slide;
		}
		void setValue(int x, int y, Object value) {
			Float v = (Float)value;
			slide[y*w+x] = v;
		}
		Float getValue(int x, int y) {
			return slide[y*w+x];
		}
		ImageProcessor getEmptyProcessor() {
			return new FloatProcessor(w, h, new float[w*h], cm);
		}
		ImageProcessor getProcessor() {
			return new FloatProcessor(w, h, slide, cm);
		}
		Slide getEmptySlide() {
			return new FloatSlide(new float[w*h], cm);
		}
		static int size = diameter*diameter*diameter;
		static float[] values = new float[size];
		Float convolvePoint(int z, int y, int x) {
			int index = 0;
			int r = diameter/2;
			for(int k=-r/2; k<=+r/2; k++) {
				for(int j=-r/2; j<=+r/2; j++) {
					for(int i=-r/2; i<=+r/2; i++) {
						values[index++] = 
							(Float)slices_in[z+k].
							getValue(x+i,y+j);
					}
				}
			}
			Arrays.sort(values);
			if(method == MAXIMUM)
				return values[0];
			if(method == MINIMUM)
				return values[size-1];
			return values[size/2];
		}
	}

	/**
	 * Class warping a byte array representing one slide.
	 */
	private static class ByteSlide extends Slide {
		byte[] slide;
		ByteSlide(byte[] slide, ColorModel cm) {
			super(cm);
			this.slide = slide;
		}
		void setValue(int x, int y, Object value) {
			Byte v = (Byte)value;
			slide[y*w+x] = v;
		}
		Byte getValue(int x, int y) {
			return slide[y*w+x];
		}
		ImageProcessor getEmptyProcessor() {
			return new ByteProcessor(w, h, new byte[w*h], cm);
		}
		ImageProcessor getProcessor() {
			return new ByteProcessor(w, h, slide, cm);
		}
		Slide getEmptySlide() {
			return new ByteSlide(new byte[w*h], cm);
		}
		static int size = diameter * diameter * diameter;
		static int[] values = new int[size];
		Byte convolvePoint(int z, int y, int x) {
			int index = 0;
			int r = diameter/2;
			for(int k=-r; k<=+r; k++) {
				for(int j=-r; j<=+r; j++) {
					for(int i=-r; i<=+r; i++) {
						values[index++] = 
							(Byte)slices_in[z+k].
							getValue(x+i,y+j)&0xff;
					}
				}
			}
			Arrays.sort(values);
			if(method == MAXIMUM)
				return (byte)values[0];
			if(method == MINIMUM)
				return (byte)values[size-1];
			return (byte)values[size/2];
		}
	}
}
