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

	public String getName() {
		return "Loading";
	}
	
	public Module.Error checkDependency() {
		// check requirements available
		if(!options.isValid()) {
			return new Module.Error(Module.REQUIREMENTS_UNAVAILABLE, 
					"The options are not valid");
		}
		// check if results are already available and uptodate
		boolean resultsAvailable = true;
		boolean resultsUptodate = true;
		for(int i = 1; i <= options.getNumChannels(); i++) {
			File channelFile = new File(image.getChannelPath(i));
			if(!channelFile.exists()){
				resultsAvailable = false;
			}
			if(channelFile.lastModified() == 0L || 
					channelFile.lastModified() < file.lastModified()) {
				resultsUptodate = false;
			}
		}
		// they are uptodate
		if(resultsUptodate) {
			return new Module.Error(Module.RESULTS_OK, "");
		}
		// they are just avaiable
		else if(resultsAvailable) {
			return new Module.Error(Module.RESULTS_OUT_OF_DATE, "");
		}
		// not available, but at least the calculations can be done
		return new Module.Error(Module.RESULTS_UNAVAILABLE, "");
	}

	public VIBImage execute() {
		fillOutputPanel(console);
		int refChannel = options.getRefChannel();
		String path = file.getParent() + File.separator + file.getName();
		
		// Now just read the first numChannels channels of the stored images
		// (and return the one corresponding to the reference channel).
		leica.Leica_SP_Reader reader = (leica.Leica_SP_Reader)IJ.
							runPlugIn("leica.Leica_SP_Reader", path);
		if(reader.getNumberOfChannels() < numChannels){
			console.append("File " + file.getName() + 
					" does not contain " + numChannels + " channels");
			throw new RuntimeException();
		}
		for(int i = 0; i < numChannels; i++) {
			ImagePlus img = reader.getImage(i);
			if(i+1 == refChannel) {
				image.setReferenceChannel(img);
				img.setTitle(file.getName());
			}
			if(!image.saveChannel(i+1, img));
				console.append("Could not write channel " + (i+1));
		}
		clearOutputPanel();
		return image;
	}
}
