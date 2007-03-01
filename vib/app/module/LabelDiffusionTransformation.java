package vib.app.module;

import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import ij.IJ;
import ij.ImagePlus;
import ij.text.TextPanel;
import ij.util.Tools;
import vib.RigidRegistration_;
import vib.TransformedImage;
import vib.app.VIBImage;
import vib.app.Options;
import vib.AmiraTable;
import vib.Center_Transformation;
import vib.DiffusionInterpol2_;
import vib.FloatMatrix;
import distance.TwoValues;

public class LabelDiffusionTransformation extends Module {

	private VIBImage template;
	
	public LabelDiffusionTransformation(VIBImage imp, Options options) {
		super(imp, options, true);
		this.template = image.getTemplate();
		dependingOn.add(LabelCenterTransformation.class);
	}

	public String getName() {
		return "Calculating label diffusion transformation";
	}

	public int checkResults() {
		/*
		AmiraTable modelStatistics = image.getStatistics();
		modelStatistics.hide();

		Hashtable modelH = 
			(Hashtable)modelStatistics.getProperties().get("Parameters");
		System.out.println(modelH);
		// test if center transformation is stored:
		String key = template.basename + "SCenterTransformation";
		if(!modelH.containsKey(key)){
			return RESULTS_UNAVAILABLE;
		}
		
		// test if label transformation is stored for each non-empty
		// label:
		TextPanel panel = modelStatistics.getTextPanel();
		int count = panel.getLineCount();
		// index 0 is 'exterior'
		for (int i = 1; i < count; i++) {
			String[] line = Tools.split(panel.getLine(i), "\t");
			String materialName = line[1];
			int material = i;
			// check if labelfield is empty:
			int voxelCount = Integer.parseInt(line[2]);
			if(voxelCount == 0) {
				continue;
			}
			// write this into amira parameters
			key = template.basename
							+ "SLabelTransformation-" + materialName;
			if(!modelH.containsKey(key)){
				System.out.println("model does not contain " + key);
				return RESULTS_UNAVAILABLE;
			}
		}
		return RESULTS_OK;*/
		return RESULTS_UNAVAILABLE;
	}

	private FloatMatrix[] readTransformations() {
		System.out.println(image.statisticsPath);
		AmiraTable modelStatistics = image.getStatistics();
		modelStatistics.hide();

		Hashtable modelH = 
			(Hashtable)modelStatistics.getProperties().get("Parameters");
		List transformations = new ArrayList();
		TextPanel panel = modelStatistics.getTextPanel();
		int count = panel.getLineCount();
		// index 0 is 'exterior', include exterior (see DiffusionInterpol2)
		transformations.add(null);
		for (int i = 1; i < count; i++) {
			String[] line = Tools.split(panel.getLine(i), "\t");
			String materialName = line[1];
			int material = i;
			// check if labelfield is empty:
			int voxelCount = Integer.parseInt(line[2]);
			if(voxelCount == 0) {
				continue;
			}
			String key = template.basename
							+ "SLabelTransformation-" + materialName;
			String value = (String)modelH.get(key);
			if(value == null)
				console.append("transformation for material " + materialName + 
						" could not be read");
			transformations.add(FloatMatrix.parseMatrix(value));
		}

		return (FloatMatrix[])transformations.toArray(new FloatMatrix[]{});
	}
	
	protected void runThisModule() {
		/*
		if(image.getTemplate().name.equals(image.name)) {
			return;
		}*/
		ImagePlus templateLabels = template.getResampledLabels();
		// duplicate
		ImagePlus scratch = new ImagePlus(template.resampledLabelsPath);
		FloatMatrix[] trans = readTransformations();

		for(int i=0; i<options.getNumChannels(); i++) {
			ImagePlus model = image.getResampledChannel(i+1);
			boolean rememberDistortion = true;
			boolean reuseDistortion = false; //i>0;
			float tolerance = 0.5f;

			DiffusionInterpol2_ interpol = new DiffusionInterpol2_();
			interpol.initialize(scratch, templateLabels, model, trans, 
					reuseDistortion, rememberDistortion, tolerance);
			interpol.doit();
			//scratch.show();
			//scratch.updateAndDraw();
			//Thread.currentThread().stop();
			if(!image.saveWarped(i+1, model))
				console.append("could not save " + image.getWarpedPath(i+1));
		}

		// same wih labels

		/*
		run("DiffusionInterpol2 ", "model=model templatelabels=templateLabels labeltransformationlist=\[$trans\] rememberdistortion$reuse");


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
			String materialName = line[1];
			console.append("...material: " + line[1]);
			int material = i;
			// check if labelfield is empty:
			int voxelCount = Integer.parseInt(line[2]);
			if(voxelCount == 0) {
				continue;
			}
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
			String key = template.basename 
							+ "SLabelTransformation-" + materialName;
			String value = matrix.toStringForAmira();
			h.put(key,value);
			console.append("...save " + materialName + " in statistics");
			if(!image.saveStatistics(modelStatistics))
				console.append("Could not save statistics for " + image.name);
		}*/
	}
}
