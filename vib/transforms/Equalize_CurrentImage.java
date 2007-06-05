/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/*
    FIXME: This was taken from the ContrastEnhancer class in ImageJ
    for reasons that aren't clear to me now. (MHL)
    
*/


package vib.transforms;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.text.*;

public class Equalize_CurrentImage implements PlugIn {
	
	int max, range;
	boolean classicEqualization=false;
	int stackSize;
	boolean updateSelectionOnly=false;
	
	static boolean equalize=true;
	static boolean normalize=false;
	static boolean processStack=true;
	static boolean useStackHistogram=false;
	static boolean entireImage=false;
	static double saturated = 0.5;
	
	public void run(String ignoredArg) {
		ImagePlus imp=WindowManager.getCurrentImage();
		equalize(imp);
	}
	
	public void equalize(ImagePlus imp) {
		if (imp.getBitDepth()==32) {
			IJ.showMessage("Contrast Enhancer", "Equalization of 32-bit images not supported.");
			return;
		}
		classicEqualization = IJ.altKeyDown();
		ImageStack stack = imp.getStack();
		stackSize=stack.getSize();
		for (int i=1; i<=stackSize; i++) {
			IJ.showProgress(i, stackSize);
			ImageProcessor ip = stack.getProcessor(i);
			equalize(ip);
		}
	}
	
	/**	
		Changes the tone curves of images. 
		It should bring up the detail in the flat regions of your image.
		Histogram Equalization can enhance meaningless detail and hide 
		important but small high-contrast features. This method uses a
		similar algorithm, but uses the square root of the histogram 
		values, so its effects are less extreme. Hold the alt key down 
		to use the standard histogram equalization algorithm.
		This code was contributed by Richard Kirk (rak@cre.canon.co.uk).
	*/ 	
	public void equalize(ImageProcessor ip) {
		
		int[] histogram = ip.getHistogram();
		ip.resetRoi();
		if (ip instanceof ShortProcessor) {	// Short
			max = 65535;
			range = 65535;
		} else { //bytes
			max = 255;
			range = 255;
		}
		
		double sum;
		
		sum = getWeightedValue(histogram, 0);
		for (int i=1; i<max; i++)
			sum += 2 * getWeightedValue(histogram, i);
		sum += getWeightedValue(histogram, max);
		
		double scale = range/sum;
		int[] lut = new int[range+1];
		
		lut[0] = 0;
		sum = getWeightedValue(histogram, 0);
		for (int i=1; i<max; i++) {
			double delta = getWeightedValue(histogram, i);
			sum += delta;
			lut[i] = (int)Math.round(sum*scale);
			sum += delta;
		}
		lut[max] = max;
		
		applyTable(ip, lut);
	}
	
	private double getWeightedValue(int[] histogram, int i) {
		int h = histogram[i];
		if (h<2 || classicEqualization) return (double)h;
		return Math.sqrt((double)(h));
	}
	
	void applyTable(ImageProcessor ip, int[] lut) {
		if (updateSelectionOnly) {
			ImageProcessor mask = ip.getMask();
			if (mask!=null) ip.snapshot();
			ip.applyTable(lut);
			if (mask!=null) ip.reset(mask);
		} else
			ip.applyTable(lut);
	}
	
}
