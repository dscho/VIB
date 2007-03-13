package vib.app.module;

import ij.ImagePlus;

import vib.app.ImageMetaData;
import vib.app.Options;

import vib.DiffusionInterpol2_;
import vib.FloatMatrix;
import vib.InterpolatedImage;
import vib.RigidRegistration_;
import vib.TransformedImage;

public class LabelDiffusionTransformation extends Module {
	protected String getName() { return "LabelDiffusionTransformation"; }
	protected String getMessage() {
		return "Calculating label diffusion transformation";
	}

	protected void run(State state, int index) {
		new LabelCenterTransformation().runOnOneImage(state, index);

		ImagePlus templateLabels = null;

		DiffusionInterpol2_ interpol = new DiffusionInterpol2_();
		boolean rememberDistortion = true;
		boolean reuseDistortion = false;
		float tolerance = 0.5f;

		FloatMatrix[] transformations = null;

		// DiffusionInterpolation for all channels
		for(int i = -1; i < state.options.numChannels; i++) {
			String imagePath = state.getResampledPath(i, index);
			String warpedPath = state.getWarpedPath(i, index);
			String statisticsPath = state.getStatisticsPath(index);
			if (state.upToDate(new String[] { imagePath,
					statisticsPath }, warpedPath))
				continue;

			if (templateLabels == null)
				templateLabels = state.getTemplateLabels();
			InterpolatedImage ii =
				new InterpolatedImage(templateLabels);
			ImagePlus scratch =
				ii.cloneDimensionsOnly().getImage();

			if (transformations == null)
				transformations =
					readTransformations(state, index);

			ImagePlus model = state.getImage(imagePath);

			/*
			 * Copy transformations because they get transformed
			 * in DiffusionInterpol2_.
			 */
			FloatMatrix[] trans = copyMatrices(transformations);
			interpol.initialize(scratch, templateLabels, model,
					trans,
					reuseDistortion, rememberDistortion,
					tolerance);
			interpol.doit();
			reuseDistortion = true; // true after the first channel
			state.save(scratch, warpedPath);
		}

	}

	private static FloatMatrix[] readTransformations(State state,
			int index) {
		ImageMetaData templStats = state.getStatistics(-1);
		ImageMetaData stats = state.getStatistics(index);

		FloatMatrix[] result =
			new FloatMatrix[templStats.materials.length];
		for (int i = 0; i < result.length; i++) {
			String name = templStats.materials[i].name;
			String transformName = state.getTransformLabel() +
				name;
			result[i] = stats.getMatrix(transformName);
			if (result[i] == null)
				result[i] = new FloatMatrix(1.0f);
		}

		return result;
	}

	private static FloatMatrix[] copyMatrices(FloatMatrix[] orig) {
		FloatMatrix[] res = new FloatMatrix[orig.length];
		System.arraycopy(orig, 0, res, 0, orig.length);
		return res;
	}
}
