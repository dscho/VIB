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
import vib.InterpolatedImage;
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
		// check availability
		File statistics = new File(image.statisticsPath);
		File warped = null;
		boolean uptodate = true;
		for(int i=0; i<options.getNumChannels(); i++) {
			warped = new File(image.getWarpedPath(i+1));
			if(!warped.exists()) {
				return RESULTS_UNAVAILABLE;
			}
			if(warped.lastModified() == 0L || 
				warped.lastModified() < statistics.lastModified()) {
				uptodate = false;
			}
		}
		warped = new File(image.warpedLabelsPath);
		if(!warped.exists()) {
			return RESULTS_UNAVAILABLE;
		}
		if(warped.lastModified() == 0L || 
			warped.lastModified() < statistics.lastModified()) {
			uptodate = false;
		}
		if(!uptodate)
			return RESULTS_OUT_OF_DATE;
		return RESULTS_OK;
	}

	private FloatMatrix[] readTransformations() {
		AmiraTable modelStatistics = image.getStatistics();
		modelStatistics.hide();

		Hashtable modelH = 
			(Hashtable)modelStatistics.getProperties().get("Parameters");
		TextPanel panel = modelStatistics.getTextPanel();
		int count = panel.getLineCount();
		FloatMatrix[] transformations = new FloatMatrix[count];
		/*
		 * DiffusionInterpol2_ needs an array of transformations with 
		 * an entry for _ALL_ materials, even if they are not labelled.
		 * Then, the transformation is just null. The same holds true 
		 * for 'Exterior'
		 */
		transformations[0] = null;
		for (int i = 1; i < count; i++) {
			String[] line = Tools.split(panel.getLine(i), "\t");
			String materialName = line[1];
			int material = i;
			// check if labelfield is empty:
			int voxelCount = Integer.parseInt(line[2]);
			if(voxelCount == 0) {
				transformations[i] = null;
				continue;
			}
			String key = template.basename
							+ "SLabelTransformation-" + materialName;
			String value = (String)modelH.get(key);
			if(value == null)
				console.append("transformation for material " + materialName + 
						" could not be read");
			transformations[i] = FloatMatrix.parseMatrix(value);
		}

		return transformations;
	}

	private FloatMatrix[] copyMatrices(FloatMatrix[] orig) {
		FloatMatrix[] res = new FloatMatrix[orig.length];
		for(int i = 0; i < orig.length; i++) {
			if(orig[i] == null)
				res[i] = null;
			else 
				res[i] = new FloatMatrix(orig[i]);
		}
		return res;
	}
	
	protected void runThisModule() {
		ImagePlus templateLabels = template.getResampledLabels();
		// workaround: Labelfields have a wrong Calibration
		templateLabels.setCalibration(
				template.getResampledReferenceChannel().getCalibration());
		// duplicate
		ImagePlus scratch = new InterpolatedImage(templateLabels)
										.cloneDimensionsOnly().getImage();
		DiffusionInterpol2_ interpol = new DiffusionInterpol2_();
		boolean rememberDistortion = true;
		boolean reuseDistortion = false;
		float tolerance = 0.5f;
		FloatMatrix[] transformations = readTransformations();

		// DiffusionInterpolation for all channels
		for(int i=0; i<options.getNumChannels(); i++) {
			ImagePlus model = image.getResampledChannel(i+1);
			// copy transformations because they get transformed in 
			// DiffusionInterpol2_.
			FloatMatrix[] trans = copyMatrices(transformations);
			interpol.initialize(scratch, templateLabels, model, trans, 
					reuseDistortion, rememberDistortion, tolerance);
			interpol.doit();
			reuseDistortion = true; // true after the first channel
			if(!image.saveWarped(i+1, scratch))
				console.append("could not save " + image.getWarpedPath(i+1));
		}

		// DiffusionInterpolation for labels
		ImagePlus model = image.getResampledLabels();
		// workaround for wrong calibration of Labelfields
		model.setCalibration(
				image.getResampledReferenceChannel().getCalibration());
		FloatMatrix[] trans = copyMatrices(transformations);
		interpol.initialize(scratch, templateLabels, model, trans, 
				reuseDistortion, rememberDistortion, tolerance);
		interpol.doit();
		if(!image.saveWarpedLabels(scratch))
			console.append("could not save " + image.warpedLabelsPath);
	}
}
