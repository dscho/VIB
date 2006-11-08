package vib.app.module;

// import leica.Leica_SP_Reader;
import vib.app.VIBImage;
import vib.app.Options;
import vib.app.gui.dialog.Console;

import ij.IJ;
import ij.macro.Interpreter;
import ij.ImagePlus;
import ij.io.FileSaver;

import java.io.File;
import java.io.IOException;

public class Load extends Module {

	private Options options;
	private File file;
	private int numChannels;
	private VIBImage image;
	
	public Load(VIBImage image, File file, Options options) {
		this.file = file;
		this.numChannels = options.getNumChannels();
		this.options = options;
		this.image = image;
	}
	
	public Module.Error checkDependency() {
		console.append("\n * Loading...\n");
		// check requirements
		if(!options.isValid()) {
			return new Module.Error(Module.DEPENDENCIES_UNMET, 
					"The options are not valid");
		}
		// check if results are already available
		boolean resultsAvailable = true;
		for(int i = 1; i <= options.getNumChannels(); i++) {
			File channelFile = new File(image.getChannelPath(i));
			if(channelFile.lastModified() == 0L || 
					channelFile.lastModified() < file.lastModified()) {
				resultsAvailable = false;
				break;
			}
		}
		// they are avaiable
		if(resultsAvailable) {
			console.append("...skipping. Results are already available");
			return new Module.Error(Module.RESULTS_AVAILABLE, "");
		}
		// not available, but at least the calculations can be done
		return new Module.Error(Module.DEPENDENCIES_MET, "");
	}

	public VIBImage execute() {
		fillOutputPanel(console);
		console.setBusy(true);
		int refChannel = options.getRefChannel();
		console.append("Reading " + 
				numChannels + " channels of image " + file.getName());
		String path = file.getParent() + File.separator + file.getName();
		
		// Now just read the first numChannels channels of the stored images
		// (and return the one corresponding to the reference channel).
		leica.Leica_SP_Reader reader = (leica.Leica_SP_Reader)IJ.
							runPlugIn("leica.Leica_SP_Reader", path);
		if(reader.getNumberOfChannels() < numChannels)
			IJ.error("File " + file.getName() + 
					" does not contain " + numChannels + " channels");
		for(int i = 0; i < numChannels; i++) {
			ImagePlus img = reader.getImage(i);
			if(i+1 == refChannel) {
				image.setReferenceChannel(img);
				img.setTitle(file.getName());
			}
			console.append("Saving channel " + (i+1));
			boolean w = image.saveChannel(i+1, img);
			if(w)
				console.append("Successfully written channel " + (i+1));
			else
				console.append("Could not write channel " + (i+1));
		}
		console.setBusy(false);
		clearOutputPanel();
		return image;
	}
}
