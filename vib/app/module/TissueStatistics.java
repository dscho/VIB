package vib.app.module;

import java.io.File;
import ij.ImagePlus;
import vib.app.VIBImage;
import vib.app.Options;
import vib.TissueStatistics_;
import vib.AmiraTable;

public class TissueStatistics extends Module {

	private VIBImage image;
	private Options options;
	
	public TissueStatistics(VIBImage imp, Options options) {
		this.image = imp;
		this.options = options;
	}

	public String getName() {
		return "Calculating tissue statistics";
	}

	public Module.Error checkDependency() {
		// check requirements available
		File labels_r = new File(image.getResampledLabelsPath());
		if(!labels_r.exists()){
			return new Error(
					REQUIREMENTS_UNAVAILABLE, "Labelfield does not exist");
		}	
		// check availability of results
		boolean available = true;
		boolean uptodate = true;
		File statistic = new File(image.getStatisticsPath());
		if(!statistic.exists())
			available = false;
		if(statistic.lastModified() == 0L || 
				statistic.lastModified() < labels_r.lastModified())
			uptodate = false;
		// uptodate
		if(uptodate) {
			return new Error(RESULTS_OK,"");
		}
		// just available
		else if(available) {
			return new Error(RESULTS_OUT_OF_DATE, "");
		}
		// not available, but at least the requirements are fullfilled		
		return new Error(RESULTS_UNAVAILABLE, "");
	}
	
	public Object execute() {
		ImagePlus labelField = image.getResampledLabels();
		AmiraTable statistics = new TissueStatistics_().
									calculateStatistics(labelField);
		statistics.hide();
		if(!image.saveStatistics(statistics))
			console.append("Could not save statistics for " + image.getName());
		return null;
	}
}
