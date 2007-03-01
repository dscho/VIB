package vib.app.module;

import java.io.File;
import ij.ImagePlus;
import ij.IJ;
import vib.app.VIBImage;
import vib.app.Options;
import vib.app.FileGroup;
import vib.TissueStatistics_;
import vib.AmiraTable;

public class EndModule extends Module {

	public EndModule(VIBImage imp, Options options) {
		super(imp, options, true);
		dependingOn.add(LabelDiffusionTransformation.class);
	}

	public String getName() {
		return "EndModule";
	}

	public int checkResults() {
		// always recalculate
		return RESULTS_UNAVAILABLE;
	}
	
	public void runThisModule() {
		// do no own work
		console.append("done");
	}
}
