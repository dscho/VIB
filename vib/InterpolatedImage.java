package vib;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import math3d.Point3d;

public class InterpolatedImage {
	public ImagePlus image;
	int w,h,d;
	private byte[][] pixels;
	private float[][] pixelsFloat;
	Interpolate interpol;

	public InterpolatedImage(ImagePlus image) {
		this.image = image;
		ImageStack stack = image.getStack();
		d = stack.getSize();
		h = stack.getHeight();
		w = stack.getWidth();

		if (image.getType() == ImagePlus.GRAY8 ||
				image.getType() == ImagePlus.COLOR_256) {
			pixels = new byte[d][];
			for (int i = 0; i < d; i++)
				pixels[i] = (byte[])stack.getPixels(i+1);

			if (image.getType() == ImagePlus.GRAY8 &&
					!image.getProcessor().isColorLut())
				interpol = new AverageByte();
			else
				interpol = new NearestNeighbourByte();
		} else if (image.getType() == ImagePlus.GRAY32) {
			pixelsFloat = new float[d][];
			for (int i = 0; i < d; i++)
				pixelsFloat[i] = (float[])stack.getPixels(i+1);

			interpol = new AverageFloat();
		}
	}

	protected InterpolatedImage() {
	}

	public ImagePlus getImage() {
		return image;
	}

	interface Interpolate {
		double get(double x, double y, double z);
	}

	Point3d getCenter() {
		Calibration calib = image.getCalibration();
		return new Point3d(
				calib.xOrigin + w * calib.pixelWidth / 2.0,
				calib.yOrigin + h * calib.pixelHeight / 2.0,
				calib.zOrigin + d * calib.pixelDepth / 2.0);
	}

	Point3d getCenterOfGravity() {
		return getCenterOfGravity(0, 0, 0, w, h, d);
	}

	Point3d getCenterOfGravity(int x0, int y0, int z0,
			int x1, int y1, int z1) {
		Calibration calib = image.getCalibration();
		long x, y, z, total;

		x = y = z = total = 0;
		for (int k = z0; k < z1; k++)
			for (int j = y0; j < y1; j++)
				for (int i = x0; i < x1; i++) {	
					int val = getNoInterpol(i, j, k);
					x += i * val;
					y += j * val;
					z += k * val;
					total += val;
				}
		return new Point3d(
				calib.xOrigin + calib.pixelWidth * x / total,
				calib.yOrigin + calib.pixelHeight * y / total,
				calib.zOrigin + calib.pixelDepth * z / total);
	}

	/* as getCenterOfGravity(), but count only the pixels with this value */
	Point3d getCenterOfGravity(int value) {
		Calibration calib = image.getCalibration();
		long x, y, z, total;

		x = y = z = total = 0;
		for (int k = 0; k < d; k++)
			for (int j = 0; j < h; j++)
				for (int i = 0; i < w; i++) {	
					int val = getNoInterpol(i, j, k);
					if (val != value)
						continue;
					x += i;
					y += j;
					z += k;
					total++;
				}
		return new Point3d(
				calib.xOrigin + calib.pixelWidth * x / total,
				calib.yOrigin + calib.pixelHeight * y / total,
				calib.zOrigin + calib.pixelDepth * z / total);
	}

	class AverageByte implements Interpolate {
		final public double get(double x, double y, double z) {
			int x1 = (int)Math.floor(x);
			int y1 = (int)Math.floor(y);
			int z1 = (int)Math.floor(z);
			double xR = x1 + 1 - x;
			double yR = y1 + 1 - y;
			double zR = z1 + 1 - z;

			double v000 = getNoInterpol(x1, y1, z1),
			v001 = getNoInterpol(x1, y1, z1 + 1),
			v010 = getNoInterpol(x1, y1 + 1, z1),
			v011 = getNoInterpol(x1, y1 + 1, z1 + 1),
			v100 = getNoInterpol(x1 + 1, y1, z1),
			v101 = getNoInterpol(x1 + 1, y1, z1 + 1),
			v110 = getNoInterpol(x1 + 1, y1 + 1, z1),
			v111 = getNoInterpol(x1 + 1, y1 + 1, z1 + 1);

			double ret = xR * (yR * (zR * v000 + (1 - zR) * v001)
				+ (1 - yR) * (zR * v010 + (1 - zR) * v011))
				+ (1 - xR) * (yR * (zR * v100 + (1 - zR) * v101)
				+ (1 - yR) * (zR * v110 + (1 - zR) * v111));

			return ret;
		}
	}

