/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package features;

import ij.ImageJ;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.FloatProcessor;

public abstract class HessianEvalueProcessor implements GaussianGenerationCallback {

	public void proportionDone(double d) {
		IJ.showProgress(d);
	}

	public abstract double measureFromEvalues2D( double [] evalues );

	public abstract double measureFromEvalues3D( double [] evalues );

	public ImagePlus generateImage(ImagePlus original) {

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
					double v = measureFromEvalues2D(evalues);
					slice[index] = (float)v;
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
							double v = measureFromEvalues3D(evalues);
							slice[index] = (float)v;
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

		ImagePlus result=new ImagePlus("processed " + original.getTitle(), stack);
		result.setCalibration(calibration);
		return result;		
	}
}
