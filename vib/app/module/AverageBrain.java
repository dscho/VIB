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
import amira.AmiraTable;
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
		FileGroup fg = options.getFileGroup();
		int numFiles = fg.size();
		int numChannels = options.getNumChannels();
		boolean uptodate = true;

		// for each channels
		for(int ch = 0; ch < numChannels; ch++) {
			File averageCh = new File(image.getAverageChannelPath(ch+1));
			if(!averageCh.exists())
				return RESULTS_UNAVAILABLE;
			long lm = averageCh.lastModified();
			VIBImage imp = null;
			File warped = null;
			for(int i = 0; i < numFiles; i++) {
				imp = new VIBImage(fg.get(i), options);	
				warped = new File(imp.getWarpedPath(ch+1));
				if(!warped.exists())
					return RESULTS_UNAVAILABLE;
				if(averageCh.lastModified() == 0L || 
						averageCh.lastModified() < warped.lastModified()){
					uptodate = false;
				}
			}
		}

		// for labels
		File averageLabels = new File(image.averageLabelsPath);
		if(!averageLabels.exists())
			return RESULTS_UNAVAILABLE;
		long lm = averageLabels.lastModified();
		VIBImage imp = null;
		File warped = null;
		for(int i = 0; i < numFiles; i++) {
			imp = new VIBImage(fg.get(i), options);	
			warped = new File(imp.warpedLabelsPath);
			if(!warped.exists())
				return RESULTS_UNAVAILABLE;
			if(averageLabels.lastModified() == 0L || 
					averageLabels.lastModified() < warped.lastModified()){
				uptodate = false;
			}
		}

		if(!uptodate) 
			return RESULTS_OUT_OF_DATE;

		return RESULTS_OK;
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
					template.getResampledReferenceChannel()).
					cloneDimensionsOnly().getImage();
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
