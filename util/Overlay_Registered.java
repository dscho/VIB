/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import vib.TransformedImage;

public class Overlay_Registered implements PlugIn {
	
	public void run(String ignored) {

                String titleSubstring = null;
                boolean closeAllOthers = false;
                
                String macroOptions = Macro.getOptions();
		if (macroOptions != null) {
                        titleSubstring = Macro.getValue(macroOptions, "substring", null);
                        if( null != Macro.getValue(macroOptions,"close",null) ) {
                            closeAllOthers = true;
                        }
                }
		
                if (titleSubstring == null)
                        titleSubstring = "";
                
		int[] wList = WindowManager.getIDList();
		if (wList == null) {
			IJ.error("No images are open.");
			return;
		}

                String [] matchingTitles=new String[wList.length];
                ImagePlus [] matchingImagePlus=new ImagePlus[wList.length];
                ImagePlus [] allImages=new ImagePlus[wList.length];
                
                int totalMatchingTitles = 0;
		for (int i = 0; i < wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
                        String title = (imp == null) ? "" : imp.getTitle();
                        if(title.indexOf(titleSubstring) >= 0) {
                            matchingTitles[totalMatchingTitles] = title;
                            matchingImagePlus[totalMatchingTitles] = imp;
                            ++totalMatchingTitles;
                        }
                        allImages[i] = imp;
		}
                
                if( totalMatchingTitles < 2 ) {
                    IJ.error("There are only "+totalMatchingTitles+" matching images; need at least 2.");
                    return;
                }
                
                String [] onlyMatchingTitles = new String[totalMatchingTitles];
                System.arraycopy(matchingTitles,0,onlyMatchingTitles,0,totalMatchingTitles);
                ImagePlus [] onlyMatchingImagePlus = new ImagePlus[totalMatchingTitles];
                System.arraycopy(matchingImagePlus, 0, onlyMatchingImagePlus, 0, totalMatchingTitles);

		GenericDialog gd = new GenericDialog("Overlay Transformed");
		gd.addChoice("A:", onlyMatchingTitles, onlyMatchingTitles[0]);
		gd.addChoice("B:", onlyMatchingTitles, onlyMatchingTitles[1]);
		gd.addCheckbox("Keep source images", true);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}

		int[] index = new int[2];
		index[0] = gd.getNextChoiceIndex();
		index[1] = gd.getNextChoiceIndex();

		ImagePlus [] sourceImages = new ImagePlus[2];
		
		sourceImages[0] = onlyMatchingImagePlus[index[0]];
		sourceImages[1] = onlyMatchingImagePlus[index[1]];
		
                float[] valueRange;
                {
                    TransformedImage ti = new TransformedImage(
                        sourceImages[0],
                        sourceImages[1]);

                    valueRange = ti.getValuesRange();
                }
                
                sourceImages[0].getProcessor().setMinAndMax(valueRange[0],valueRange[1]);
                sourceImages[1].getProcessor().setMinAndMax(valueRange[0],valueRange[1]);
                
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

                if(closeAllOthers) {
                    for( int i=0; i < allImages.length; ++i ) {
                        allImages[i].close();
                    }
                }
	}
}
