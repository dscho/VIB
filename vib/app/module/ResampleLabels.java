package vib.app.module;

import ij.ImagePlus;

import vib.Resample_;

public class ResampleLabels extends Module {
	protected String getName() { return "ResampleLabels"; }
	protected String getMessage() { return "Resampling label"; }

	protected void run(State state, int index) {
		if (index < 0)
			new Label().runOnAllImagesAndTemplate(state);

		if (state.options.resamplingFactor == 1)
			return;
		String labelPath = state.getImagePath(-1, index);
		String resampledPath = state.getResampledPath(-1, index);
		if (state.upToDate(labelPath, resampledPath))
			return;

		ImagePlus image = state.getImage(labelPath);
		ImagePlus resampled = Resample_.resample(image,
				state.options.resamplingFactor);
		state.save(resampled, resampledPath);
	}
}
