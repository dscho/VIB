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
	public String getName() { return "LabelCenterTransformation"; }
	protected String getMessage() { return "Registering the individual neuropils"; }

	protected void run(State state, int index) {
		new CenterTransformation().runOnOneImage(state, index);

		prereqsDone(state, index);

		String labelPath = state.getResampledPath(-1, index);
		System.out.println("MHL: Got label path: "+labelPath);
		String statisticsPath = state.getStatisticsPath(index);
		ImageMetaData templStats = state.getStatistics(-1);
		ImageMetaData stats = new ImageMetaData(statisticsPath);
		boolean needsUpDate =
			!state.upToDate(labelPath, statisticsPath);
		String transformLabel =
			state.getTransformLabel(Options.LABEL_DIFFUSION);

		String centerLabel = state.getTransformLabel(Options.CENTER);
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
			int iTempl = templStats.getMaterialIndex(m.name);
			if (iTempl < 0)
				continue;
			String matTransformLabel =
				transformLabel + m.name;
			if (!needsUpDate && stats.upToDate(labelPath,
						matTransformLabel))
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
			trans.measure = new TwoValues(iTempl, i);
			RigidRegistration_ rr = new RigidRegistration_();

			FastMatrix matrix2 = rr.rigidRegistration(trans,
					materialBBox, initialTransform,
					iTempl, i, noOptimization,
					level, stoplevel, tolerance,
					nInitialPositions, showTransformed, 
					showDifferenceImage,
					fastButInaccurate);
			String forAmira = matrix2.toStringForAmira();
			FloatMatrix floatMatrix =
				FloatMatrix.parseMatrix(forAmira);
			stats.setMatrix(matTransformLabel, floatMatrix);
			if(!stats.saveTo(statisticsPath))
				throw new RuntimeException("Could not save " + 
					statisticsPath);
		}
	}
}
