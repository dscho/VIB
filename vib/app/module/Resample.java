package vib.app.module;

import ij.io.Opener;
import ij.ImagePlus;
import vib.app.Options;
import vib.Resample_;

public class Resample extends Module {

	private ImagePlus imp;
	private int resamplingFactor;
	
	public Resample(ImagePlus imp, int resamplingFactor) {
		this.imp = imp;
		this.resamplingFactor = resamplingFactor;
	}

	public Resample(String filename, int resamplingFactor) {
		this.imp = new Opener().openImage(filename);
		this.resamplingFactor = resamplingFactor;
	}

	public Module.Error checkDependency() {
		return new Module.Error(Module.DEPENDENCY_FINE, "");
	}
	
	public Object execute() {
		broadcast("Resampling image " + imp.getTitle());
		imp.show();
		// ImagePlus resampled = Resample_.resample(imp, resamplingFactor);
		// resampled.show();
		// save the resampled image
		
		return null;
	}
}
