/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;

public class Overlay_Registered implements PlugIn {
	
	public void run(String ignored) {
		
		System.out.println("Thread in plugin is: "+Thread.currentThread());
		
		int[] wList = WindowManager.getIDList();
		if (wList == null) {
			IJ.error("No images are open.");
			return;
		}

		String[] titles = new String[wList.length + 1];
		for (int i = 0; i < wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp != null ? imp.getTitle() : "";
		}

		String none = "*None*";
		titles[wList.length] = none;

		GenericDialog gd = new GenericDialog("Overlay Transformed");
		gd.addChoice("A:", titles, titles[0]);
		gd.addChoice("B:", titles, titles[1]);
		gd.addCheckbox("Keep source images", true);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}

		int[] index = new int[2];
		index[0] = gd.getNextChoiceIndex();
		index[1] = gd.getNextChoiceIndex();

		ImagePlus [] sourceImages = new ImagePlus[2];
		
		sourceImages[0] = WindowManager.getImage(wList[index[0]]);
		sourceImages[1] = WindowManager.getImage(wList[index[1]]);
		
		int width = sourceImages[0].getWidth();
		int height = sourceImages[0].getHeight();
		int depth = sourceImages[0].getStackSize();
		
		if ( ! (width == sourceImages[1].getWidth() &&
			height == sourceImages[1].getHeight() &&
			depth == sourceImages[1].getStackSize())) {
			IJ.error("The dimensions of image stack " +
				 sourceImages[1].getTitle() +
				 " do not match those of " + sourceImages[0].getTitle());
			return;
		}
		
		int type0 = sourceImages[0].getType();
		int type1 = sourceImages[1].getType();
		
		if( type0 != type1 ) {
			IJ.error("Can't overlay two images of different types.");
			return;
		}
		
		int bitDepth;

		StackConverter converter=new StackConverter(sourceImages[0]);
		converter.convertToGray8();

		converter=new StackConverter(sourceImages[1]);
		converter.convertToGray8();

		RGBStackMerge merger=new RGBStackMerge();
		ImageStack merged = merger.mergeStacks(
			width,
			height,
			depth,
			sourceImages[0].getStack(),
			sourceImages[1].getStack(),
			sourceImages[0].getStack(),
			true);
		
		ImagePlus rgbResult = new ImagePlus("Merged",merged);
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


	}

}
