package vib.app.module;

import java.util.Vector;
import java.util.Iterator;
import vib.app.gui.Console;

public abstract class Module {
	protected Console console;
	protected static Vector listeners = new Vector();

	public abstract String getName();
	protected abstract String getMessage();
	protected abstract void run(State state, int index);

	protected boolean runsOnce() { return false; }

	// at a later stage, these functions will schedule multi-threaded jobs
	public void runOnOneImage(State state, int index) {
		console = Console.instance();
		run(state, index);
		finished(index);
	}

	public void runOnAllImages(State state) {
		for (int i = 0; i < state.getImageCount(); i++)
			runOnOneImage(state, i);
	}

	public void runOnAllImagesAndTemplate(State state) {
		for (int i = -1; i < state.getImageCount(); i++)
			runOnOneImage(state, i);
	}

	public void prereqsDone(State state, int index) {
		String message = getMessage();
		if (!runsOnce())
			message += ": " + state.getBaseName(index) +
				" (" + (index + 1) + "/" +
				state.getImageCount() + ")";
		console.append(message);
	}

	public void finished(int index) {
		for(Iterator it = listeners(); it.hasNext();) {
			((ModuleListener)it.next()).moduleFinished(this, index);
		}
	}

	public static void addModuleListener(ModuleListener l) {
		listeners.add(l);
	}

	public static Iterator listeners() {
		return listeners.iterator();
	}
}
