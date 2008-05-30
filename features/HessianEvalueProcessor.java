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

	public abstract float measureFromEvalues2D( float [] evalues );
	public abstract float measureFromEvalues3D( float [] evalues );

	protected boolean normalize = false;
	protected double sigma = 1.0;

	public ImagePlus generateImage(ImagePlus original) {

		Calibration calibration=original.getCalibration();

		ComputeCurvatures c = new ComputeCurvatures(original, sigma, this);
		c.run();

		int width = original.getWidth();
		int height = original.getHeight();
		int depth = original.getStackSize();

		System.out.println("w: " + width + ", h: " + height + ", d:" + depth);

		ImageStack stack = new ImageStack(width, height);

		float[] evalues = new float[3];

		if( depth == 1 ) {

			float[] slice = new float[width * height];

			for (int y = 1; y < height - 1; ++y) {
				for (int x = 1; x < width - 1; ++x) {

					c.hessianEigenvaluesAtPoint2D(x, y,
								      true, // order absolute
								      evalues,
								      normalize,
								      false);
					int index = y * width + x;
					slice[index] = measureFromEvalues2D(evalues);
				}
				IJ.showProgress(1 / (double) height);
			}

			FloatProcessor fp = new FloatProcessor(width, height);
			fp.setPixels(slice);
			stack.addSlice(null, fp);

		} else {

			for (int z = 0; z < depth; ++z) {

				float[] slice = new float[width * height];

				if ((z >= 1) && (z < depth - 1)) {
					for (int y = 1; y < height - 1; ++y) {
						for (int x = 1; x < width - 1; ++x) {

							c.hessianEigenvaluesAtPoint3D(x, y, z,
										      true, // order absolute
										      evalues,
										      normalize,
										      false);
							int index = y * width + x;
							slice[index] = measureFromEvalues3D(evalues);
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
