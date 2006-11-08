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

	public Module.Error checkDependency() {
		console.append("\n * Resampling...\n");
		// check requirements
		for(int i = 0; i < options.getNumChannels(); i++) {
			File channel = new File(image.getChannelPath(i+1));
			if(!channel.exists())
				return new Error(
					Module.DEPENDENCIES_UNMET, "Not all channels exist");
		}
		File labels = new File(image.getLabelsPath());
		if(!labels.exists())
				return new Error(
					Module.DEPENDENCIES_UNMET, "Labels do not exist");
		// check availability of results
		boolean available = true;
		for(int i = 0; i < options.getNumChannels(); i++) {
			File lowRes = new File(image.getResampledChannelPath(i+1));
			File highRes = new File(image.getChannelPath(i+1));
			if(lowRes.lastModified() == 0L || 
					lowRes.lastModified() < highRes.lastModified()){
				available = false;
				break;
			}
		}
		File labels_r = new File(image.getResampledLabelsPath());
		if(labels_r.lastModified() == 0L ||
					labels_r.lastModified() < labels.lastModified())
			available = false;

		if(available) {
			console.append("...skipping, since results are already available");
			return new Error(Module.RESULTS_AVAILABLE, "");
		}
		// not available, but at least the requirements are fullfilled		
		return new Error(Module.DEPENDENCIES_MET, "");
	}
	
	public Object execute() {
		int resamplingFactor = options.getResamplingFactor();
		int referenceChannel = options.getRefChannel();
		for(int i = 0; i < options.getNumChannels(); i++) {
			console.append("resampling channel " + (i+1));
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
		console.append("resampling labels");
		ImagePlus resampled = 
			Resample_.resample(image.getLabels(), resamplingFactor);
		image.setResampledLabels(resampled);
		image.saveResampledLabels();
		return null;
	}
}
