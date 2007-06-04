package process3d;

import ij.process.ImageProcessor;
import ij.process.FloatProcessor;

import ij.ImageStack;
import ij.IJ;
import ij.ImagePlus;

import ij.gui.Roi;
import ij.gui.ShapeRoi;

import ij.plugin.MacroInstaller;
import ij.plugin.PlugIn;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.LinkedList;

public class Flood_Fill implements PlugIn {
	
	private static boolean debug = false;

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

	public synchronized static void fill(int x, int y, int z) {
		Point p = new Point(x, y, z);
		Vector v = new Vector();
		v.add(p);
		fill(v);
	}

	public synchronized static void fill(Vector seedpoints) {
		ImagePlus imp = IJ.getImage();
		IJ.showStatus("Flood fill");
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		byte[][] b = new byte[d][];
		for(int z = 0; z < d; z++) {
			b[z] = (byte[])imp.getStack().
					getProcessor(z+1).getPixels();
		}

		LinkedList queue = new LinkedList();
		queue.addAll(seedpoints);
		while(!queue.isEmpty()) {
			IJ.showProgress(1, queue.size());
			Point p = (Point)queue.removeLast();
			if(b[p.z][p.y*w+p.x] != (byte)255) {
				continue;
			}
			b[p.z][p.y*w+p.x] = (byte)100;

			if(p.x>0)   queue.addFirst(new Point(p.x-1,p.y,p.z));
			if(p.x<w-1) queue.addFirst(new Point(p.x+1,p.y,p.z));
			if(p.y>0)   queue.addFirst(new Point(p.x,p.y-1,p.z));
			if(p.y<h-1) queue.addFirst(new Point(p.x,p.y+1,p.z));
			if(p.z>0)   queue.addFirst(new Point(p.x,p.y,p.z-1));
			if(p.z<d-1) queue.addFirst(new Point(p.x,p.y,p.z+1));
		}
		imp.updateAndDraw();
	}

	private static class Point {
		int x, y, z;
		public Point(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
}
