package vib.app.module;

import java.io.File;
import ij.ImagePlus;
import vib.app.VIBImage;
import vib.app.Options;
import vib.AmiraTable;

public class LabelDiffusionTransformation extends Module {

	private VIBImage template;
	private VIBImage image;
	private Options options;
	
	public LabelDiffusionTransformation(
					VIBImage imp, VIBImage templ, Options options) {
		this.image = imp;
		this.template = templ;
		this.options = options;
	}

	public String getName() {
		return "Calculating label diffusion transformation";
	}

	public Module.Error checkDependency() {/*
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
		}*/
		// not available, but at least the requirements are fullfilled		
		return new Error(RESULTS_UNAVAILABLE, "");
	}
	
	public Object execute() {
		AmiraTable modelStatistics = image.getStatistics();
		modelStatistics.hide();
		AmiraTable templateStatistics = template.getStatistics();
		templateStatistics.hide();
		//if(!image.saveStatistics(statistics))
		//	console.append("Could not save statistics for " + image.getName());
		return null;
	}
}
