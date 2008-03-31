package org.imagearchive.lsm.toolbox;

import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.MedianCut;
import ij.process.ShortProcessor;

import java.awt.Image;
import java.io.File;

import org.imagearchive.lsm.toolbox.info.CZ_LSMInfo;
import org.imagearchive.lsm.toolbox.info.ImageDirectory;
import org.imagearchive.lsm.toolbox.info.LsmFileInfo;

/*******************************************************************************
 * Batch Converter Class - Adapted from Wayne Rasband's Batch Converter plug-in. *
 ******************************************************************************/

public class BatchConverter {

	private MasterModel masterModel;

	public BatchConverter(MasterModel masterModel) {
		super();
		this.masterModel = masterModel;
	}

	public void convertFile(String file, String outputDir, String format,
			boolean verbose, boolean sepDir) {
		String finalDir = "";
		File f = new File(file);

		ImagePlus[] impTab = new Reader(masterModel).open(f.getParent(), f
				.getName(), false, verbose, false);

		if (impTab != null && impTab.length > 0) {
			LsmFileInfo lsm = (LsmFileInfo) impTab[0].getOriginalFileInfo();
			CZ_LSMInfo cz = ((ImageDirectory) lsm.imageDirectories.get(0)).TIF_CZ_LSMINFO;

			for (int i = 0; i < impTab.length; i++) {
				if (sepDir) {
					finalDir = outputDir + System.getProperty("file.separator")
							+ f.getName();
					File fdir = new File(finalDir);
					if (!fdir.exists())
						fdir.mkdirs();
				} else
					finalDir = outputDir;

				if (impTab[i].getImageStackSize() > 1)
					for (int j = 1; j <= impTab[i].getImageStackSize(); j++) {
						String title = lsm.fileName+" - "+cz.channelNamesAndColors.ChannelNames[i]
								+ " - " + new Integer(j).toString();
						save(new ImagePlus(title, impTab[i].getImageStack()
								.getProcessor(j)), finalDir, format, title);
					}
				else {
						String title = lsm.fileName+" - "+cz.channelNamesAndColors.ChannelNames[i];
						save(new ImagePlus(title, impTab[i].getImageStack().getProcessor(1)), finalDir, format, title);
				}
			}
		}
	}

	/***************************************************************************
	 * method : process, optional method to add some image processing before
	 * conversion *
	 **************************************************************************/

	/**
	 * This is the place to add code to process each image. The image is not
	 * written if this method returns null.
	 */
	public ImagePlus process(ImagePlus imp) {
		/* No processing defined for this plugin */
		return imp;
	}

	/***************************************************************************
	 * method : save, saves the image with an appropriate file name *
	 **************************************************************************/

	public void save(ImagePlus img, String dir, String format, String fileName) {
		String path = dir + System.getProperty("file.separator") + fileName;
		if (format.equals("Tiff"))
			new FileSaver(img).saveAsTiff(path + ".tif");
		else if (format.equals("8-bit Tiff"))
			saveAs8bitTiff(img, path + ".tif");
		else if (format.equals("Zip"))
			new FileSaver(img).saveAsZip(path + ".zip");
		else if (format.equals("Raw"))
			new FileSaver(img).saveAsRaw(path + ".raw");
		else if (format.equals("Jpeg"))
			new FileSaver(img).saveAsJpeg(path + ".jpg");
	}

	/***************************************************************************
	 * method : saveAs8bitTiff, image processing for 8-bit Tiff saving *
	 **************************************************************************/

	void saveAs8bitTiff(ImagePlus img, String path) {
		ImageProcessor ip = img.getProcessor();
		if (ip instanceof ColorProcessor) {
			ip = reduceColors(ip);
			img.setProcessor(null, ip);
		} else if ((ip instanceof ShortProcessor)
				|| (ip instanceof FloatProcessor)) {
			ip = ip.convertToByte(true);
			img.setProcessor(null, ip);
		}
		new FileSaver(img).saveAsTiff(path);
	}

	/***************************************************************************
	 * method : reduceColors, reduces the color range for the appropriate format *
	 **************************************************************************/

	ImageProcessor reduceColors(ImageProcessor ip) {
		MedianCut mc = new MedianCut((int[]) ip.getPixels(), ip.getWidth(), ip
				.getHeight());
		Image img = mc.convert(256);
		return (new ByteProcessor(img));
	}

}
