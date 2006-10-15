package vib_app.module;

import ij.IJ;
import ij.ImagePlus;

public class Label extends Module {

	private ImagePlus grey;
	
	public Label(ImagePlus imp) {
		this.grey = imp;
	}

	public Module.Error checkDependency() {
		return new Module.Error(Module.DEPENDENCY_FINE, "");
	}

	public Object execute() {
		boolean busy = true;
		reportState(busy);
		broadcast("Please label the brain in the segmentation editor");
		grey.show();
		IJ.runPlugIn("Segmentation_Editor","");
		return null;
	}
}
