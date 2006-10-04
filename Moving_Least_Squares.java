import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Rectangle;

/**
 * This plugin implements the algorithms from the paper
 * Image Deformation Using Moving Least Squares, Schaefer S. and McPhail T.
 * and Warrent J.
 * (http://faculty.cs.tamu.edu/schaefer/research/mls.pdf)
 */
public class Moving_Least_Squares implements PlugInFilter {
	ImagePlus image;

	public void run(ImageProcessor ip) {
		Roi roi = image.getRoi();
		if (!(roi instanceof PointRoi)) {
			IJ.error("Need a point selection!");
			return;
		}
		PointRoi points1 = (PointRoi)roi;
		int n = points1.getNCoordinates();

		int[] ids = WindowManager.getIDList();
		String[] titles = new String[ids.length];
		int j = 0;
		for (int i = 0; i < ids.length; i++) {
			ImagePlus other = WindowManager.getImage(ids[i]);
			if (other == image)
				continue;
			Roi other_roi = other.getRoi();
			if (!(other_roi instanceof PointRoi))
				continue;
			if (((PointRoi)other_roi).getNCoordinates() != n)
				continue;
			ids[j] = ids[i];
			titles[j] = other.getTitle();
			j++;
		}
		
		if (j < 1) {
			IJ.error("Need another image with " + n
					+ "selected points!");
			return;
		}

		int id;
		if (j < ids.length) {
			String[] temp = new String[j];
			System.arraycopy(titles, 0, temp, 0, j);
			titles = temp;
		}

		String[] methods = {"Affine", "Similarity", "Rigid"};
		GenericDialog gd = new GenericDialog("Align Images");
		gd.addChoice("method", methods, methods[2]);
		gd.addChoice("source", titles, titles[0]);
		gd.addNumericField("alpha", 1.0, 3);
		gd.addNumericField("gridSize", 0.0, 3);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		Method method;
		switch (gd.getNextChoiceIndex()) {
			case 0:
				method = new Affine(); break;
			case 1:
				method = new Similarity(); break;
			default:
				method = new Rigid(); break;
		}
		id = ids[gd.getNextChoiceIndex()];
		method.alpha = (float)gd.getNextNumber();
		float gridSize = (float)gd.getNextNumber();

		ImagePlus source = WindowManager.getImage(id);
		Interpolator inter =
			new BilinearInterpolator(source.getProcessor());

		PointRoi points2 = (PointRoi)source.getRoi();
		method.setCoordinates(points1, points2);

		int w = ip.getWidth(), h = ip.getHeight();
		method.warpImage(inter, w, h, ip.getPixels());
		if (gridSize > 0)
			method.drawGrid(w, h, gridSize, ip.getPixels());
		image.updateAndDraw();
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_16 | DOES_32;
	}

	static abstract class Method {
		int n;
		// the centroids
		float pCX, pCY, qCX, qCY;
		float[] pX, pY, qX, qY;

		public void setCoordinates(int[] x1, int[] y1,
				int[] x2, int[] y2, int n) {
			setCoordinates(x1, y1, 0, 0, x2, y2, 0, 0, n);
		}

		public void setCoordinates(PointRoi points1, PointRoi points2) {
			Rectangle r1 = points1.getBounds();
			Rectangle r2 = points2.getBounds();
			setCoordinates(points1.getXCoordinates(),
				points1.getYCoordinates(), r1.x, r1.y,
				points2.getXCoordinates(),
				points2.getYCoordinates(), r2.x, r2.y,
				points1.getNCoordinates());
		}

		public void setCoordinates(int[] x1, int[] y1, int oX1, int oY1,
				int[] x2, int[] y2, int oX2, int oY2, int n) {
			this.n = n;
			pX = new float[n];
			pY = new float[n];
			qX = new float[n];
			qY = new float[n];
			for (int i = 0; i < n; i++) {
				pX[i] = x1[i] + oX1;
				pY[i] = y1[i] + oY1;
				qX[i] = x2[i] + oX2;
				qY[i] = y2[i] + oY2;
			}
		}

		public float alpha = 1.0f;

		// both x, y and px, py are supposed to be absolute
		public float w(float x, float y, float px, float py) {
			x -= px;
			y -= py;
			if (x == 0 && y == 0)
				return 1e10f;
			x = 1 / (x * x + y * y);
			if (alpha == 1)
				return x;
			return (float)Math.exp(Math.log(x) * alpha);
		}

		float m11, m12, m21, m22;

		// x, y is supposed to be absolute
		public void calculateCentroids(float x, float y) {
			pCX = pCY = qCX = qCY = 0;
			float total = 0;
			for (int i = 0; i < n; i++) {
				float w = w(x, y, pX[i], pY[i]);
				total += w;
				pCX += w * pX[i];
				pCY += w * pY[i];
				qCX += w * qX[i];
				qCY += w * qY[i];
			}
			pCX /= total;
			pCY /= total;
			qCX /= total;
			qCY /= total;
		}

		public abstract void calculateM(float x, float y);

		// resultX, resultY is absolute
		float resultX, resultY;

		// x, y is supposed to be absolute
		public void calculate(float x, float y) {
			calculateCentroids(x, y);
			calculateM(x, y);
			resultX = qCX + m11 * (x - pCX)
				+ m12 * (y - pCY);
			resultY = qCY + m21 * (x - pCX)
				+ m22 * (y - pCY);
		}

		public void warpImage(Interpolator inter,
				int w, int h, Object pixels) {
			if (pixels instanceof byte[])
				warpImage(inter, w, h, (byte[])pixels);
			else if (pixels instanceof short[])
				warpImage(inter, w, h, (short[])pixels);
			else if (pixels instanceof float[])
				warpImage(inter, w, h, (float[])pixels);
			else
				IJ.error("Unknown pixel type");
		}

