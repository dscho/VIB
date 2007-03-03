package vib.app.module;

import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import ij.IJ;
import ij.ImagePlus;
import ij.text.TextPanel;
import ij.util.Tools;
import vib.AverageBrain_;
import vib.InterpolatedImage;
import vib.TransformedImage;
import vib.app.VIBImage;
import vib.app.Options;
import vib.AmiraTable;
import vib.Center_Transformation;
import vib.DiffusionInterpol2_;
import vib.FastMatrix;
import vib.app.FileGroup;
import distance.TwoValues;

public class AverageBrain extends Module {

	private VIBImage template;
	
	public AverageBrain(VIBImage imp, Options options) {
		super(imp, options, true);
		this.template = image.getTemplate();
		dependingOn.add(LabelDiffusionTransformation.class);
	}

	public String getName() {
		return "Calculating average brain";
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

	protected void runThisModule() {
		FileGroup fg = options.getFileGroup();
		int numFiles = fg.size();
		int numChannels = options.getNumChannels();

		String[] files = new String[numFiles];
		FastMatrix[] matrices = new FastMatrix[numFiles];

		// for each channels
		for(int ch = 0; ch < numChannels; ch++) {
			for(int i = 0; i < numFiles; i++) {
				VIBImage image = new VIBImage(fg.get(i), options);
				files[i] = image.getWarpedPath(ch+1);
				matrices[i] = new FastMatrix(1.0);
			}
			AverageBrain_ ab = new AverageBrain_();
			ImagePlus scratch = new InterpolatedImage(
					template.getResampledReferenceChannel()).cloneDimensionsOnly().getImage();
			ab.doit(scratch, files, matrices);
			image.saveAverageChannel(scratch, ch+1);
		}

		// for the labelfields
		for(int i = 0; i < numFiles; i++) {
			VIBImage image = new VIBImage(fg.get(i), options);
			files[i] = image.warpedLabelsPath;
			matrices[i] = new FastMatrix(1.0);
		}
		AverageBrain_ ab = new AverageBrain_();
		ImagePlus scratch = new InterpolatedImage(
				template.getResampledReferenceChannel()).cloneDimensionsOnly().getImage();
		ab.doit(scratch, files, matrices);
		image.saveAverageLabels(scratch);
	}
}
