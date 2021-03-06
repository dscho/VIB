/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.measure.Calibration;
import vib.TransformedImage;

import java.util.ArrayList;

public class Overlay_Registered implements PlugIn {

	public static ImageStack overlayToStack( ImagePlus a, ImagePlus b ) {

		float[] valueRange;
		{
			TransformedImage ti = new TransformedImage( a, b );
			valueRange = ti.getValuesRange();
		}

		a.getProcessor().setMinAndMax(valueRange[0],valueRange[1]);
		b.getProcessor().setMinAndMax(valueRange[0],valueRange[1]);

		int width = a.getWidth();
		int height = a.getHeight();
		int depth = a.getStackSize();

		if ( ! (width == b.getWidth() &&
			height == b.getHeight() &&
			depth == b.getStackSize())) {
			IJ.error("The dimensions of image stack " +
				 b.getTitle() +
				 " do not match those of " + a.getTitle());
			return null;
		}

		int type0 = a.getType();
		int type1 = b.getType();

		if( type0 != type1 ) {
			IJ.error("Can't overlay two images of different types.");
			return null;
		}

		int bitDepth;

		float [] range0 = getValuesRange(a);
		float [] range1 = getValuesRange(b);

		// FIXME: this does change the original images to GRAY_8, so this isn't ideal.

		a.getProcessor().setMinAndMax(range0[0],range0[1]);
		StackConverter converter=new StackConverter(a);
		converter.convertToGray8();

		b.getProcessor().setMinAndMax(range1[0],range1[1]);
		converter=new StackConverter(b);
		converter.convertToGray8();

		RGBStackMerge merger=new RGBStackMerge();
		ImageStack merged = merger.mergeStacks(
			width,
			height,
			depth,
			a.getStack(),
			b.getStack(),
			a.getStack(),
			true);

		return merged;
	}

	public static ImagePlus overlayToImagePlus( ImagePlus a, ImagePlus b ) {

		ImageStack merged = overlayToStack( a, b );

		ImagePlus result = new ImagePlus( "Merged", merged );

		Calibration ca = a.getCalibration();
		Calibration cb = b.getCalibration();
		if( ca == null && cb == null ) {
			// Then that's fine...
		} else if( ca != null ) {
			if( ! ca.equals( cb ) ) {
				IJ.error("The calibrations of the two images differ");
				return null;
			}
			result.setCalibration( ca );
		} else {
			IJ.error("Calibration is set in one image but not the other.");
			return null;
		}

		return result;
	}

	public static float[] getValuesRange(ImagePlus imagePlus) {

		int stackSize      = imagePlus.getStackSize();
		ImageStack stack      = imagePlus.getStack();
		int bitDepth      = imagePlus.getBitDepth();

		float minValue = Float.MAX_VALUE;
		float maxValue = Float.MIN_VALUE;

		int z;
		for( z = 0; z < stackSize; ++z ) {
			if( 8 == bitDepth) {
				byte [] pixels = (byte[])stack.getPixels(z+1);
				for( int i = 0; i < pixels.length; ++i ) {
					int value = pixels[i] & 0xFF;
					if( value > maxValue )
						maxValue = value;
					if( value < minValue )
						minValue = value;
				}
			} else if( 16 == bitDepth ) {
				short [] pixels = (short[])stack.getPixels(z+1);
				for( int i = 0; i < pixels.length; ++i ) {
					short value = pixels[i];
					if( value > maxValue )
						maxValue = value;
					if( value < minValue )
						minValue = value;
				}
			}
		}

		float [] result = new float[2];

		result[0] = minValue;
		result[1] = maxValue;

		return result;
	}

	public void run(String ignored) {

		String macroOptions = Macro.getOptions();

		String mustHaveSubstring = "";

		String titleSubstring = null;
		boolean closeAllOthers = false;

		if (macroOptions != null) {
			String value = Macro.getValue(macroOptions, "substring", null);
			if( value != null ) {
				mustHaveSubstring = value;
			}
			value = Macro.getValue(macroOptions, "keep", null);
			if( value != null ) {
				System.out.println("Got keep!: '"+value+"'");
			}
			value = Macro.getValue(macroOptions, "close", null);
			if( value != null ) {
				System.out.println("Got close!: '"+value+"'");
			}
		}

		int[] wList = WindowManager.getIDList();
		if (wList == null) {
			IJ.error("No images are open.");
			return;
		}
		if (wList.length < 2 ) {
			IJ.error("Must have at least two images open.");
			return;
		}

		ArrayList<String> matchingTitles = new ArrayList<String>();
		ArrayList<ImagePlus> matchingImages = new ArrayList<ImagePlus>();

		ImagePlus [] matchingImagePlus=new ImagePlus[wList.length];
		ImagePlus [] allImages=new ImagePlus[wList.length];

		int totalMatchingTitles = 0;
		for (int i = 0; i < wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			String title = imp != null ? imp.getTitle() : "";
			if ( title.indexOf(mustHaveSubstring) >= 0 ) {
				System.out.println("Yes, matched: "+title);
				matchingTitles.add(title);
				matchingImages.add(imp);
			} else
				System.out.println("No, didn't match '"+"' in: "+title);
		}

		if( matchingTitles.size() < 2 ) {
			IJ.error("Fewer than two images matched the substring '"+mustHaveSubstring+"'");
			return;
		}

		GenericDialog gd = new GenericDialog("Overlay Transformed");
		gd.addChoice("A:", (String[])matchingTitles.toArray(new String[1]), matchingTitles.get(0));
		gd.addChoice("B:", (String[])matchingTitles.toArray(new String[1]), matchingTitles.get(1));
		gd.addCheckbox("Keep source images", true);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}

		int[] index = new int[2];
		index[0] = gd.getNextChoiceIndex();
		index[1] = gd.getNextChoiceIndex();

		ImagePlus [] sourceImages = new ImagePlus[2];

		sourceImages[0] = matchingImages.get(index[0]);
		sourceImages[1] = matchingImages.get(index[1]);

		ImagePlus rgbResult = overlayToImagePlus( sourceImages[0], sourceImages[1] );
		rgbResult.show();

		ModelessQuestions q=new ModelessQuestions("Rate This Registration",rgbResult);

		q.addTextField("I dunno", 20, "Your message:" );

		q.addTextField("Well", 10, "Something");

		q.addLabel("Just a label.");

		q.addCompletingButton("ok","Done");
		q.addCompletingButton("unhappy","Rubbish!");

		for( int i = 0; i <= 10; ++i ) {
			q.addRadio("rating",""+i);
		}

		q.waitForAnswers();

		IJ.error("Finished waiting for answers!");

		if(closeAllOthers) {
			for( int i=0; i < allImages.length; ++i ) {
				allImages[i].close();
			}
		}
	}
}
