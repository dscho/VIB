package process3d;

import ij.text.TextWindow;
import ij.gui.GenericDialog;
import ij.WindowManager;

import ij.ImageStack;
import ij.ImagePlus;
import ij.IJ;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;

import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class Find_Minima implements PlugInFilter {

	private ImagePlus image;
	private int w, h, d;
	private byte[][] data;
	private byte[][] minima;

	public Find_Minima() {}

	public Find_Minima(ImagePlus imp) {
		this.init(imp);
	}

	public void init(ImagePlus imp) {
		this.image = imp;
		w = imp.getWidth();
		h = imp.getHeight();
		d = imp.getStackSize();
		data = new byte[d][];
		for(int z = 0; z < d; z++)
			data[z] = (byte[])imp.getStack().getPixels(z+1);
	}

	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_8G;
	}

	public void run(ImageProcessor ip) {
		classify().show();
	}


	public ImagePlus classify() {
		minima = new byte[d][w * h];
		ImageStack stack = new ImageStack(w, h);

		for(int z = 0; z < d; z++) {
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					classifyPixel(x, y, z);
				}
			}
			stack.addSlice("", minima[z]);
			IJ.showProgress(z, d);
		}

		ImagePlus ret = new ImagePlus("Minima", stack);
		ret.setCalibration(image.getCalibration());
		return ret;
	}

	private void classifyPixel(int x, int y, int z) {
		int v = getD(x, y, z);
		int min = isMinimum(x, y, z, v);

		if(min == NO_MIN) {
			for(int i = 0; i < 13; i++) {
				int ax = i % 3 - 1 + x;
				int ay = (i / 3) % 3 - 1 + y;
				int az = i / 9 - 1 + z;
				int av = getD(ax, ay, az);
				byte ci = getC(ax, ay, az);

				if(ci == LABEL && av > v)
					unlabel(ax, ay, az);
			}
		} else if(min == STRICT_MIN) {
			for(int i = 0; i < 13; i++) {
				int ax = i % 3 - 1 + x;
				int ay = (i / 3) % 3 - 1 + y;
				int az = i / 9 - 1 + z;
				int av = getD(ax, ay, az);

				byte ci = getC(ax, ay, az);

				if(ci == LABEL)
					unlabel(ax, ay, az);
			}
			label(x, y, z);
		} else if(min == NON_STRICT_MIN) {
			for(int i = 0; i < 13; i++) {
				int ax = i % 3 - 1 + x;
				int ay = (i / 3) % 3 - 1 + y;
				int az = i / 9 - 1 + z;
				int av = getD(ax, ay, az);

				byte ci = getC(ax, ay, az);

				if(ci == LABEL && av > v)
					unlabel(ax, ay, az);
			}
			label(x, y, z);
		}
	}


	private static final int NO_MIN = 0;
	private static final int NON_STRICT_MIN = 1;
	private static final int STRICT_MIN = 2;

	private static final byte LABEL = (byte)255;
	private static final byte CLEAR = (byte)0;

	private final int isMinimum(int x, int y, int z, int v) {
		for(int i = 0; i < 13; i++) {
			if(getD(i % 3 - 1 + x, (i / 3) % 3 - 1 + y, i / 9 - 1 + z) < v)
				return NO_MIN;
		}
		for(int i = 0; i < 13; i++) {
			if(getD(i % 3 - 1 + x, (i / 3) % 3 - 1 + y, i / 9 - 1 + z) == v)
				return NON_STRICT_MIN;
		}
		return STRICT_MIN;
	}

	private final int getD(int x, int y, int z) {
		if(x >= 0 && x < w && y >= 0 && y < h && z >= 0 && z < d)
			return (int)(data[z][y * w + x] & 0xff);
		return 0;
	}

	private final byte getC(int x, int y, int z) {
		if(x >= 0 && x < w && y >= 0 && y < h && z >= 0 && z < d)
			return minima[z][y * w + x];
		return -1;
	}

	private final void label(int x, int y, int z) {
		minima[z][y * w + x] = LABEL;
	}

	private final void unlabel(int x, int y, int z) {
		Stack<Point> s = new Stack<Point>();
		s.push(new Point(x, y, z));
		while (!s.isEmpty()){
			Point n = s.pop();
			if ((n.x >= 0) && (n.x < w) &&
				(n.y >= 0) && (n.y < h) &&
				(n.z >= 0) && (n.z < d) &&
				minima[n.z][n.y * w + n.x] == LABEL) {

				minima[n.z][n.y * w + n.x] = CLEAR;
				s.push(new Point(n.x - 1, n.y - 1, n.z - 1));
				s.push(new Point(n.x    , n.y - 1, n.z - 1));
				s.push(new Point(n.x + 1, n.y - 1, n.z - 1));
				s.push(new Point(n.x - 1, n.y    , n.z - 1));
				s.push(new Point(n.x    , n.y    , n.z - 1));
				s.push(new Point(n.x + 1, n.y    , n.z - 1));
				s.push(new Point(n.x - 1, n.y + 1, n.z - 1));
				s.push(new Point(n.x    , n.y + 1, n.z - 1));
				s.push(new Point(n.x + 1, n.y + 1, n.z - 1));
				s.push(new Point(n.x - 1, n.y - 1, n.z    ));
				s.push(new Point(n.x    , n.y - 1, n.z    ));
				s.push(new Point(n.x + 1, n.y - 1, n.z    ));
				s.push(new Point(n.x - 1, n.y    , n.z    ));
				s.push(new Point(n.x + 1, n.y    , n.z    ));
				s.push(new Point(n.x - 1, n.y + 1, n.z    ));
				s.push(new Point(n.x    , n.y + 1, n.z    ));
				s.push(new Point(n.x + 1, n.y + 1, n.z    ));
				s.push(new Point(n.x - 1, n.y - 1, n.z + 1));
				s.push(new Point(n.x    , n.y - 1, n.z + 1));
				s.push(new Point(n.x + 1, n.y - 1, n.z + 1));
				s.push(new Point(n.x - 1, n.y    , n.z + 1));
				s.push(new Point(n.x    , n.y    , n.z + 1));
				s.push(new Point(n.x + 1, n.y    , n.z + 1));
				s.push(new Point(n.x - 1, n.y + 1, n.z + 1));
				s.push(new Point(n.x    , n.y + 1, n.z + 1));
				s.push(new Point(n.x + 1, n.y + 1, n.z + 1));
			}
		}
	}

	private final static class Point {
		private int x, y, z;
		
		Point(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
}

