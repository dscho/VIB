import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.ImageProcessor;
import ij.plugin.filter.PlugInFilter;

import java.awt.Point;
import java.awt.geom.GeneralPath;

import java.util.ArrayList;

public class Visual_Grep implements PlugInFilter {
	ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_RGB | NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		int[] ids = WindowManager.getIDList();
		String[] idList = new String[ids.length];
		for (int i = 0; i < ids.length; i++)
			idList[i] = WindowManager.getImage(ids[i]).getTitle();

		int level = 0, w = ip.getWidth();
		while (w > 200) {
			w /= 2;
			level++;
		}

		GenericDialog gd = new GenericDialog("Visual Grep");
		gd.addChoice("needle", idList, idList[0]);
		gd.addNumericField("tolerance", 3000, 0);
		gd.addNumericField("pyramidLevel", level, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		int needleIndex = ids[gd.getNextChoiceIndex()];
		ImagePlus needle = WindowManager.getImage(needleIndex);
		int tolerance = (int)gd.getNextNumber();
		level = (int)gd.getNextNumber();

		ArrayList points = getPoints(ip, needle.getProcessor(),
				tolerance, level, level, null);
		Roi roi = getRoi(points,
				needle.getWidth(), needle.getHeight());
		imp.setRoi(roi);
		imp.updateAndDraw();
	}

	ArrayList getPoints(ImageProcessor haystack, ImageProcessor needle,
			int tolerance, int level, int totalLevel,
			ArrayList initial) {
		int w = haystack.getWidth(), h = haystack.getHeight();
		int needleW = needle.getWidth(), needleH = needle.getHeight();
		ArrayList points = new ArrayList();
		int[] pixels = (int[])haystack.getPixels();
		int[] needlePixels = (int[])needle.getPixels();

		if (level > 0) {
			ArrayList scaledInitial = null;
			if (initial != null) {
				scaledInitial = new ArrayList();
				for (int i = 0; i < initial.size(); i++) {
					Point p = (Point)initial.get(i);
					Point p2 = new Point(p.x / 2, p.y / 2);
					scaledInitial.add(p2);
				}
			}
			ImageProcessor scaledHaystack =
				haystack.resize(w / 2, h / 2);
			ImageProcessor scaledNeedle =
				needle.resize(needleW / 2, needleH / 2);
			initial = getPoints(scaledHaystack, scaledNeedle,
					tolerance, level - 1, totalLevel,
					scaledInitial);

			for (int i = 0; i < initial.size(); i++) {
				Point p = (Point)initial.get(i);
				p.x *= 2;
				p.y *= 2;
				getPoints(points, pixels, w,
						needlePixels, needleW, needleH,
						p.x, p.y, p.x + 2, p.y + 2,
						tolerance, false);
			}
		} else
			// exhaustive
			getPoints(points, pixels, w,
					needlePixels, needleW, needleH,
					0, 0, w - needleW, h - needleH,
					tolerance, true);

		IJ.showProgress(level + 1, totalLevel);

		return points;
	}

	// returns a list of matches in the boundingBox (x1,y1,x2,y2)

	void getPoints(ArrayList points, int[] pixels, int row,
			int[] needle, int needleW, int needleH,
			int x1, int y1, int x2, int y2,
			int tolerance, boolean showProgress) {
		for (int y = y1; y < y2; y++) {
			for (int x = x1; x < x2; x++)
				if (distance(pixels, x, y, row, needle,
							needleW, needleH)
						< tolerance)
					points.add(new Point(x, y));
			if (showProgress)
				IJ.showProgress(y - y1 + 1, y2 - y1);
		}
	}

	float distance(int[] haystack, int x, int y, int row,
			int[] needle, int needleW, int needleH) {
		long diff = 0;
		for (int j = 0; j < needleH; j++)
			for (int i = 0; i < needleW; i++) {
				int v1 = haystack[x + i + row * (y + j)];
				int v2 = needle[i + needleW * j];
				int r = ((v1 >> 16) & 0xff)
					- ((v2 >> 16) & 0xff);
				int g = ((v1 >> 8) & 0xff)
					- ((v2 >> 8) & 0xff);
				int b = (v1 & 0xff) - (v2 & 0xff);
				diff += r * r + g * g + b * b;
			}
		return diff / (float)(needleW * needleH);
	}

	Roi getRoi(ArrayList points, int w, int h) {
		if (points.size() < 0)
			return null;

		if (points.size() == 1) {
			Point p = (Point)points.get(0);
			return new Roi(p.x, p.y, w, h);
		}

		GeneralPath gp = new GeneralPath();
		for (int i = 0; i < points.size(); i++) {
			Point p = (Point)points.get(i);
			gp.moveTo(p.x, p.y);
			gp.lineTo(p.x + w, p.y);
			gp.lineTo(p.x + w, p.y + h);
			gp.lineTo(p.x, p.y + h);
			gp.closePath();
		}

		return new ShapeRoi(gp);
	}
}
