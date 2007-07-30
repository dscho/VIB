import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;

import java.awt.Rectangle;
import java.util.ArrayList;

public class Average_Color implements PlugInFilter {
	protected ImagePlus image;
	protected int w, h;
	protected int[] pixels;

	public void test() {
		GenericDialog gd = new GenericDialog("L*");
		gd.addNumericField("L*", 75, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		float l = (float)gd.getNextNumber();

		float[] lab = new float[3];
		float[] rgb = new float[3];
		int[] rgbi = new int[3];

		w = h = 128 * 2 + 1;
		pixels = new int[w * h];
		for (int a = -128; a <= 128; a++)
			for (int b = -128; b <=  128; b++) {
				lab[0] = l;
				lab[1] = a;
				lab[2] = b;
				CIELAB2sRGB(lab, rgb);
				rgbi[0] = unnorm(rgb[0]);
				rgbi[1] = unnorm(rgb[1]);
				rgbi[2] = unnorm(rgb[2]);
				pixels[a + 128 + (b + 128) * w] =
					(rgbi[0] << 16) | (rgbi[1] << 8) |
					rgbi[2];
			}

		ColorProcessor p = new ColorProcessor(w, h, pixels);
		new ImagePlus("Colors", p).show();
	}

	public void run(ImageProcessor ip) {
		Roi roi = image.getRoi();
		if (roi == null) {
			IJ.error("Need a ROI");
			return;
		}
		boolean haveShapeRoi = (roi instanceof ShapeRoi);

		GenericDialog gd = new GenericDialog("Average Color");
		gd.addCheckbox("CIELab averaging", true);
		if (haveShapeRoi)
			gd.addCheckbox("Split roi", true);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		boolean cielab = gd.getNextBoolean();
		boolean splitRoi = haveShapeRoi ? gd.getNextBoolean() : false;

		w = image.getWidth();
		h = image.getHeight();
		pixels = (int[])image.getProcessor().getPixels();

		if (splitRoi) {
			ShapeRoi shape = (ShapeRoi)roi;
			Roi[] rois = shape.getRois();
			for (int i = 0; i < rois.length; i++)
				averageColorInRoi(rois[i], cielab);
		} else
			averageColorInRoi(roi, cielab);
		image.updateAndDraw();
	}

	/* White point D65, CIE 1964 */
	final static float luminance = 0.54f;
	final static float xn = 0.31382f, yn = 0.33100f;
	final static float Yn = luminance, Xn = Yn * xn / yn,
		     Zn = Yn * (1 - xn - yn) / yn;
	static float[] tmp = new float[3];

	private final static float power3(float t) {
		if (t > 0.008856f)
			return (float)Math.exp(Math.log(t) * 3);
		return (t - 16f / 116) / 7.787f;
	}

	private final static float root3(float t) {
		if (t > power3(0.008856f))
			return (float)Math.exp(Math.log(t) / 3);
		return 7.787f * t + 16.0f / 116;
	}

	private final static void XYZ2CIELAB(float[] xyz, float[] lab) {
		lab[0] = 116 * root3(xyz[1] / Yn) - 16;
		lab[1] = 500 * (root3(xyz[0] / Xn) - root3(xyz[1] / Yn));
		lab[2] = 200 * (root3(xyz[1] / Yn) - root3(xyz[2] / Zn));
	}

	private final static void CIELAB2XYZ(float[] lab, float[] xyz) {
		float fy = (lab[0] + 16) / 116;
		xyz[0] = Xn * power3(lab[1] / 500 + fy);
		xyz[1] = Yn * power3(fy);
		xyz[2] = Zn * power3(fy - lab[2] / 200);
	}

	private final static float linear2gamma(float t) {
		return t <= 0.0031308 ? 12.92f * t :
			(float)(1.055 * Math.exp(Math.log(t) / 2.4) - 0.055);
	}

	private final static float gamma2linear(float t) {
		return t <= 0.04045 ? t / 12.92f : (float)
			Math.exp(Math.log((t + 0.055) / 1.055) * 2.4);
	}

	private final static void sRGB2XYZ(float[] rgb, float[] xyz) {
		rgb[0] = gamma2linear(rgb[0]);
		rgb[1] = gamma2linear(rgb[1]);
		rgb[2] = gamma2linear(rgb[2]);
		xyz[0] = 0.4124f * rgb[0] + 0.3576f * rgb[1] + 0.1805f * rgb[2];
		xyz[1] = 0.2126f * rgb[0] + 0.7152f * rgb[1] + 0.0722f * rgb[2];
		xyz[2] = 0.0193f * rgb[0] + 0.1192f * rgb[1] + 0.9505f * rgb[2];
	}

	private final static void XYZ2sRGB(float[] xyz, float[] rgb) {
		rgb[0] = 3.2410f * xyz[0] - 1.5374f * xyz[1] - 0.4986f * xyz[2];
		rgb[1] =-0.9692f * xyz[0] + 1.8760f * xyz[1] + 0.0416f * xyz[2];
		rgb[2] = 0.0556f * xyz[0] - 0.2040f * xyz[1] + 1.0570f * xyz[2];
		rgb[0] = linear2gamma(rgb[0]);
		rgb[1] = linear2gamma(rgb[1]);
		rgb[2] = linear2gamma(rgb[2]);
	}

	private final static void CIELAB2sRGB(float[] lab, float[] rgb) {
		float[] xyz = new float[3];
		CIELAB2XYZ(lab, xyz);
		XYZ2sRGB(xyz, rgb);
	}

	private final static void sRGB2CIELAB(float[] rgb, float[] lab) {
		float[] xyz = new float[3];
		sRGB2XYZ(rgb, xyz);
		XYZ2CIELAB(xyz, lab);
	}

	private final static float norm(float f) {
		return f / 255f;
	}

	private final static int unnorm(float f) {
		return f < 0 ? 0 : f > 1 ? 255 : (int)Math.round(f * 255);
	}

	private void averageColorInRoi(Roi roi, boolean cielab) {
		Rectangle r = roi.getBounds();
		float[] rgb = new float[3];
		float[] lab = new float[3];
		float[] cumul = new float[3];
		int count = 0;

		/* first get the cumulated values in the given color space */
		for (int y = r.y; y < r.y + r.height; y++)
			for (int x = r.x; x < r.x + r.width; x++) {
				if (!roi.contains(x, y))
					continue;
				int v = pixels[x + w * y];
				rgb[0] = norm((v >> 16) & 0xff);
				rgb[1] = norm((v >> 8) & 0xff);
				rgb[2] = norm(v & 0xff);
				if (cielab) {
					sRGB2CIELAB(rgb, lab);
					cumul[0] += lab[0];
					cumul[1] += lab[1];
					cumul[2] += lab[2];
				} else {
					cumul[0] += rgb[0];
					cumul[1] += rgb[1];
					cumul[2] += rgb[2];
				}
				count++;
			}

		/* then make the average... */
		cumul[0] /= count;
		cumul[1] /= count;
		cumul[2] /= count;

		int[] rgbi = new int[3];
		if (cielab) {
			CIELAB2sRGB(cumul, rgb);
			rgbi[0] = unnorm(rgb[0]);
			rgbi[1] = unnorm(rgb[1]);
			rgbi[2] = unnorm(rgb[2]);
		} else {
			rgbi[0] = unnorm(cumul[0]);
			rgbi[1] = unnorm(cumul[1]);
			rgbi[2] = unnorm(cumul[2]);
		}

		int v = (rgbi[0] << 16) | (rgbi[1] << 8) | rgbi[2];

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
