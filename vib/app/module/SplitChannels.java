package vib.app.module;

import ij.ImagePlus;

import java.io.File;

import leica.Leica_SP_Reader;

public class SplitChannels extends Module {
	protected String getName() { return "SplitChannels"; }
	protected String getMessage() { return "Splitting channels"; }

	protected void run(State state, int index) {
		int numChannels = state.options.numChannels;
		if (numChannels < 2 || index < 0)
			return;

		File file = (File)state.options.fileGroup.get(index);
		String path = file.getAbsolutePath();
		Leica_SP_Reader reader = new Leica_SP_Reader();
		reader.run(path);
		if(reader.getNumberOfChannels() < numChannels) {
			console.append("File " + path + " does not contain " +
					numChannels + " channels, but " +
					reader.getNumberOfChannels());
			throw new RuntimeException();
		}
		// save reference channel last, to avoid unnecessary loading
		int refChannel = state.options.refChannel - 1;
		for(int i = 0; i < numChannels; i++) {
			if (i == refChannel)
				continue;
			ImagePlus img = reader.getImage(i);
			state.save(img, state.getImagePath(i, index));
		}
		state.save(reader.getImage(refChannel),
				state.getImagePath(refChannel, index));
	}
}

