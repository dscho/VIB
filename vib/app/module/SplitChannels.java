package vib.app.module;

import ij.ImagePlus;

import java.io.File;

import leica.Leica_SP_Reader;

public class SplitChannels extends Module {
	protected String getName() { return "SplitChannels"; }
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

		Leica_SP_Reader reader = new Leica_SP_Reader();
		reader.run(path);
		if(reader.getNumberOfChannels() < numChannels) {
			if (index < 0 && reader.getNumberOfChannels() == 1) {
				// be graceful when the template has only one channel
				path = state.getImagePath(refChannel, index);
				if(!state.save(reader.getImage(0), path))
					console.append(
						"Could not save " + path);;
				return;
			}
			console.append("File " + path + " does not contain " +
					numChannels + " channels, but " +
					reader.getNumberOfChannels());
			throw new RuntimeException();
		}
		// save reference channel last, to avoid unnecessary loading
		for(int i = 0; i < numChannels; i++) {
			if (i == refChannel)
				continue;
			ImagePlus img = reader.getImage(i);
			path = state.getImagePath(i, index);
			if(!state.save(img, path))
				console.append("Could not save " + path);
			new File(path).setLastModified(file.lastModified());
		}
		path = state.getImagePath(refChannel, index);
		if(!state.save(reader.getImage(refChannel), path))
			console.append("Could not save " + path);
		new File(path).setLastModified(file.lastModified());
	}
}

