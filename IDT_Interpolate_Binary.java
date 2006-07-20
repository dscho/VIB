import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.util.ArrayList;
import java.util.Iterator;
import math3d.Point3d;
import math3d.FastMatrixN;

/*
 * This plugin takes a binary stack as input, where some slices are
 * labeled (i.e. contain white regions), and some are not. The unlabaled
 * regions are interpolated by weighting the signed integer distance
 * transformed labeled slices.
 */

public class IDT_Interpolate_Binary implements PlugInFilter {
	ImagePlus image;
	int[][] idt;
	int w, h;

	public void run(ImageProcessor ip) {
		ImageStack stack = image.getStack();
		int sliceCount = stack.getSize();
		if (sliceCount < 3) {
			IJ.error("Too few slices to interpolate!");
			return;
		}

		IJ.showStatus("getting signed integer distance transform");
		w = ip.getWidth();
		h = ip.getHeight();
		idt = new int[sliceCount][];
		int first = sliceCount, last = -1;
ImageStack st2 = new ImageStack(w, h);
		for (int z = 0; z < sliceCount; z++) {
			idt[z] = getIDT(stack.getProcessor(z + 1).getPixels());
			if (idt[z] != null) {
				if (z < first)
					first = z;
				last = z;
			}
st2.addSlice("", new ij.process.FloatProcessor(w, h, idt[z] != null ? idt[z] : new int[w * h]));
		}
new ImagePlus("test", st2).show();

		if (first == last || last < 0) {
			IJ.error("Not enough to interpolate");
			return;
		}

		IJ.showStatus("calculating weights");
		int current = 0, next = first;
		for (int z = first; z < last; z++) {
			if (z == next) {
				current = z;
				for (next = z + 1; idt[next] == null; next++);
				continue;
			}

			byte[] p =
				(byte[])stack.getProcessor(z + 1).getPixels();
			for (int i = 0; i < w * h; i++)
				if (0 <= idt[current][i] * (next - z)
						+ idt[next][i] * (z - current))
					p[i] = (byte)255;
			IJ.showProgress(z - first + 1, last - z);
		}
	}

	/*
	 * This function calculates the signed integer distance transform.
	 * Distance transform means that each pixel is assigned the distance
	 * to the boundary.
	 * IDT means that the distance is not the Euclidean, but the minimal
	 * sum of neighbour distances with 3 for horizontal and neighbours,
	 * and 4 for diagonal neighbours (in 3d, the 3d diagonal neighbour
	 * would be 5).
	 * Signed means that the outside pixels have a negative sign.
	 */
	int[] getIDT(Object pixels) {
		byte[] p = (byte[])pixels;
		int[] result = new int[w * h];
		final int t = (w + h) * 9; // way too large
		int count = 0;

		for (int i = 0; i < result.length; i++)
			result[i] = t;

		for (int j = 0; j < h; j++)
			for (int i = 0; i < w; i++) {
				int idx = i + w * j;
				if (isBoundary(p, i, j)) {
					result[idx] = 0;
					count++;
				} else {
					int s = (p[idx] == 0 ? -1 : +1);
					int v0 = t, v1 = t, v2 = t;
					if (i > 0) {
						v0 = result[idx - 1] + 3 * s;
						if (j > 0)
							v2 = result[idx - 1 - w]
								+ 4 * s;
					}
					if (j > 0)
						v1 = result[idx - w] + 3 * s;
					if (Math.abs(v0) > Math.abs(v1))
						v0 = v1;
					if (Math.abs(v0) > Math.abs(v2))
						v0 = v2;
					result[idx] = v0;
				}
			}

		if (count == 0)
			return null;

		for (int j = h - 1; j >= 0; j--)
			for (int i = w - 1; i >= 0; i--) {
				int idx = i + w * j;
				int s = (p[idx] == 0 ? -1 : +1);
				int v0 = t, v1 = t, v2 = t;
				if (i < w - 1) {
					v0 = result[idx + 1] + 3 * s;
					if (j < h - 1)
						v2 = result[idx + 1 + w]
							+ 4 * s;
				}
				if (j < h - 1)
					v1 = result[idx + w] + 3 * s;
				if (Math.abs(v0) > Math.abs(v1))
					v0 = v1;
				if (Math.abs(v0) > Math.abs(v2))
					v0 = v2;
				if (Math.abs(v0) < Math.abs(result[idx]))
					result[idx] = v0;
			}

		for (int i = w - 1; i >= 0; i--)
			for (int j = h - 1; j >= 0; j--) {
				int idx = i + w * j;
				int s = (p[idx] == 0 ? -1 : +1);
				int v0 = t, v1 = t, v2 = t;
				if (i < w - 1) {
					v0 = result[idx + 1] + 3 * s;
					if (j < h - 1)
						v2 = result[idx + 1 + w]
							+ 4 * s;
				}
				if (j < h - 1)
					v1 = result[idx + w] + 3 * s;
				if (Math.abs(v0) > Math.abs(v1))
					v0 = v1;
				if (Math.abs(v0) > Math.abs(v2))
					v0 = v2;
				if (Math.abs(v0) < Math.abs(result[idx]))
					result[idx] = v0;
			}

		for (int i = 0; i < w; i++)
			for (int j = 0; j < h; j++) {
				int idx = i + w * j;
				int s = (p[idx] == 0 ? -1 : +1);
				int v0 = t, v1 = t, v2 = t;
				if (i > 0) {
					v0 = result[idx - 1] + 3 * s;
					if (j > 0)
						v2 = result[idx - 1 - w]
							+ 4 * s;
				}
				if (j > 0)
					v1 = result[idx - w] + 3 * s;
				if (Math.abs(v0) > Math.abs(v1))
					v0 = v1;
				if (Math.abs(v0) > Math.abs(v2))
					v0 = v2;
				if (Math.abs(v0) < Math.abs(result[idx]))
					result[idx] = v0;
			}

		return result;
	}

	final boolean isBoundary(byte[] pixels, int x, int y) {
		if (pixels[x + w * y] == 0)
			return false;
		if (x <= 0 || pixels[x - 1 + w * y] == 0)
			return true;
		if (x >= w - 1 || pixels[x + 1 + w * y] == 0)
			return true;
		if (y <= 0 || pixels[x + w * (y - 1)] == 0)
			return true;
		if (y >= h - 1 || pixels[x + w * (y + 1)] == 0)
			return true;
		if (x <= 0 || y <= 0 || pixels[x - 1 + w * (y - 1)] == 0)
			return true;
		if (x <= 0 || y >= h - 1 || pixels[x - 1 + w * (y + 1)] == 0)
			return true;
		if (x >= w - 1 || y <= 0 || pixels[x + 1 + w * (y - 1)] == 0)
			return true;
		if (x >= w - 1 || y >= h - 1 ||
				pixels[x + 1 + w * (y + 1)] == 0)
			return true;
		return false;
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G;
	}
}

