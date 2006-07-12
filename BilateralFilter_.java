import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import ij.plugin.filter.PlugInFilter;

public class BilateralFilter_ implements PlugInFilter {
	ImagePlus image;

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Bilateral Parameters");
		gd.addNumericField("spatial radius", 3, 0);
		gd.addNumericField("range radius", 50, 0);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		final InterpolatedImage orig = new InterpolatedImage(image);
		InterpolatedImage res = orig.cloneDimensionsOnly();
		final float[] spatial = makeKernel(gd.getNextNumber());
		final float[] range = makeKernel(gd.getNextNumber());

		InterpolatedImage.Iterator iter = res.iterator(true);
		InterpolatedImage o = orig;
		float[] s = spatial;
		int sc = spatial.length / 2;
		float[] r = range;
		int rc = range.length / 2;

		while (iter.next() != null) {
			int v0 = o.getNoInterpol(iter.i, iter.j, iter.k);
			float v = 0, total = 0;
			for (int n = 0; n < s.length; n++)
				for (int m = 0; m < s.length; m++) {
					int v1 = o.getNoInterpol(
							iter.i + m - sc,
							iter.j + n - sc,
							iter.k);
					if (Math.abs(v1 - v0) > rc)
						continue;
					float w = s[m] * s[n]
						* r[v1 - v0 + rc];
					v += v1 * w;
					total += w;
				}
			res.set(iter.i, iter.j, iter.k, (int)(v / total));
		}

		res.image.show();
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G;
	}

	public static float[] makeKernel(double radius) {
		radius += 1;
		int size = (int)radius*2-1;
		float[] kernel = new float[size];
		float total = 0;
		for (int i=0; i<size; i++) {
			double v = (i + 1 - radius) / (radius * 2) / 0.2;
			kernel[i] = (float)Math.exp(-0.5 * v * v);
			total += v;
		}
		if (total <= 0.0)
			for (int i = 0; i < size; i++)
				kernel[i] = 1.0f / size;
		else if (total != 1.0)
			for (int i = 0; i < size; i++)
				kernel[i] /= total;
		return kernel;
	}
}
