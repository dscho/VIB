package vib.app.module;

import ij.ImagePlus;

import java.io.File;

import vib.AverageBrain_;
import vib.FastMatrix;
import vib.InterpolatedImage;

public class AverageBrain extends Module {
	public String getName() { return "AverageBrain"; }
	protected String getMessage() { return "Averaging brain"; }
	protected boolean runsOnce() { return true; }

	protected void run(State state, int index) {
		if (index != 0)
			return;
		new TransformImages().runOnAllImages(state);
		new Resample().runOnAllImages(state);

		prereqsDone(state, index);

		new AverageBrain().doit(state);
	}

	private void doit(State state) {
		matrices = null;
		scratch = null;
		for (int i = -1; i < state.options.numChannels; i++) {
			/* calculate AverageBrain */
			String outputPath = state.getOutputPath(i);
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
			// try to reuse labels if they were already loaded
			scratch = state.options.needsLabels() ?
				state.getTemplateLabels() :
				state.getTemplate();
		averageBrain.doit(scratch, images, matrices);
		if(!state.save(scratch, outputPath))
			throw new RuntimeException(
				"Could not save " + outputPath);
	}

	private FastMatrix[] getMatrices(State state) {
		FastMatrix[] result = new FastMatrix[state.getImageCount()];
		for (int i = 0; i < result.length; i++) {
			result[i] = new FastMatrix();
			/*
			 * AverageBrain needs a FastMatrix, but
			 * state.getTransformMatrix() returns a FloatMatrix...
			 */
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

