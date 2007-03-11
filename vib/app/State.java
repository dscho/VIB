package vib.app;

import amira.AmiraMeshDecoder;
import amira.AmiraTable;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;

import java.io.File;

public class State {
        Options options;

	public State(Options options) {
		this.options = options;

		labelPath = options.workingDirectory + "/labels";
		warpedPath = options.workingDirectory + "/warped";
		warpedLabelsPath = options.workingDirectory + "/warped-labels";
		statisticsPath = options.workingDirectory + "/statistics";

		int imageCount = options.fileGroup.size();

		channels = new String[options.numChannels][imageCount];
		labels = new String[imageCount];

		for (int i = 0; i < imageCount; i++) {
			File file = (File)options.fileGroup.get(i);
			String baseName = file.getName();
			for (int j = 0; j < options.numChannels; j++)
				// TODO: how to determine 2nd channel's path?
				channels[j][i] = file.getAbsolutePath();
			labels[i] = getLabelPath() + "/" +
				baseName + ".labels";
		}
	}

	public String[][] channels;
	public String[] labels;

	private String labelPath;
	private String warpedPath;
	private String warpedLabelsPath;
	private String statisticsPath;
        private String currentImagePath;
        private ImagePlus currentImage;
        private ImagePlus template;

	public String getBaseName(int index) {
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

	public String getLabelPath() {
		return labelPath;
	}

	/*
	 * no getLabelPath(int index), as the label path can be reset
	 * file by file by the Resample module.
	 */

	public String getWarpedPath(int channel) {
		return warpedPath + "_" + channel + "/"
			+ getTemplateBaseName() + ".warped";
	}

	public String getWarpedLabelsPath() {
		return warpedLabelsPath + "/"
			+ getTemplateBaseName() + ".warped";

	}

	public String getStatisticsPath() {
		return statisticsPath;
	}

	public String getStatisticsPath(int index) {
		return statisticsPath + "/"
			+ getBaseName(index) + ".statisticss";
	}

	public AmiraTable getStatistics(int index) {
		AmiraMeshDecoder decoder = new AmiraMeshDecoder();
		if (decoder.open(getStatisticsPath(index)) &&
				decoder.isTable())
			return decoder.getTable();
		return null;
	}

	public String getTransformLabel() {
		return getTemplateBaseName() +
			options.TRANSFORM_LABELS[options.transformationMethod];
	}

	public int getFileCount() {
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

	public boolean save(ImagePlus image, String path) {
		FileSaver fs = new FileSaver(image);
		return fs.saveAsTiffStack(path);
	}

	// caching the latest image
        public ImagePlus getImage(String path) {
                if (!path.equals(currentImagePath)) {
			currentImagePath = path;
			currentImage = IJ.openImage(currentImagePath);
		}
                return currentImage;
        }

        public ImagePlus getTemplate() {
                if (template == null)
                        template = IJ.openImage(options.templatePath);
                return template;
        }
}