		public void warpImage(Interpolator inter,
				int w, int h, float[] pixels) {
			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					calculate(i, j);
					pixels[i + w * j] =
						//qCX;
						inter.get(resultX, resultY);
				}
				IJ.showProgress(j + 1, h);
			}
		}

		public void warpImage(Interpolator inter,
				int w, int h, short[] pixels) {
			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					calculate(i, j);
					pixels[i + w * j] = (short)
						inter.get(resultX, resultY);
				}
				IJ.showProgress(j + 1, h);
			}
		}

		public void warpImage(Interpolator inter,
				int w, int h, byte[] pixels) {
			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					calculate(i, j);
					pixels[i + w * j] = (byte)
						inter.get(resultX, resultY);
				}
				IJ.showProgress(j + 1, h);
			}
		}

		public void drawGrid(int w, int h, float step, Object pixels) {
			if (pixels instanceof byte[])
				drawGrid(w, h, step, (byte[])pixels);
			else if (pixels instanceof short[])
				drawGrid(w, h, step, (short[])pixels);
			else if (pixels instanceof float[])
				drawGrid(w, h, step, (float[])pixels);
			else
				IJ.error("Unknown pixel type");
		}

		boolean gridCondition(int i, int j, float step) {
			float x0, y0, x1, y1, x2, y2, x3, y3;
			x0 = (float)Math.floor(resultX / step);
			y0 = (float)Math.floor(resultY / step);
			calculate(i, j - 0.5f);
			x1 = (float)Math.floor(resultX / step);
			y1 = (float)Math.floor(resultY / step);
			calculate(i, j + 0.5f);
			x2 = (float)Math.floor(resultX / step);
			y2 = (float)Math.floor(resultY / step);
			calculate(i + 0.5f, j);
			x3 = (float)Math.floor(resultX / step);
			y3 = (float)Math.floor(resultY / step);
			return x0 != x1 || x0 != x2 || x0 != x3 || y0 != y1 ||
				y0 != y2 || y0 != y3;
		}

		public void drawGrid(int w, int h, float step, float[] pixels) {
			for (int j = 0; j < h; j++) {
					calculate(-0.5f, j - 0.5f);
				for (int i = 0; i < w; i++) {
					if (gridCondition(i, j, step))
						pixels[i + w * j] = 0;
				}
				IJ.showProgress(j + 1, h);
			}
		}

		public void drawGrid(int w, int h, float step, short[] pixels) {
			for (int j = 0; j < h; j++) {
					calculate(-0.5f, j - 0.5f);
				for (int i = 0; i < w; i++) {
					if (gridCondition(i, j, step))
						pixels[i + w * j] = 0;
				}
				IJ.showProgress(j + 1, h);
			}
		}

		public void drawGrid(int w, int h, float step, byte[] pixels) {
			for (int j = 0; j < h; j++) {
					calculate(-0.5f, j - 0.5f);
				for (int i = 0; i < w; i++) {
					if (gridCondition(i, j, step))
						pixels[i + w * j] = 0;
				}
				IJ.showProgress(j + 1, h);
			}
		}
	}

	static class Affine extends Method {
		public void calculateM(float x, float y) {
			float a11, a12, a22;
			float b11, b12, b21, b22;
			a11 = a12 = a22 = b11 = b12 = b21 = b22 = 0;
			for (int i = 0; i < n; i++) {
				float w = w(x, y, pX[i], pY[i]);
				float pXi = pX[i] - pCX, pYi = pY[i] - pCY;
				float qXi = qX[i] - qCX, qYi = qY[i] - qCY;
				a11 += w * pXi * pXi;
				a12 += w * pXi * pYi;
				a22 += w * pYi * pYi;
				b11 += w * pXi * qXi;
				b12 += w * pXi * qYi;
				b21 += w * pYi * qXi;
				b22 += w * pYi * qYi;
			}
			float detA = a11 * a22 - a12 * a12;
			m11 = (a22 * b11 - a12 * b21) / detA;
			m12 = (-a12 * b11 + a11 * b21) / detA;
			m21 = (a22 * b12 - a12 * b22) / detA;
			m22 = (-a12 * b12 + a11 * b22) / detA;
		}
	}

	static class Similarity extends Method {
		public void calculateM(float x, float y) {
			float mu = 0;
			m11 = m12 = m21 = m22 = 0;
			for (int i = 0; i < n; i++) {
				float w = w(x, y, pX[i], pY[i]);
				float pXi = pX[i] - pCX, pYi = pY[i] - pCY;
				float qXi = qX[i] - qCX, qYi = qY[i] - qCY;
				m11 += w * (pXi * qXi + pYi * qYi);
				m12 += w * (pYi * qXi - pXi * qYi);
				mu += w * (pXi * pXi + pYi * pYi);
			}
			m11 /= mu;
			m12 /= mu;
			m21 = -m12;
			m22 = m11;
		}
	}

	static class Rigid extends Method {
		public void calculateM(float x, float y) {
			float mu1 = 0, mu2 = 0;
			m11 = m12 = m21 = m22 = 0;
			for (int i = 0; i < n; i++) {
				float w = w(x, y, pX[i], pY[i]);
				float pXi = pX[i] - pCX, pYi = pY[i] - pCY;
				float qXi = qX[i] - qCX, qYi = qY[i] - qCY;
				m11 += w * (pXi * qXi + pYi * qYi);
				m12 += w * (pYi * qXi - pXi * qYi);
			}
			float mu = (float)Math.sqrt(m11 * m11 + m12 * m12);
			m11 /= mu;
			m12 /= mu;
			m21 = -m12;
			m22 = m11;
		}
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

