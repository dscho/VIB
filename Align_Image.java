import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class Align_Image implements PlugInFilter {
	ImagePlus image;

	public void run(ImageProcessor ip) {
		Roi roi = image.getRoi();
		if (!(roi instanceof Line)) {
			IJ.error("Need a line selection!");
			return;
		}
		Line line1 = (Line)roi;

		int[] ids = WindowManager.getIDList();
		String[] titles = new String[ids.length];
		int j = 0;
		for (int i = 0; i < ids.length; i++) {
			ImagePlus other = WindowManager.getImage(ids[i]);
			if (other == image)
				continue;
			Roi other_roi = other.getRoi();
			if (!(other_roi instanceof Line))
				continue;
			ids[j] = ids[i];
			titles[j] = other.getTitle();
			j++;
		}
		
		if (j < 1) {
			IJ.error("Need another image with line selection!");
			return;
		}

		int id;
		if (j == 1)
			id = ids[0];
		else {
			if (j < ids.length) {
				String[] temp = new String[j];
				System.arraycopy(titles, 0, temp, 0, j);
				titles = temp;
			}

			GenericDialog gd = new GenericDialog("Align Images");
			gd.addChoice("template", titles, titles[0]);
			gd.showDialog();
			if (gd.wasCanceled())
				return;
			id = ids[gd.getNextChoiceIndex()];
		}

		ImagePlus templ = WindowManager.getImage(id);
		int w = templ.getWidth(), h = templ.getHeight();
		ImageProcessor result = new FloatProcessor(w, h);
		float[] pixels = (float[])result.getPixels();
		Interpolator inter = new BilinearInterpolator(ip);

		Line line2 = (Line)templ.getRoi();

		/* the linear mapping to map line1 onto line2 */
		float a00, a01, a02, a10, a11, a12;
		float dx1 = line1.x2 - line1.x1;
		float dy1 = line1.y2 - line1.y1;
		float dx2 = line2.x2 - line2.x1;
		float dy2 = line2.y2 - line2.y1;
		float det = dx2 * dx2 + dy2 * dy2;
		a00 = (dx2 * dx1 + dy2 * dy1) / det;
		a10 = (dx2 * dy1 - dy2 * dx1) / det;
		a01 = -a10;
		a11 = a00;
		a02 = line1.x1 - a00 * line2.x1 - a01 * line2.y1;
		a12 = line1.y1 - a10 * line2.x1 - a11 * line2.y1;

		for (j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {
				float x = i * a00 + j * a01 + a02;
				float y = i * a10 + j * a11 + a12;
				pixels[i + j * w] = inter.get(x, y);
			}
			IJ.showProgress(j + 1, h);
		}

		result.setMinAndMax(ip.getMin(), ip.getMax());
		ImagePlus res = new ImagePlus("aligned " + image.getTitle(),
			result);
		res.setCalibration(templ.getCalibration());
		res.setRoi(line2);
		res.show();
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_16 | DOES_32 | NO_CHANGES;
	}

	static abstract class Interpolator {
		ImageProcessor ip;
		int w, h;

		public Interpolator(ImageProcessor ip) {
			this.ip = ip;
			w = ip.getWidth();
			h = ip.getHeight();
		}

		public abstract float get(float x, float y);
	}

	static class BilinearInterpolator extends Interpolator {
		public BilinearInterpolator(ImageProcessor ip) {
			super(ip);
		}

		public float get(float x, float y) {
			int i = (int)x;
			int j = (int)y;
			float fx = x - i;
			float fy = y - j;
			float v00 = ip.getPixelValue(i, j);
			float v01 = ip.getPixelValue(i + 1, j);
			float v10 = ip.getPixelValue(i, j + 1);
			float v11 = ip.getPixelValue(i + 1, j + 1);
			return (1 - fx) * (1 - fy) * v00 + fx * (1 - fy) * v01
				+ (1 - fx) * fy * v10 + fx * fy * v11;
		}
	}
}

