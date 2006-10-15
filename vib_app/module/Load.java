package vib_app.module;

import vib_app.VIB_Leica_Reader;
import vib_app.App;

import ij.ImagePlus;
import ij.io.FileSaver;

import java.io.File;
import java.io.IOException;

public class Load extends Module {

	private File file;
	private int numChannels;
	
	
	public Load(File file, int numChannels) {
		this.file = file;
		this.numChannels = numChannels;
	}
	
	public Module.Error checkDependency() {
		if(!App.instance().getOptions().isValid()) {
			return new Module.Error(Module.DATA_MISSING, 
					"The current project contains invalid data");
		}
		return new Module.Error(Module.DEPENDENCY_FINE, "");
	}

	public ImagePlus execute() {
		boolean busy = true;
		reportState(busy);
		int refChannel = App.instance().getOptions().getRefChannel();
		VIB_Leica_Reader reader = new VIB_Leica_Reader();
		ImagePlus ret = null;
		for(int i = 0; i < numChannels; i++) {
			try {
				if(i == 0)
					broadcast("Reading " + numChannels + " channels of image " 
											+ file.getName());
				ImagePlus img = reader.getImage(
					file.getParent() + File.separator, file.getName(), i);
				
				if(i+1 == refChannel)
					ret = img;
				String dirS = App.instance().getOptions().getWorkingDirectory()
									+ File.separator
									+ "images-" + (i+1) + File.separator;
				File dir = new File(dirS);
				if(!dir.exists()) 
					dir.mkdir();
				
				String path = dirS + File.separator + file.getName();
				
				FileSaver fs = new FileSaver(img);
				broadcast("Saving channel " + (i+1) 
									+ " of image " + file.getName());
				fs.saveAsTiffStack(path);
			} catch (IOException e) {
				busy = false;
				reportState(busy);
				throw new RuntimeException("Cannot load file " + file + ": "
								+ e.getMessage());
			}
		}
		busy = false;
		reportState(busy);
		broadcast("Finished reading filegroup");
		return ret;
	}
}
