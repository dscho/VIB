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

		for (int z = 0; z < sliceCount; z++) {
			idt[z] = getIDT(stack.getProcessor(z + 1).getPixels());
			if (idt[z] != null) {
				if (z < first)
					first = z;
				last = z;
			}
		 }

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
	 * The following calculates the signed integer distance transform.
	 * Distance transform means that each pixel is assigned the distance
	 * to the boundary.
	 * IDT means that the distance is not the Euclidean, but the minimal
	 * sum of neighbour distances with 3 for horizontal and neighbours,
	 * and 4 for diagonal neighbours (in 3d, the 3d diagonal neighbour
	 * would be 5).
	 * Signed means that the outside pixels have a negative sign.
	 */
	class IDT {
		int[] result;

		IDT() {
			result = new int[w * h];
			int infinity = (w + h) * 9;

			for (int i = 0; i < result.length; i++)
				result[i] = infinity;
		}

		int init(byte[] p) {
			int count = 0;

			for (int j = 0; j < h; j++)
				for (int i = 0; i < w; i++) {
					int idx = i + w * j;
					if (isBoundary(p, i, j)) {
						result[idx] = 0;
						count++;
					} else if (isJustOutside(p, i, j))
						result[idx] = -1;
				}
			return count;
		}

		final void idt(int x, int y, int dx, int dy) {
			if (x + dx < 0 || y + dy < 0 ||
					x + dx >= w || y + dy >= h)
				return;
			int value = result[x + dx + w * (y + dy)];
			int distance = (dx == 0 || dy == 0 ? 3 : 4);
			value += distance * (value < 0 ? -1 : 1);
			if (Math.abs(result[x + w * y]) > Math.abs(value))
				result[x + w * y] = value;
		}

		void propagate() {
			for (int j = 0; j < h; j++)
				for (int i = 0; i < w; i++) {
					idt(i, j, -1, 0);
					idt(i, j, -1, -1);
					idt(i, j, 0, -1);
				}

			for (int j = h - 1; j >= 0; j--)
				for (int i = w - 1; i >= 0; i--) {
					idt(i, j, +1, 0);
					idt(i, j, +1, +1);
					idt(i, j, 0, +1);
				}

			for (int i = w - 1; i >= 0; i--)
				for (int j = h - 1; j >= 0; j--) {
					idt(i, j, +1, 0);
					idt(i, j, +1, +1);
					idt(i, j, 0, +1);
				}

			for (int i = 0; i < w; i++)
				for (int j = 0; j < h; j++) {
					idt(i, j, -1, 0);
					idt(i, j, -1, -1);
					idt(i, j, 0, -1);
				}
		}
	}

	int[] getIDT(Object pixels) {
		IDT idt = new IDT();
		if (idt.init((byte[])pixels) == 0)
			return null;
		idt.propagate();
		return idt.result;
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

	final boolean isJustOutside(byte[] pixels, int x, int y) {
		if (pixels[x + w * y] != 0)
			return false;
		if (x > 0 && pixels[x - 1 + w * y] != 0)
			return true;
		if (x < w - 1 && pixels[x + 1 + w * y] != 0)
			return true;
		if (y > 0 && pixels[x + w * (y - 1)] != 0)
			return true;
		if (y < h - 1 && pixels[x + w * (y + 1)] != 0)
			return true;
		if (x > 0 && y > 0 && pixels[x - 1 + w * (y - 1)] != 0)
			return true;
		if (x > 0 && y < h - 1 && pixels[x - 1 + w * (y + 1)] != 0)
			return true;
		if (x < w - 1 && y > 0 && pixels[x + 1 + w * (y - 1)] != 0)
			return true;
		if (x < w - 1 && y < h - 1 &&
				pixels[x + 1 + w * (y + 1)] != 0)
			return true;
		return false;
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G;
	}
}

