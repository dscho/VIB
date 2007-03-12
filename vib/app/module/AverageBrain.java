package vib.app.module;

import ij.ImagePlus;

import java.io.File;

import vib.AverageBrain_;
import vib.FastMatrix;
import vib.InterpolatedImage;

public class AverageBrain extends Module {
	protected static String name = "AverageBrain";

	public static void run(State state, int index) {
		if (index != 0)
			return;
		TransformImages.runOnAllImages(state);
		Resample.runOnAllImages(state);

		new AverageBrain().doit(state);
	}

	private void doit(State state) {
		matrices = null;
		scratch = null;
		for (int i = -1; i < state.options.numChannels; i++) {
			/* calculate AverageBrain */
			String outputPath = state.getWarpedPath(i);
			doit(state, getCompleteChannel(state, i), outputPath);
		}
	}

	private FastMatrix[] matrices;
	private ImagePlus scratch;

	public void doit(State state, String[] images, String outputPath) {
		if (state.upToDate(images, outputPath))
			return;
		AverageBrain_ averageBrain = new AverageBrain_();
		if (matrices == null)
			matrices = getMatrices(state);
		if (scratch == null)
			// TODO: invalidate template?
			scratch = state.getTemplateLabels();
		averageBrain.doit(scratch, images, matrices);
		state.save(scratch, outputPath);
	}

	private FastMatrix[] getMatrices(State state) {
		FastMatrix[] result = new FastMatrix[state.getImageCount()];
		for (int i = 0; i < result.length; i++) {
			result[i] = new FastMatrix();
			result[i].copyFrom(state.getTransformMatrix(i));
		}
		return result;
	}

	private String[] getCompleteChannel(State state, int channel) {
		String[] result = new String[state.getImageCount()];
		for (int i = 0; i < result.length; i++)
			result[i] = state.getWarpedPath(channel, i);
		return result;
	}
}

