/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package features;

import ij.*;
import ij.measure.Calibration;
import ij.plugin.*;
import ij.process.*;

public class Surfaceness_ implements PlugIn {

	public void run(String ignored) {

		ImagePlus original = WindowManager.getCurrentImage();
		if (original == null) {
			IJ.error("No current image to calculate surfaceness of.");
			return;
		}

                if( original.getStackSize() == 1 ) {
                        IJ.error("It only makes sense to look for Sufaceness of 3D images (stacks)");
                        return;
                }

		SurfacenessProcessor tp = new SurfacenessProcessor();

		ImagePlus result = tp.generateImage(original);
		result.setTitle("surfaceness of " + original.getTitle());

		result.show();
	}
}
