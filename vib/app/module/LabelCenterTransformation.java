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
	
	public LabelCenterTransformation(VIBImage imp, 
									VIBImage templ, Options options) {
		super(imp, options, false);
		this.template = templ;
	}

	public String getName() {
		return "Calculating label center transformation";
	}

	public int checkResults() {
		// check requirements available
		File statisticsM = new File(image.statisticsPath);
		File statisticsT = new File(template.statisticsPath);
		// check availability of results
		// TODO
		/*
		boolean available = true;
		boolean uptodate = true;
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
		// not available, but at least the requirements are fullfilled */
		return RESULTS_UNAVAILABLE;
	}
	
	protected void runThisModule() {
		AmiraTable modelStatistics = image.getStatistics();
		modelStatistics.hide();
		AmiraTable templateStatistics = template.getStatistics();
		templateStatistics.hide();
		Center_Transformation.bestRigid(modelStatistics, templateStatistics);
		
		if(!image.saveStatistics(modelStatistics))
			console.append("Could not save statistics for " + image.name);
	}
}
