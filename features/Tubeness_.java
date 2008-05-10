/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package features;

import ij.*;
import ij.measure.Calibration;
import ij.plugin.*;
import ij.process.*;

/* For testing the hessianEigenvaluesAtPoint() method, essentially,
 * and experimenting with measures based on those eigenvalues. */
public class Tubeness_ implements PlugIn, GaussianGenerationCallback {

	public ImagePlus generateTubenessImage(ImagePlus original) {

		Calibration calibration=original.getCalibration();

		ComputeCurvatures c = new ComputeCurvatures(original, 1.0, this);
		c.run();

		int width = original.getWidth();
		int height = original.getHeight();
		int depth = original.getStackSize();

		System.out.println("w: " + width + ", h: " + height + ", d:" + depth);

		ImageStack stack = new ImageStack(width, height);

		double[] evalues = new double[3];

		if( depth == 1 ) {

			float[] slice = new float[width * height];

			for (int y = 1; y < height - 1; ++y) {
				for (int x = 1; x < width - 1; ++x) {

					c.hessianEigenvaluesAtPoint2D(x, y,
								      true, // order absolute
								      evalues,
								      false);

					int index = y * width + x;

					if ((evalues[1] >= 0) || (evalues[0] >= 0)) {

						// If either of the two principle eigenvalues
						// is positive then the curvature is in the
						// wrong direction - towards higher
						// instensities rather than lower.

						slice[index] = 0;

					} else {

						slice[index] = (float) Math.abs(evalues[1]);

					}
				}
				IJ.showProgress(1 / (double) height);
			}

			FloatProcessor fp = new FloatProcessor(width, height);
			fp.setPixels(slice);
			stack.addSlice(null, fp);

		} else {

			for (int z = 0; z < depth; ++z) {

				System.out.println("Working on slice: " + z);

				float[] slice = new float[width * height];

				if ((z >= 1) && (z < depth - 1)) {
					for (int y = 1; y < height - 1; ++y) {
						for (int x = 1; x < width - 1; ++x) {

							c.hessianEigenvaluesAtPoint3D(x, y, z,
										      true, // order absolute
										      evalues,
										      false);

							int index = y * width + x;

							if ((evalues[1] >= 0) || (evalues[2] >= 0)) {

								// If either of the two principle eigenvalues
								// is positive then the curvature is in the
								// wrong direction - towards higher
								// instensities rather than lower.

								slice[index] = 0;

							} else {

								slice[index] = (float) Math.sqrt(evalues[2] * evalues[1]);

							}
						}
					}
				}

				FloatProcessor fp = new FloatProcessor(width, height);
				fp.setPixels(slice);
				stack.addSlice(null, fp);
				IJ.showProgress(z / (double) depth);

			}

		}

		IJ.showProgress(1.0);

		ImagePlus result=new ImagePlus("tubeness of " + original.getTitle(), stack);
		result.setCalibration(calibration);
		return result;
	}

	public void run(String ignored) {

		ImagePlus original = WindowManager.getCurrentImage();
		if (original == null) {
			IJ.error("No current image to calculate tubeness of.");
			return;
		}

		ImagePlus result = generateTubenessImage(original);

		result.show();
	}

	public void proportionDone(double d) {
		IJ.showProgress(d);
	}
}
