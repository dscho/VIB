package vib.app.module;

import amira.AmiraTable;

import ij.ImagePlus;

import java.io.File;

import vib.app.State;
import vib.AverageBrain_;
import vib.FastMatrix;
import vib.InterpolatedImage;

public class AverageBrain extends Module {
	protected static String name = "AverageBrain";

	public static void run(State state, int index) {
		if (index != 0)
			return;
		TransformImages.runOnAllImages(state);

		new AverageBrain().doit(state);
	}

	private void doit(State state) {
		matrices = null;
		scratch = null;
		for (int i = 0; i < state.channels.length; i++) {
			/* calculate AverageBrain */
			String outputPath = state.getWarpedPath(i);
			doit(state, state.channels[i], outputPath);
		}
		/* calculate MainProbs */
		String outputPath = state.getWarpedLabelsPath();
		doit(state, state.labels, outputPath);
	}

	private FastMatrix[] matrices;
	private ImagePlus scratch;

	public void doit(State state, String[] images, String outputPath) {
		if (upToDate(images, outputPath))
			return;
		AverageBrain_ averageBrain = new AverageBrain_();
		if (matrices == null)
			matrices = getMatrices(state);
		if (scratch == null)
			// TODO: invalidate template?
			scratch = state.getTemplate();
		averageBrain.doit(scratch, images, matrices);
		state.save(scratch, outputPath);
	}

	private boolean upToDate(String[] images, String outputPath) {
		File output = new File(outputPath);
		if (!output.exists())
			return false;
		for (int i = 0; i < images.length; i++) {
			File image = new File(images[i]);
			if (!image.exists())
				continue;
			try {
				if (image.lastModified() >
						output.lastModified())
					return false;
			} catch (Exception e) {
				// ignore unreadable file
			}
		}
		return true;
	}

	private FastMatrix[] getMatrices(State state) {
		FastMatrix[] result = new FastMatrix[state.channels[0].length];
		for (int i = 0; i < result.length; i++) {
			AmiraTable table = state.getStatistics(i);
			String matrix = table.get(state.getTransformLabel());
			try {
				if (matrix != null)
					result[i] =
						FastMatrix.parseMatrix(matrix);
			} catch (Exception e) {
				// will set to identity
			}
			if (result[i] == null)
				result[i] = new FastMatrix(1.0);
		}
		return result;
	}
}

