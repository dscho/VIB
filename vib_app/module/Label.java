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
		grey.show();
		IJ.runPlugIn("Segmentation_Editor","");
		return null;
	}
}
