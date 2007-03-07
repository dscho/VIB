package vib.app.module;

import java.io.File;
import ij.ImagePlus;
import ij.IJ;
import vib.app.VIBImage;
import vib.app.Options;
import vib.TissueStatistics_;
import amira.AmiraTable;

public class TissueStatistics extends Module {

	public TissueStatistics(VIBImage imp, Options options) {
		super(imp, options, false);
		dependingOn.add(Resample.class);
	}

	public String getName() {
		return "Calculating tissue statistics";
	}

	public int checkResults() {
		File labels_r = new File(image.resampledLabelsPath);
		// check availability of results
		boolean available = true;
		boolean uptodate = true;
		File statistic = new File(image.statisticsPath);
		if(!statistic.exists())
			available = false;
		if(statistic.lastModified() == 0L || 
				statistic.lastModified() < labels_r.lastModified())
			uptodate = false;
		// uptodate
		if(uptodate) {
			return RESULTS_OK;
		}
		// just available
		else if(available) {
			return RESULTS_OUT_OF_DATE;
		}
		// not available, but at least the requirements are fullfilled		
		return RESULTS_UNAVAILABLE;
	}
	
	public void runThisModule() {
		console.append("...retrieve labels of " + image.name);
		ImagePlus labelField = image.getResampledLabels();
		console.append("...calculate statistics of " + image.name);
		AmiraTable statistics = new TissueStatistics_().
									calculateStatistics(labelField);
		statistics.hide();
		console.append("...save statistics of " + image.name);
		if(!image.saveStatistics(statistics))
			console.append("Could not save statistics for " + image.name);

	}
}
