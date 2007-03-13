package vib.app.module;

import distance.TwoValues;

import ij.ImagePlus;

import vib.app.ImageMetaData;
import vib.app.Options;

import vib.FastMatrix;
import vib.FloatMatrix;
import vib.RigidRegistration_;
import vib.TransformedImage;

public class LabelCenterTransformation extends Module {
	protected String getName() { return "LabelCenterTransformation"; }
	protected String getMessage() { return "Registering the individual neuropils"; }

	protected void run(State state, int index) {
		new CenterTransformation().runOnOneImage(state, index);

		prereqsDone(state, index);

		String labelPath = state.getResampledPath(-1, index);
		String statisticsPath = state.getStatisticsPath(index);
		ImageMetaData stats = new ImageMetaData(statisticsPath);
		boolean needsUpDate =
			!state.upToDate(labelPath, statisticsPath);
		String transformLabel = state.getTemplateBaseName() +
			Options.TRANSFORMS[Options.LABEL_DIFFUSION];

		String centerLabel = state.getTemplateBaseName() +
			Options.TRANSFORMS[Options.CENTER];
		FloatMatrix matrix = stats.getMatrix(centerLabel);

		ImagePlus labels = null, templLabels = null;

		// rigid registration for each label
		int count = stats.materials.length;
		// index 0 is 'exterior'
		for (int i = 1; i < count; i++) {
			ImageMetaData.Material m = stats.materials[i];
			// check if labelfield is empty:
			if (m.count == 0)
				continue;
			String matTransformLabel =
				transformLabel + m.name;
			if (!needsUpDate && stats.getMatrix(matTransformLabel)
					!= null)
				continue;

			String initialTransform = matrix.toStringForAmira();
			int level = 4;
			int stoplevel = 2;
			double tolerance = 4.0;
			String materialBBox = "";
			boolean noOptimization = false;
			int nInitialPositions = 1;
			boolean showTransformed = false;
			boolean showDifferenceImage = false;
			boolean fastButInaccurate = false;
			if (labels == null)
				labels = state.getImage(labelPath);
			if (templLabels == null)
				templLabels = state.getTemplateLabels();

			while (level > 0 &&
					(templLabels.getWidth() >> level) < 32)
				level--;
			if (stoplevel > level)
				stoplevel = level;
			TransformedImage trans = new TransformedImage(
					templLabels, labels);
			// TODO: be more graceful about different orders
			trans.measure = new TwoValues(i, i);
			RigidRegistration_ rr = new RigidRegistration_();

			console.append("...rigidRegistration");
			FastMatrix matrix2 = rr.rigidRegistration(trans,
					materialBBox, initialTransform,
					i, i, noOptimization,
					level, stoplevel, tolerance,
					nInitialPositions, showTransformed, 
					showDifferenceImage,
					fastButInaccurate);
			String forAmira = matrix2.toStringForAmira();
			FloatMatrix floatMatrix =
				FloatMatrix.parseMatrix(forAmira);
			stats.setMatrix(matTransformLabel, floatMatrix);
			stats.saveTo(statisticsPath);
		}
	}
}
