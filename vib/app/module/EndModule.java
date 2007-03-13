package vib.app.module;

import vib.app.gui.Console;

public class EndModule extends Module {
	protected String getName() { return "EndModule"; }
	protected String getMessage() { return "Running the VIB protocol"; }

	protected void run(State state, int index) {
		if (index != 0)
			return;
		new AverageBrain().runOnAllImages(state);
	}
}
