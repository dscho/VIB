import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import distance.PixelPairs;
import java.util.StringTokenizer;
import math3d.Point3d;

public class TransformedImage {
	InterpolatedImage orig, transform;
	FastMatrix fromOrig;
	private FastMatrix transformation;
	FastMatrix toTransform;

	public PixelPairs measure;

	/*
	 * This matrix contains the first 3 rows of the uniform matrix which
	 * transforms pixel coordinates in the original onto pixel
	 * coordinates in the "transformed" stack.
	 */
	FastMatrix matrix;

	public TransformedImage(ImagePlus orig, ImagePlus transform) {
		this.orig = new InterpolatedImage(orig);
		this.transform = new InterpolatedImage(transform);

		fromOrig = FastMatrix.fromCalibration(orig);
		toTransform = FastMatrix.fromCalibration(transform).inverse();

		x0 = y0 = z0 = 0;
		x1 = this.orig.w; y1 = this.orig.h; z1 = this.orig.d;
	}

	public void setTransformation(FastMatrix matrix) {
		transformation = matrix.inverse();
		this.matrix = toTransform.times(transformation.times(fromOrig));
	}

	public TransformedImage resample(int factor) {
		ImagePlus resOrig, resTran;
		
		if (measure instanceof distance.MutualInformation) {
			resOrig = Resample_.resampleMinEnt(orig.image, factor);
			resTran = Resample_.resampleMinEnt(transform.image,
					factor);
		} else {
			resOrig = Resample_.resample(orig.image, factor);
			resTran = Resample_.resample(transform.image, factor);
		}
		TransformedImage res = new TransformedImage(resOrig, resTran);
		res.transformation = transformation;
		res.measure = measure;
		res.x0 = x0 / factor;
		res.y0 = y0 / factor;
		res.z0 = z0 / factor;
		res.x1 = (x1 + factor - 1) / factor;
		res.y1 = (y1 + factor - 1) / factor;
		res.z1 = (z1 + factor - 1) / factor;
		return res;
	}

	public static abstract class Iterator {
		TransformedImage t;

		boolean showProgress;

		public Iterator(TransformedImage trans) {
			this(trans, false);
		}

		public Iterator(TransformedImage trans, boolean showProgress) {
			t = trans;
			this.showProgress = showProgress;
		}

		// these are the original coordinates
		int i, j, k;
		// these are the transformed coordinates
		double x, y, z;

		// implement this!
		abstract void step();

		void doit() {
			doit(0, 0, 0, t.orig.w, t.orig.h, t.orig.d);
		}

		void doit(int x0, int y0, int z0, int x1, int y1, int z1) {
			int zd = z1 - z0;
			if (t.matrix.isIdentity()) {
				for (k = z0; k < z1; k++) {
					for (j = y0; j < y1; j++) {
						for (i = x0; i < x1; i++) {
							x = i;
							y = j;
							z = k;
							step();
						}
					}
					if (showProgress)
						IJ.showProgress(k - z0 + 1, zd);
				}
				return;
			}

			int xd = x1 - x0;
			for (k = z0; k < z1; k++) {
				for (j = y0; j < y1; j++) {
					t.matrix.apply(0, j, k);
					Point3d start = t.matrix.getResult();
					t.matrix.apply(x1, j, k);
					Point3d stop = t.matrix.getResult()
						.minus(start);
					for (i = x0; i < x1; i++) {
						x = start.x + stop.x * i / x1;
						y = start.y + stop.y * i / x1;
						z = start.z + stop.z * i / x1;
						step();
					}
				}
				if (showProgress)
					IJ.showProgress(k - z0 + 1, zd);
			}
		}
	}

	// the bounding box for the distance calculation
	int x0, y0, z0, x1, y1, z1;

	public float getDistance() {
		Iterator iter = new Iterator(this) {
			void step() {
				float v1 = orig.getNoInterpol(i, j, k);
				float v2 = (float)transform.interpol.get(x, y, z);
				measure.add(v1, v2);
			}
		};

		measure.reset();
		iter.doit(x0, y0, z0, x1, y1, z1);
		return measure.distance();
	}

