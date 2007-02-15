package vib.app.module;

import java.util.Hashtable;
import java.io.File;
import ij.ImagePlus;
import ij.text.TextPanel;
import ij.util.Tools;
import vib.RigidRegistration_;
import vib.TransformedImage;
import vib.app.VIBImage;
import vib.app.Options;
import vib.AmiraTable;
import vib.Center_Transformation;
import vib.FastMatrix;
import distance.TwoValues;

public class LabelCenterTransformation extends Module {

	private VIBImage template;
	
	public LabelCenterTransformation(VIBImage imp, Options options) {
		super(imp, options, true);
		this.template = image.getTemplate();
		dependingOn.add(TissueStatistics.class);
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
		console.append("...Loading model statistics");
		AmiraTable modelStatistics = image.getStatistics();
		modelStatistics.hide();
		console.append("...Loading template statistics");
		AmiraTable templateStatistics = template.getStatistics();
		templateStatistics.hide();
		console.append("...calculate center transformation");
		FastMatrix centerTransform = Center_Transformation.
							bestRigid(modelStatistics, templateStatistics);
		console.append("...saving center transformation in statistics file");
		if(!image.saveStatistics(modelStatistics))
			console.append("Could not save statistics for " + image.name);

		// rigid registration for each label
		TextPanel panel = modelStatistics.getTextPanel();
		int count = panel.getLineCount();
		// index 0 is 'exterior'
		for (int i = 1; i < count; i++) {
			String[] line = Tools.split(panel.getLine(i), "\t");
			console.append("...material: " + line[1]);
			int material = i;
			String initialTransform = centerTransform.toStringForAmira();
			int level = 4;
			int stoplevel = 2;
			double tolerance = 4.0;
			String materialBBox = "";
			boolean noOptimization = false;
			int nInitialPositions = 1;
			boolean showTransformed = false;
			boolean showDifferenceImage = false;
			boolean fastButInaccurate = false;
		 	TransformedImage trans = new TransformedImage(
					template.getLabels(), image.getLabels());
			trans.measure = new TwoValues(material, material);
			RigidRegistration_ rr = new RigidRegistration_();

			console.append("...rigidRegistration");
			FastMatrix matrix = rr.rigidRegistration(trans, materialBBox, 
					initialTransform, material, material, noOptimization, level,
					stoplevel, tolerance, nInitialPositions, showTransformed, 
					showDifferenceImage, fastButInaccurate);
			// write this into amira parameters
			Hashtable h = (Hashtable)modelStatistics.
									getProperties().get("Parameters");
			String key = templateStatistics.
									getTitle() + "SLabelTransformation ";
			String value = matrix.toStringForAmira();
			h.put(key,value);
			console.append("...save statistics");
			if(!image.saveStatistics(modelStatistics))
				console.append("Could not save statistics for " + image.name);
		}
	}
}
