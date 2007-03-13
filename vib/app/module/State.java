package vib.app.module;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;

import java.io.File;

import vib.app.ImageMetaData;
import vib.app.Options;

import vib.FloatMatrix;

/*
 * Convention: index < 0 means template, channel < 0 means labels
 */

public class State {
        public Options options;

	public State(Options options) {
		this.options = options;

		imagesPath = options.workingDirectory + "/images";
		labelPath = options.workingDirectory + "/labels";
		mkdir(labelPath);
		if (options.resamplingFactor > 1)
			resampledPath = options.workingDirectory +
				"/resampled";
		else
			resampledPath = null;
		if (options.transformationMethod == Options.LABEL_DIFFUSION)
			warpedPath = options.workingDirectory + "/warped";
		statisticsPath = options.workingDirectory + "/statistics";
		mkdir(statisticsPath);
		for (int c = -1; c < options.numChannels; c++) {
			if (warpedPath != null)
				mkdir(warpedPath + getChannelName(c));
			if (c >= 0 && options.numChannels > 1)
				mkdir(imagesPath + getChannelName(c));
			if (resampledPath != null)
				mkdir(resampledPath + getChannelName(c));
		}

		int imageCount = options.fileGroup.size();

		channels = new String[options.numChannels][imageCount];

		for (int i = 0; i < imageCount; i++) {
			File file = (File)options.fileGroup.get(i);
			String baseName = file.getName();
			for (int j = 0; j < options.numChannels; j++)
				// TODO: how to determine 2nd channel's path?
				channels[j][i] = file.getAbsolutePath();
		}
	}

	private String[][] channels;
	private String imagesPath;
	private String labelPath;
	private String resampledPath;
	private String warpedPath;
	private String statisticsPath;
        private String currentImagePath;
        private ImagePlus currentImage;
        private ImagePlus templateLabels;

	public String getBaseName(int index) {
		if (index < 0)
			return getTemplateBaseName();
		return getBaseName(channels[0][index]);
	}

	public String getTemplateBaseName() {
		return getBaseName(options.templatePath);
	}

	public static String getBaseName(String fileName) {
		int slash = fileName.lastIndexOf('/');
		if (slash >= 0)
			fileName = fileName.substring(slash + 1);
		int dot = fileName.lastIndexOf('.');
		if (dot >= 0)
			fileName = fileName.substring(0, dot);
		return fileName;
	}

	public String getChannelName(int channel) {
		return channel < 0 ? "_labels" :
			options.numChannels < 2 ? "" : "_" + (channel + 1);
	}

	public String getImagePath(int channel, int index) {
		if (channel < 0)
			// labels
			return labelPath + "/" +
				getBaseName(index) + ".labels";
		if (index < 0)
			// template
			return options.templatePath;
		if (options.numChannels < 2)
			return channels[channel][index];
		return imagesPath + getChannelName(channel) + "/"
			+ getBaseName(index) + ".tif";
	}

	public String getResampledPath(int channel, int index) {
		if (options.resamplingFactor == 1)
			return getImagePath(channel, index);
		return resampledPath + getChannelName(channel) + "/" +
			getBaseName(index) + ".tif";
	}

	/*
	 * no getLabelPath(int index), as the label path can be reset
	 * file by file by the Resample module.
	 */

	public String getWarpedPath(int channel, int index) {
		if (warpedPath == null)
			return getResampledPath(channel, index);
		return warpedPath + getChannelName(channel) + "/"
			+ getBaseName(index) + ".warped";
	}

	public String getWarpedPath(int channel) {
		return warpedPath + getChannelName(channel) + "/"
			+ getTemplateBaseName() + ".warped";
	}

	public String getStatisticsPath() {
		return statisticsPath;
	}

	public String getStatisticsPath(int index) {
		return statisticsPath + "/"
			+ getBaseName(index) + ".statisticss";
	}

	public ImageMetaData getStatistics(int index) {
		try {
			return new ImageMetaData(getStatisticsPath(index));
		} catch (Exception e) {
			return null;
		}
	}

	public String getTransformLabel() {
		return getTemplateBaseName() +
			options.TRANSFORM_LABELS[options.transformationMethod];
	}

	/*
	 * TODO:
	 * it is fatal for dependency checking to have all transforms in the
	 * same statistics file. Also, we really should get rid of the
	 * AmiraTable mess: make the files csv files instead.
	 */

	public FloatMatrix getTransformMatrix(int index) {
		ImageMetaData metaData = getStatistics(index);
		FloatMatrix matrix = metaData.getMatrix(getTransformLabel());
		return matrix != null ? matrix : new FloatMatrix(1.0f);
	}

	public int getImageCount() {
		return channels[0].length;
	}

	public static boolean upToDate(String[] sources, String target) {
		File output = new File(target);
		if (!output.exists())
			return false;
		for (int i = 0; i < sources.length; i++) {
			File source = new File(sources[i]);
			if (!source.exists())
				continue;
			try {
				if (source.lastModified() >
						output.lastModified())
					return false;
			} catch (Exception e) {
				// ignore unreadable file
			}
		}
		return true;
	}

	public static boolean upToDate(String source, String target) {
		return upToDate(new String[] { source }, target);
	}

	public boolean save(ImagePlus image, String path) {
		currentImagePath = path;
		currentImage = image;
		FileSaver fs = new FileSaver(image);
		return fs.saveAsTiffStack(path);
	}

	// caching the latest image
        public ImagePlus getImage(String path) {
                if (!path.equals(currentImagePath)) {
			File f = new File(path);
			if (!f.exists())
				return null;
			// give the garbage collector a chance
			currentImage = null;

			currentImagePath = path;
			currentImage = IJ.openImage(currentImagePath);
		}
                return currentImage;
        }

        public ImagePlus getTemplateLabels() {
                if (templateLabels == null)
			templateLabels =
				IJ.openImage(getResampledPath(-1, -1));
		// TODO: check if the dimensions are really borked
                return templateLabels;
        }

	private void mkdir(String path) {
		new File(path).mkdir();
	}
}

