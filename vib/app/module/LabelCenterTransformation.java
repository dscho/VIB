package vib.app.module;

import java.io.File;
import ij.ImagePlus;
import vib.app.VIBImage;
import vib.app.Options;
import vib.AmiraTable;
import vib.Center_Transformation;
import vib.FastMatrix;

public class LabelCenterTransformation extends Module {

	private VIBImage template;
	private VIBImage image;
	private Options options;
	
	public LabelCenterTransformation(
					VIBImage imp, VIBImage templ, Options options) {
		this.image = imp;
		this.template = templ;
		this.options = options;
	}

	public String getName() {
		return "Calculating label center transformation";
	}

	public Module.Error checkDependency() {
		/*
		// check requirements available
		File statisticsM = new File(image.getStatisticsPath());
		File statisticsT = new File(template.getStatisticsPath());
		if(!statisticsM.exists() || !statisticsT.exists()){
			return new Error(
					REQUIREMENTS_UNAVAILABLE, "Statistics files do not exist");
		}	
		// check availability of results
		boolean available = true;
		boolean uptodate = true;
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
		// not available, but at least the requirements are fullfilled */
		return new Error(RESULTS_UNAVAILABLE, "");
	}
	
	public Object execute() {
		AmiraTable modelStatistics = image.getStatistics();
		modelStatistics.hide();
		AmiraTable templateStatistics = template.getStatistics();
		templateStatistics.hide();
		Center_Transformation.bestRigid(
										modelStatistics, templateStatistics);
		
		if(!image.saveStatistics(modelStatistics))
			console.append("Could not save statistics for " + image.getName());
		return null;
	}
}
