package vib.app.module;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.StackConverter;

import java.io.File;
import java.awt.image.ColorModel;

import leica.Leica_SP_Reader;

import util.BatchOpener;

public class SplitChannels extends Module {
	public String getName() { return "SplitChannels"; }
	protected String getMessage() { return "Splitting channels"; }

	protected void run(State state, int index) {
		
		prereqsDone(state, index);

		int numChannels = state.options.numChannels;
		if (numChannels < 2)
			return;
		int refChannel = state.options.refChannel - 1;

		File file;
		if (index < 0)
			file = new File(state.options.templatePath);
		else
			file = (File)state.options.fileGroup.get(index);
		String path = file.getAbsolutePath();

		boolean upToDate = true;
		for (int i = 0; i < numChannels; i++)
			if (!state.upToDate(path,
						state.getImagePath(i, index)))
				upToDate = false;

		if (upToDate)
			return;
		
		ImagePlus [] allChannels = BatchOpener.open(path);
		for(int i = 0; i < allChannels.length; i++) {
			new StackConverter(allChannels[i]).convertToGray8();
		}
		
		int channelsInFile = allChannels.length;
		
		if(channelsInFile < numChannels) {
			if (index < 0 && channelsInFile == 1) {
				// be graceful when the template has 
				// only one channel
				path = state.getImagePath(refChannel, index);
				if(!state.save(allChannels[0], path))
					throw new RuntimeException("Could not "
						+ "save " + path);	
				return;
			}
			throw new RuntimeException("Found unexpectedly " 
				+ channelsInFile + " channels " 
				+ " in " + path);
		}
		// save reference channel last, to avoid unnecessary loading
		for(int i = 0; i < numChannels; i++) {
			if (i == refChannel)
				continue;
			ImagePlus img = allChannels[i];
			path = state.getImagePath(i, index);
			if(!state.save(img, path))
				throw new RuntimeException("Could not save " + 
					path);
			new File(path).setLastModified(file.lastModified());
		}
		path = state.getImagePath(refChannel, index);
		if(!state.save(allChannels[refChannel], path))
			throw new RuntimeException("Could not save " + path);
		new File(path).setLastModified(file.lastModified());

	}
}