	/*
	 * This weights the values of the 8 ligands by the inverted distance
	 * and picks the one with the maximum
	 */
	class MaxLikelihoodByte implements Interpolate {
		int[] value = new int[8];
		double[] histo = new double[256];
		double xF, yF, zF;

		public MaxLikelihoodByte(double pixelWidth, double pixelHeight,
				double pixelDepth) {
			xF = pixelWidth;
			yF = pixelHeight;
			zF = pixelDepth;
		}

		double xR, yR, zR;
		final double eps = 1e-10;
		final double factor(int dx, int dy, int dz) {
			double x = (dx == 0 ? xR : 1 - xR);
			double y = (dy == 0 ? yR : 1 - yR);
			double z = (dz == 0 ? zR : 1 - zR);
			return 1.0 / (eps + x * x + y * y + z * z);
		}
			
		final public double get(double x, double y, double z) {
			int x1 = (int)Math.floor(x);
			int y1 = (int)Math.floor(y);
			int z1 = (int)Math.floor(z);
			xR = x1 + 1 - x;
			yR = y1 + 1 - y;
			zR = z1 + 1 - z;

			for (int i = 0; i < 2; i++)
				for (int j = 0; j < 2; j++)
					for (int k = 0; k < 2; k++) {
						int l = i + 2 * (j + 2 * k);
						value[l] = getNoInterpol(x1 + i,
								y1 + j, z1 + k);
						histo[value[l]]++;
						/*histo[value[l]] += factor(i,
								j, k);*/
					}

			int winner = value[0];

			for (int i = 1; i < 8; i++)
				//if (histo[value[i]] >= histo[winner])
				if (value[i] >= winner)
					winner = value[i];

			for (int i = 0; i < 8; i++)
				histo[value[i]] = 0;

			return winner;
		}
	}

	public class NearestNeighbourByte implements Interpolate {
		final public double get(double x, double y, double z) {
			return getInt(x, y, z);
		}

		final public int getInt(double x, double y, double z) {
			double x1 = Math.round(x);
			double y1 = Math.round(y);
			double z1 = Math.round(z);
			return getNoInterpol((int)x1, (int)y1, (int)z1);
		}
	}

	final public int getNoCheck(int x, int y, int z) {
		/* no check; we know exactly that it is inside */
		return pixels[z][x + w * y] & 0xff;
	}

	final public int getNoInterpol(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return 0;
		return getNoCheck(x, y, z);
	}

	final public byte getNearestByte(double x, double y, double z) {
		int i = (int)Math.round(x);
		if (i < 0 || i >= w)
			return 0;
		int j = (int)Math.round(y);
		if (j < 0 || j >= h)
			return 0;
		int k = (int)Math.round(z);
		if (k < 0 || k >= d)
			return 0;
		return pixels[k][i + w * j];
	}

