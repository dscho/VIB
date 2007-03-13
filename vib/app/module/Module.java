package vib.app.module;

import vib.app.gui.Console;

public abstract class Module {
	protected Console console;

	protected abstract String getName();
	protected abstract String getMessage();
	protected abstract void run(State state, int index);

	// at a later stage, these functions will schedule multi-threaded jobs
	public void runOnOneImage(State state, int index) {
		if (console == null)
			console = Console.instance();
		console.append(getMessage() + ": " + index + "/" +
				state.getImageCount() + "\n");
		run(state, index);
	}

	public void runOnAllImages(State state) {
		for (int i = 0; i < state.getImageCount(); i++)
			runOnOneImage(state, i);
	}

	public void runOnAllImagesAndTemplate(State state) {
		for (int i = -1; i < state.getImageCount(); i++)
			runOnOneImage(state, i);
	}
}