	private static class GetTransformed extends Iterator {
		public GetTransformed(TransformedImage t) {
			super(t, true);
		}

		InterpolatedImage result = t.orig.cloneDimensionsOnly();

		void step() {
			result.set(i, j, k,
					t.transform.getNoInterpol((int)x,
						(int)y, (int)z));
		}
	}

	public ImagePlus getTransformed() {
		GetTransformed iter = new GetTransformed(this);
		iter.doit();
		iter.result.image.setTitle("transformed");
		return iter.result.image;
	}

	private static class GetDifferenceImage extends Iterator {
		public GetDifferenceImage(TransformedImage t) {
			super(t, true);
		}

		InterpolatedImage result =
			InterpolatedImage.cloneDimensionsOnly(t.orig.image,
					//ImagePlus.GRAY32);
					ImagePlus.GRAY8);

		void step() {
			t.measure.reset();
			t.measure.add(t.orig.getNoInterpol(i, j, k),
					t.transform.getNoInterpol((int)x,
						(int)y, (int)z));
			//result.setFloat(i, j, k, measure.distance());
			result.set(i, j, k, (byte)(int)t.measure.distance());
		}
	}

	public ImagePlus getDifferenceImage() {
		GetDifferenceImage iter = new GetDifferenceImage(this);
		iter.doit(x0, y0, z0, x1, y1, z1);
		iter.result.image.setTitle("difference");
		return iter.result.image;
	}

	public void narrowBBox(int x0, int x1, int y0, int y1, int z0, int z1) {
System.err.println("bbox: " + x0 + " " + x1 + " " + y0 + " " + y1 + " " + z0 + " " + z1);
		if (x0 < 0)
			x0 = 0;
		else if (x0 >= orig.w)
			x0 = orig.w - 1;
		if (y0 < 0)
			y0 = 0;
		else if (y0 >= orig.h)
			y0 = orig.h - 1;
		if (z0 < 0)
			z0 = 0;
		else if (z0 >= orig.d)
			z0 = orig.d - 1;
		if (x1 < 1)
			x1 = 1;
		else if (x1 > orig.w)
			x1 = orig.w;
		if (y1 < 1)
			y1 = 1;
		else if (y1 > orig.h)
			y1 = orig.h;
		if (z1 < 1)
			z1 = 1;
		else if (z1 > orig.d)
			z1 = orig.d;
		this.x0 = x0;
		this.x1 = x1;
		this.y0 = y0;
		this.y1 = y1;
		this.z0 = z0;
		this.z1 = z1;
	}

	public void narrowSearchToMaterial(int value, int border) {
		x0 = orig.w; y0 = orig.h; z0 = orig.d;
		x1 = y1 = z1 = 0;
		for (int k = 0; k < orig.d; k++)
			for (int j = 0; j < orig.h; j++)
				for (int i = 0; i < orig.w; i++) {
					int v = orig.getNoInterpol(i, j, k);
					if (v != value)
						continue;
					if (x0 > i) x0 = i;
					else if (x1 < i) x1 = i;
					if (y0 > j) y0 = j;
					else if (y1 < j) y1 = j;
					if (z0 > k) z0 = k;
					else if (z1 < k) z1 = k;
				}
		x0 -= border; y0 -= border; z0 -= border;
		x1 += border + 1; y1 += border + 1; z1 += border + 1;
		if (x0 < 0) x0 = 0;
		if (y0 < 0) y0 = 0;
		if (z0 < 0) z0 = 0;
		if (x1 > orig.w) x1 = orig.w;
		if (y1 > orig.h) y1 = orig.h;
		if (z1 > orig.d) z1 = orig.d;
	}

	/* package methods */
	int getOrig(int x, int y, int z) {
		return orig.getNoCheck(x, y, z);
	}

	float getTransformed(double x, double y, double z) {
		matrix.apply(x, y, z);
		return (float)transform.interpol.get(
				matrix.x, matrix.y, matrix.z);
	}

	float getTransformedNoInterpol(double x, double y, double z) {
		matrix.apply(x, y, z);
		return (float)transform.getNoInterpol(
				(int)Math.round(matrix.x),
				(int)Math.round(matrix.y),
				(int)Math.round(matrix.z));
	}
}
