package process3d;

import ij.process.ImageProcessor;
import ij.process.FloatProcessor;

import ij.ImageStack;
import ij.IJ;
import ij.ImagePlus;

import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Toolbar;

import ij.plugin.MacroInstaller;
import ij.plugin.PlugIn;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.LinkedList;

public class Flood_Fill implements PlugIn {
	
	private static boolean debug = false;
	private static int tol = 0;

	public static final String MACRO_CMD =
		"var leftClick=16, alt=9;\n" +
		"macro 'Flood Fill Tool - C111O11ffC100T6c0aF' {\n" +
		" while (true) {\n" +
		"  getCursorLoc(x, y, z, flags);\n" +
		"  if (flags&leftClick==0) exit();\n" +
		"  call('process3d.Flood_Fill.fill', x,y,z);\n" +
		"  exit();" + 
		" }\n" +
		"}\n" +
		"\n";
	
	public void run(String arg) {
		MacroInstaller installer = new MacroInstaller();
        	installer.install(MACRO_CMD);
	}

	public synchronized static void fill(String x, String y, String z) {
		fill(Integer.parseInt(x),
				Integer.parseInt(y),
				Integer.parseInt(z));
	}

	public synchronized static void fill(int sx, int sy, int sz) {
		fill(IJ.getImage(), sx, sy, sz, (byte)Toolbar.getForegroundColor().getRGB());
	}

	public synchronized static void fill(ImagePlus imp, int sx, int sy, int sz, byte color) {
		IJ.showStatus("Flood fill");
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		int wh = w * h;
		byte[][] b = new byte[d][];
		for(int z = 0; z < d; z++) {
			b[z] = (byte[])imp.getStack().
					getProcessor(z+1).getPixels();
		}
		int colorToFill = (int)(b[sz][sy * w + sx] & 0xff);

		Stack stack = new Stack();
		stack.push(sz * wh + sy * w + sx);
		while(!stack.isEmpty()) {
			int p = stack.pop();
			int pz = p / wh;
			int pi = p % wh;
			int py = pi / w;
			int px = pi % w;

			int by = (int)(b[pz][pi] & 0xff);
			if(Math.abs(colorToFill - by) > tol)
				continue;

			b[pz][pi] = color;

			int pzwh = pz * wh;
			if(pz > 0)     stack.push((pz - 1) * wh + pi);
			if(pz < d - 1) stack.push((pz + 1) * wh + pi);
			if(py > 0)     stack.push(pzwh + pi - w);
			if(py < h- 1)  stack.push(pzwh + pi + w);
			if(px > 0)     stack.push(pzwh + pi - 1);
			if(px < w - 1) stack.push(pzwh + pi + 1);
		}
		imp.updateAndDraw();
	}

	static final class Stack {
		private int[] array;
		private int size;
		
		Stack() {
			array = new int[1000000];
			size = 0;
		}

		public void push(int n) {
			if(size == array.length) {
				int[] tmp = new int[array.length + 1000000];
				System.arraycopy(array, 0, tmp, 0, array.length);
				array = tmp;
			}
			array[size] = n;
			size++;
		}

		public int pop() {
			size--;
			return array[size];
		}

		public int size() {
			return size;
		}

		public boolean isEmpty() {
			return size == 0;
		}
	}
}
