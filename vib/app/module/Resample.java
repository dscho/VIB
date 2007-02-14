package vib.app.module;

import java.io.File;
import ij.io.Opener;
import ij.ImagePlus;
import ij.IJ;
import vib.app.VIBImage;
import vib.app.Options;
import vib.app.FileGroup;
import vib.Resample_;

public class Resample extends Module {

	public Resample(VIBImage imp, Options options) {
		super(imp, options, true);
		dependingOn.add(Label.class);
	}

	public String getName() {
		return "Resampling";
	}

	public int checkResults() {
		// check availability of results
		boolean available = true;
		boolean uptodate = true;
		for(int i = 1; i <= options.getNumChannels(); i++) {
			File lowRes = new File(image.getResampledChannelPath(i));
			File highRes = new File(image.getChannelPath(i));
			if(!lowRes.exists())
				available = false;
			if(lowRes.lastModified() == 0L || 
					lowRes.lastModified() < highRes.lastModified()){
				uptodate = false;
			}
		}
		File labels = new File(image.labelsPath);
		File labels_r = new File(image.resampledLabelsPath);
		if(!labels_r.exists())
			available = false;
		if(labels_r.lastModified() == 0L ||
					labels_r.lastModified() < labels.lastModified())
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
	
	protected void runThisModule() {
		int resamplingFactor = options.getResamplingFactor();
		int referenceChannel = options.getRefChannel();
		for(int i = 1; i <= options.getNumChannels(); i++) {
			if(i == referenceChannel) {
				console.append("...retrieve and resample channel " + i + 
						" of " + image.name);
				ImagePlus resampled = Resample_.
					resample(image.getReferenceChannel(), resamplingFactor);
				image.setResampledReferenceChannel(resampled);
				console.append("...save channel " + i + " of " + image.name);
				image.saveResampledReferenceChannel();
				image.releaseReferenceChannel();
				image.releaseResampledReferenceChannel();
			} else {
				console.append("...retrieve and resample channel " + i + 
						" of " + image.name);
				ImagePlus resampled = 
					Resample_.resample(image.getChannel(i), resamplingFactor);
				console.append("...save channel " + i + " of " + image.name);
				image.saveResampledChannel(i, resampled);
			}
		}
		console.append("...retrieve and resample labels of " + image.name);
		ImagePlus resampled = 
			Resample_.resample(image.getLabels(), resamplingFactor);
		image.releaseLabels();
		image.setResampledLabels(resampled);
		console.append("...save labels of " + image.name);
		if(!image.saveResampledLabels()) {
			console.append("Could not save resampled " + image.name);
		}
		image.releaseResampledLabels();
	}
}
