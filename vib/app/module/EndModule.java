package vib.app.module;

import vib.app.gui.Console;

public class EndModule extends Module {
	protected static final String name = "EndModule";
	protected static final String message = "Running the VIB protocol";

	protected static void run(State state, int index) {
		if (index != 0)
			return;
		AverageBrain.runOnAllImages(state);
	}
}
