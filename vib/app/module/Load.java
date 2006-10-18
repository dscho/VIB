package vib.app.module;

// import leica.Leica_SP_Reader;
import vib.app.Options;

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
	
	public Load(File file, int numChannels, Options options) {
		this.file = file;
		this.numChannels = numChannels;
		this.options = options;
	}
	
	public Module.Error checkDependency() {
		if(!options.isValid()) {
			return new Module.Error(Module.DATA_MISSING, 
					"The current project contains invalid data");
		}
		return new Module.Error(Module.DEPENDENCY_FINE, "");
	}

	public ImagePlus execute() {
		boolean busy = true;
		reportState(busy);
		int refChannel = options.getRefChannel();
		broadcast("Reading " + 
				numChannels + " channels of image " + file.getName());
		String path = file.getParent() + File.separator + file.getName();
		
		// Now just read the first numChannels channels of the stored images
		// (and return the one corresponding to the reference channel).
		leica.Leica_SP_Reader reader = (leica.Leica_SP_Reader)IJ.
							runPlugIn("leica.Leica_SP_Reader", path);
		if(reader.getNumberOfChannels() < numChannels)
			IJ.error("File " + file.getName() + 
					" does not contain " + numChannels + " channels");
		ImagePlus ret = null;
		for(int i = 0; i < numChannels; i++) {
			ImagePlus img = reader.getImage(i);
			if(i+1 == refChannel)
				ret = img;
			String dirS = options.getWorkingDirectory()
								+ File.separator
								+ "images-" + (i+1) + File.separator;
			File dir = new File(dirS);
			if(!dir.exists()) 
				dir.mkdir();
			
			String savepath = dirS + File.separator + file.getName();
			
			FileSaver fs = new FileSaver(img);
			broadcast("Saving channel " + (i+1) 
								+ " of image " + file.getName());
			fs.saveAsTiffStack(savepath);
		}
		busy = false;
		reportState(busy);
		broadcast("Finished reading filegroup");
		return ret;
	}
}
