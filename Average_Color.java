import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.util.ArrayList;

public class Average_Color implements PlugInFilter {
	protected ImagePlus image;
	protected int w, h;
	protected int[] pixels;
	protected static String[] colorSpaces = {
		"CIEXYZ", "GRAY", "LINEAR_RGB", "PYCC"
	};
	protected static int[] colorSpaceIndices = {
		ColorSpace.CS_CIEXYZ, ColorSpace.CS_GRAY,
		ColorSpace.CS_LINEAR_RGB, ColorSpace.CS_PYCC
	};

	public void run(ImageProcessor ip) {
		Roi roi = image.getRoi();
		if (roi == null) {
			IJ.error("Need a ROI");
			return;
		}
		boolean haveShapeRoi = (roi instanceof ShapeRoi);

		GenericDialog gd = new GenericDialog("Average Color");
		gd.addChoice("Color space", colorSpaces, colorSpaces[0]);
		if (haveShapeRoi)
			gd.addCheckbox("Split roi", true);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		int colorSpaceIndex =
			colorSpaceIndices[gd.getNextChoiceIndex()];
		boolean splitRoi = haveShapeRoi ? gd.getNextBoolean() : false;

		ColorSpace colorSpace = ICC_ColorSpace.getInstance(colorSpaceIndex);
		w = image.getWidth();
		h = image.getHeight();
		pixels = (int[])image.getProcessor().getPixels();

		if (splitRoi) {
			ShapeRoi shape = (ShapeRoi)roi;
			Roi[] rois = shape.getRois();
			for (int i = 0; i < rois.length; i++)
				averageColorInRoi(rois[i], colorSpace);
		} else
			averageColorInRoi(roi, colorSpace);
		image.updateAndDraw();
	}

	private void averageColorInRoi(Roi roi, ColorSpace cs) {
		Rectangle r = roi.getBounds();
		float[] rgb = new float[3];
		float[] cumul = new float[cs.getNumComponents()];
		int count = 0;

		/* first get the cumulated values in the given color space */
		for (int y = r.y; y < r.y + r.height; y++)
			for (int x = r.x; x < r.x + r.width; x++) {
				if (!roi.contains(x, y))
					continue;
				int v = pixels[x + w * y];
				rgb[0] = ((v >> 16) & 0xff) / 255.0f;
				rgb[1] = ((v >> 8) & 0xff) / 255.0f;
				rgb[2] = (v & 0xff) / 255.0f;
				float[] c = cs.fromRGB(rgb);
				for (int i = 0; i < cumul.length; i++)
					cumul[i] += c[i];
				count++;
			}

		/* then make the average... */
		for (int i = 0; i < cumul.length; i++)
			cumul[i] /= count;
		rgb = cs.toRGB(cumul);

		int v = (((int)Math.round(rgb[0] * 255.0f)) << 16)
			| (((int)Math.round(rgb[1] * 255.0f)) << 8)
			| ((int)Math.round(rgb[2] * 255.0f));

		/* and now substitute that */
		for (int y = r.y; y < r.y + r.height; y++)
			for (int x = r.x; x < r.x + r.width; x++)
				if (roi.contains(x, y))
					pixels[x + w * y] = v;
	}

	public int setup(String args, ImagePlus imp) {
		this.image = imp;
		return DOES_RGB;
	}
}
