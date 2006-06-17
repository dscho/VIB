/*
 * This plugin is based on a proposal by Tom Larkworthy.
 * Written and public domained in June 2006 by Johannes E. Schindelin
 */
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Threshold_to_Selection2 implements PlugInFilter {
	ImagePlus image;
	ImageProcessor ip;
	double min, max;
	int w, h;

	public void run(ImageProcessor ip) {
long t = System.currentTimeMillis();
		this.ip = ip;
		min = ip.getMinThreshold();
		max = ip.getMaxThreshold();

		w = ip.getWidth();
		h = ip.getHeight();

		image.setRoi(getRoi(0, h));
System.err.println("took " + ((System.currentTimeMillis() - t) / 1000.0));
	}

	final boolean selected(int x, int y) {
		float v = ip.getPixelValue(x, y);
		return v >= min && v <= max;
	}

	public Roi getRoi(int y0, int y1) {
		return getShapeRoi(y0, y1);
	}

	public ShapeRoi getShapeRoi(int y0, int y1) {
		if (y0 + 1 == y1)
			return getShapeRoi(y0);
		else {
			int m = (y0 + y1) / 2;
			ShapeRoi r1 = getShapeRoi(y0, m);
			ShapeRoi r2 = getShapeRoi(m, y1);
			if (r1 == null)
				return r2;
			if (r2 == null)
				return r1;
			return r1.or(r2);
		}
	}

	public ShapeRoi getShapeRoi(int y) {
		ShapeRoi r = null;
		for (int x = 0; x < w; x++)
			if (selected(x, y)) {
				int x1;
				for (x1 = x + 1; x1 < w &&
						selected(x1, y); x1++);
				ShapeRoi r1 = new ShapeRoi(
						new Roi(x, y, x1 - x, 1));
				if (r == null)
					r = r1;
				else
					r = r.or(r1);
				x = x1 + 1;
			}
		IJ.showProgress(y + 1, h);
		return r;
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_16 | DOES_32 | NO_CHANGES;
	}
}

