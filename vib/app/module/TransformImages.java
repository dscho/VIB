package vib.app.module;

import vib.app.Options;

public class TransformImages extends Module {
	protected String name = "TransformImages";
	protected String message = "Transforming images";

	protected void run(State state, int index) {
		Module module = null;
		switch (state.options.transformationMethod) {
		case Options.CENTER:
			module = new CenterTransformation();
			break;
		case Options.LABEL_DIFFUSION:
			module = new LabelDiffusionTransformation();
			break;
		default:
			throw new RuntimeException("invalid transformation: "
				+ state.options.transformationMethod);
		}
		module.runOnOneImage(state, index);
	}
}

