package vib.app.module;

import ij.IJ;

import vib.app.gui.Console;

public class Show extends Module {
	protected String getName() { return "Show"; }
	protected String getMessage() { return "Displaying the results"; }
	protected boolean runsOnce() { return true; }

	protected void run(State state, int index) {
		if (index != 0)
			return;
		prereqsDone(state, index);

		for (int i = -1; i < state.options.numChannels; i++)
			IJ.open(state.getOutputPath(i));
	}
}
