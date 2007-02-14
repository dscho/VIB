package vib.app.module;

import vib.app.VIBImage;
import vib.app.Options;

import ij.IJ;
import ij.macro.Interpreter;
import ij.ImagePlus;
import ij.io.FileSaver;

import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import vib.app.Options;
import vib.app.VIBImage;

public class Load extends Module {

	private int numChannels;
	
	public Load(VIBImage image, Options options) {
		super(image, options, false);
		this.numChannels = options.getNumChannels();
	}

	public String getName() {
		return "Loading";
	}
	
	public int checkResults() {
		// check if results are already available and uptodate
		boolean resultsAvailable = true;
		boolean resultsUptodate = true;
		File fileToLoad = new File(image.originalPath);
		for(int i = 1; i <= options.getNumChannels(); i++) {
			File channelFile = new File(image.getChannelPath(i));
			if(!channelFile.exists()){
				resultsAvailable = false;
			}
			if(channelFile.lastModified() == 0L || 
					channelFile.lastModified() < fileToLoad.lastModified()) {
				resultsUptodate = false;
			}
		}
		// they are uptodate
		if(resultsUptodate) {
			return RESULTS_OK;
		}
		// they are just avaiable
		else if(resultsAvailable) {
			return RESULTS_OUT_OF_DATE;
		}
		// not available, but at least the calculations can be done
		return RESULTS_UNAVAILABLE;
	}

	protected void runThisModule() {
		// check if requirements for this module are fullfilled
		if(!options.isValid()) {
			console.append("Options are not valid");
			IJ.error("Options are not valid");
		}
		
		// start the calculations
		int refChannel = options.getRefChannel();
		String path = image.originalPath;
		
		// Now just read the first numChannels channels of the stored images
		// (and return the one corresponding to the reference channel).
		console.append("...loading file " + image.name);
		leica.Leica_SP_Reader reader = (leica.Leica_SP_Reader)IJ.
							runPlugIn("leica.Leica_SP_Reader", path);
		if(reader.getNumberOfChannels() < numChannels){
			console.append("File " + image.name + 
					" does not contain " + numChannels + " channels");
			throw new RuntimeException();
		}
		for(int i = 1; i <= numChannels; i++) {
			ImagePlus img = reader.getImage(i-1);
			if(i == refChannel) {
				image.setReferenceChannel(img);
				img.setTitle(image.name);
			}
			console.append("...saving channel " + i 
										+ " of file " + image.name);
			if(!image.saveChannel(i, img)){
				console.append("Could not write channel " + i);
			}
		}
	}
}