	public void set(int x, int y, int z, int value) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return;
		pixels[z][x + w * y] = (byte)value;
	}

	public class Iterator implements java.util.Iterator {
		// these are the coordinates
		public int i, j, k;

		boolean showProgress = false;

		int x0, x1, y0, y1, z0, z1, xd, zd;

		public Iterator(boolean showProgress, int x0, int y0, int z0,
				int x1, int y1, int z1) {
			this.showProgress = showProgress;
			this.x0 = x0; this.y0 = y0; this.z0 = z0;
			this.x1 = x1; this.y1 = y1; this.z1 = z1;
			xd = x1 - x0; zd = z1 - z0;
			i = x0 - 1; j = y0; k = z0;
		}

		public boolean hasNext() {
			return i + 1 < x1 || j + 1 < y1 || k + 1 < z1;
		}

		public Object next() {
			if (++i >= x1) {
				i = x0;
				if (++j >= y1) {
					j = y0;
					if (++k >= z1)
						return null;
					if (showProgress)
						IJ.showProgress(k - z0 + 1, zd);
				}
			}
			return this;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public Iterator iterator() {
		return iterator(false);
	}

	public Iterator iterator(boolean showProgress) {
		return iterator(showProgress, 0, 0, 0, w, h, d);
	}

	public Iterator iterator(boolean showProgress,
			int x0, int y0, int z0, int x1, int y1, int z1) {
		return new Iterator(showProgress, x0, y0, z0, x1, y1, z1);
	}

	// this is quick'n dirty
	public void drawLine(int x1, int y1, int z1, int x2, int y2, int z2,
			int value) {
		int c1 = Math.abs(x1 - x2);
		int c2 = Math.abs(y1 - y2);
		int c3 = Math.abs(z1 - z2);
		if (c2 > c1)
			c1 = c2;
		if (c3 > c1)
			c1 = c3;
		if (c1 == 0) {
			set(x1, y1, z1, value);
			return;
		}
		for (int i = 0; i <= c1; i++)
			set(x1 + i * (x2 - x1) / c1, y1 + i * (y2 - y1) / c1,
					z1 + i * (z2 - z1) / c1, value);
	}

	/* float */
	class AverageFloat implements Interpolate {
		public double get(double x, double y, double z) {
			int x1 = (int)Math.floor(x);
			int y1 = (int)Math.floor(y);
			int z1 = (int)Math.floor(z);
			double xR = x1 + 1 - x;
			double yR = y1 + 1 - y;
			double zR = z1 + 1 - z;

			double v000 = getNoInterpolFloat(x1, y1, z1),
			v001 = getNoInterpolFloat(x1, y1, z1 + 1),
			v010 = getNoInterpolFloat(x1, y1 + 1, z1),
			v011 = getNoInterpolFloat(x1, y1 + 1, z1 + 1),
			v100 = getNoInterpolFloat(x1 + 1, y1, z1),
			v101 = getNoInterpolFloat(x1 + 1, y1, z1 + 1),
			v110 = getNoInterpolFloat(x1 + 1, y1 + 1, z1),
			v111 = getNoInterpolFloat(x1 + 1, y1 + 1, z1 + 1);

			double ret = xR * (yR * (zR * v000 + (1 - zR) * v001)
				+ (1 - yR) * (zR * v010 + (1 - zR) * v011))
				+ (1 - xR) * (yR * (zR * v100 + (1 - zR) * v101)
				+ (1 - yR) * (zR * v110 + (1 - zR) * v111));

			return ret;
		}
	}

	public float getNoCheckFloat(int x, int y, int z) {
		/* no check; we know exactly that it is inside */
		return pixelsFloat[z][x + w * y];
	}

	public float getNoInterpolFloat(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return 0;
		return getNoCheckFloat(x, y, z);
	}

	public void setFloat(int x, int y, int z, float value) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return;
		pixelsFloat[z][x + w * y] = value;
	}

	public InterpolatedImage cloneDimensionsOnly() {
		return cloneDimensionsOnly(image, image.getType());
	}

	public static InterpolatedImage cloneDimensionsOnly(ImagePlus ip,
			int type) {
		InterpolatedImage result = new InterpolatedImage();
		result.w = ip.getWidth();
		result.h = ip.getHeight();
		result.d = ip.getStack().getSize();

		switch (type) {
			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256:
				result.pixels = new byte[result.d][];
				break;
			case ImagePlus.GRAY32:
				result.pixelsFloat = new float[result.d][];
				break;
		}

		ImageStack stack = new ImageStack(result.w, result.h, null);
		for (int i = 0; i < result.d; i++)
			switch (type) {
				case ImagePlus.GRAY8:
				case ImagePlus.COLOR_256:
					result.pixels[i] =
						new byte[result.w * result.h];
					stack.addSlice("", result.pixels[i]);
					break;
				case ImagePlus.GRAY32:
					result.pixelsFloat[i] =
						new float[result.w * result.h];
					stack.addSlice("", result.pixelsFloat[i]);
					break;
			}

		result.image = new ImagePlus("", stack);
		result.image.setCalibration(ip.getCalibration());
		return result;
	}

	public InterpolatedImage cloneImage() {
		InterpolatedImage res = cloneDimensionsOnly();
		for (int k = 0; k < d; k++)
			switch (image.getType()) {
				case ImagePlus.GRAY8:
				case ImagePlus.COLOR_256:
					System.arraycopy(pixels[k], 0,
							res.pixels[k],
							0, w * h);
					break;
				case ImagePlus.GRAY32:
					System.arraycopy(pixelsFloat[k], 0,
							res.pixelsFloat[k],
							0, w * h);
					break;
			}
		return res;
	}
}

