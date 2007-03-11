package vib.app.module;

import vib.app.gui.Console;
import vib.app.State;

public class Module {
	protected static Console console;
	protected static String name = "EndModule";

	protected static void run(State state, int index) {
		if (console == null)
			console = Console.instance();
		if (index != 0)
			return;
		AverageBrain.runOnAllImages(state);
	}

	// at a later stage, these functions will schedule multi-threaded jobs
	public static void runOnOneImage(State state, int index) {
		run(state, index);
	}

	public static void runOnAllImages(State state) {
		for (int i = 0; i < state.getFileCount(); i++)
			runOnOneImage(state, i);
	}

	public static void runOnAllImagesAndTemplate(State state) {
		for (int i = -1; i < state.getFileCount(); i++)
			runOnOneImage(state, i);
	}
}
