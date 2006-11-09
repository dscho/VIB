package vib.app.module;

import java.io.File;
import ij.io.Opener;
import ij.ImagePlus;
import vib.app.VIBImage;
import vib.app.Options;
import vib.Resample_;

public class Resample extends Module {

	private VIBImage image;
	private Options options;
	
	public Resample(VIBImage imp, Options options) {
		this.image = imp;
		this.options = options;
	}

	public String getName() {
		return "Resampling";
	}

	public Module.Error checkDependency() {
		// check requirements available
		for(int i = 0; i < options.getNumChannels(); i++) {
			File channel = new File(image.getChannelPath(i+1));
			if(!channel.exists())
				return new Error(REQUIREMENTS_UNAVAILABLE, 
						"Channel " + (i+1) + "does not exist");
		}
		File labels = new File(image.getLabelsPath());
		if(!labels.exists())
				return new Error(
					Module.REQUIREMENTS_UNAVAILABLE, "Labels do not exist");
		// check availability of results
		boolean available = true;
		boolean uptodate = true;
		for(int i = 0; i < options.getNumChannels(); i++) {
			File lowRes = new File(image.getResampledChannelPath(i+1));
			File highRes = new File(image.getChannelPath(i+1));
			if(!lowRes.exists())
				available = false;
			if(lowRes.lastModified() == 0L || 
					lowRes.lastModified() < highRes.lastModified()){
				uptodate = false;
			}
		}
		File labels_r = new File(image.getResampledLabelsPath());
		if(!labels_r.exists())
			available = false;
		if(labels_r.lastModified() == 0L ||
					labels_r.lastModified() < labels.lastModified())
			uptodate = false;

		// uptodate
		if(uptodate) {
			return new Error(RESULTS_OK,"");
		}
		// just available
		else if(available) {
			return new Error(RESULTS_OUT_OF_DATE, "");
		}
		// not available, but at least the requirements are fullfilled		
		return new Error(RESULTS_UNAVAILABLE, "");
	}
	
	public Object execute() {
		int resamplingFactor = options.getResamplingFactor();
		int referenceChannel = options.getRefChannel();
		for(int i = 0; i < options.getNumChannels(); i++) {
			if(i == referenceChannel) {
				ImagePlus resampled = Resample_.
					resample(image.getReferenceChannel(), resamplingFactor);
				image.setResampledReferenceChannel(resampled);
				image.saveResampledReferenceChannel();
			} else {
				ImagePlus resampled = 
					Resample_.resample(image.getChannel(i+1), resamplingFactor);
				image.saveResampledChannel(i+1, resampled);
			}
		}
		ImagePlus resampled = 
			Resample_.resample(image.getLabels(), resamplingFactor);
		image.setResampledLabels(resampled);
		if(!image.saveResampledLabels()) {
			console.append("Could not save resampled " + image.getName());
		}
		return null;
	}
}
