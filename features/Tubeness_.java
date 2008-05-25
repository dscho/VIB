/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package features;

import ij.*;
import ij.measure.Calibration;
import ij.plugin.*;
import ij.process.*;

public class Tubeness_ implements PlugIn {

	public void run(String ignored) {

		ImagePlus original = WindowManager.getCurrentImage();
		if (original == null) {
			IJ.error("No current image to calculate tubeness of.");
			return;
		}

		TubenessProcessor tp = new TubenessProcessor();

		ImagePlus result = tp.generateImage(original);
		result.setTitle("tubeness of " + original.getTitle());

		result.show();
	}
}
