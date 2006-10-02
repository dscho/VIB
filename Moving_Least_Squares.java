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
		gd.addChoice("method", methods, methods[1]);
		gd.addChoice("template", titles, titles[0]);
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
		ImagePlus templ = WindowManager.getImage(id);
		int w = templ.getWidth(), h = templ.getHeight();
		Interpolator inter = new BilinearInterpolator(ip);

		PointRoi points2 = (PointRoi)templ.getRoi();
		method.setCoordinates(points2, points1);

		ImageProcessor result = new FloatProcessor(w, h);
		float[] pixels = (float[])result.getPixels();
		method.warpImage(inter, w, h, pixels);

		result.setMinAndMax(ip.getMin(), ip.getMax());
		ImagePlus res = new ImagePlus("warped "
				+ image.getTitle(), result);
		res.setCalibration(templ.getCalibration());
		res.setRoi(points2);
		res.show();
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_16 | DOES_32 | NO_CHANGES;
	}

	static abstract class Method {
		int n;
		// the centroids
		float pCX, pCY, qCX, qCY;
		// these coordinates are relative to the centroid
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
			float totalP = 0, totalQ = 0;
			for (int i = 0; i < n; i++) {
				float w = w(x, y, pX[i], pY[i]);
				totalP += w;
				pCX += w * pX[i];
				pCY += w * pY[i];
				w = w(x, y, qX[i], qY[i]);
				totalQ += w;
				qCX += w * qX[i];
				qCY += w * qY[i];
			}
			pCX /= totalP;
			pCY /= totalP;
			qCX /= totalQ;
			qCY /= totalQ;
		}

		// x, y is supposed to be relative to pCX, pCY
		public abstract void calculateM(float x, float y);

		// resultX, resultY is absolute
		float resultX, resultY;

		// x, y is supposed to be absolute
		public void calculate(float x, float y) {
			try {
				calculateCentroids(x, y);
				calculateM(x, y);
				resultX = qCX + m11 * (x - pCX)
					+ m12 * (y - pCY);
				resultY = qCY + m21 * (x - pCX)
					+ m22 * (y - pCY);
			} catch(RuntimeException e) {
			}
		}

		public void warpImage(Interpolator inter,
				int w, int h, float[] pixels) {
			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					calculate(i, j);
					pixels[i + w * j] =
						inter.get(resultX, resultY);
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
				float pXi = pX[i] - pCX, pYi = pY[i] - pCY;
				float qXi = qX[i] - qCX, qYi = qY[i] - qCY;
				if (x == pX[i] && y == pY[i]) {
					resultX = qX[i];
					resultY = qY[i];
					throw new RuntimeException("");
				}
				float w = w(x, y, pX[i], pY[i]);
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
			m12 = (a22 * b12 - a12 * b22) / detA;
			m21 = (-a12 * b11 + a11 * b21) / detA;
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
				m12 += w * (-pXi * qYi + pYi * qXi);
				mu += w * (pXi * pXi + pYi +pYi);
			}
			m11 /= mu;
			m12 /= mu;
			m21 = m12;
			m22 = m11;
		}
	}

	static class Rigid extends Method {
		public void calculateM(float x, float y) {
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

