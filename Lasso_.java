import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.plugin.MacroInstaller;
import ij.plugin.PlugIn;

import java.util.TreeMap;

public class Lasso_ implements PlugIn {
	private byte[] pixels;
	private int[] dijkstra, previous;

	private int w, h;

	public static final String MACRO_CMD =
		"var clicked = 0;\n" +
		"var leftClick = 16;\n" +
		"\n" +
		"macro 'Lasso - C037T0d14<T7d14<' {\n" +
		"  while (true) {\n" +
		"    getCursorLoc(x, y, z, flags);\n" +
		"    if (flags & leftClick == 0) {\n" +
		"      if (!clicked) {\n" +
		"        clicked = 1;\n" +
		"        call('Lasso_.start', x, y);\n" +
		"      } else\n" +
		"        call('Lasso_.move', x, y);\n" +
		"    } else\n" +
		"      clicked = 0;\n" +
		"    }\n" +
		"    wait(10);\n" +
		"  }\n" +
		"}";

	public void run(String arg){
		if (IJ.versionLessThan("1.37c"))
			return;

		MacroInstaller installer = new MacroInstaller();
		installer.install(MACRO_CMD);
	}

	private static Lasso_ instance;

	public synchronized static void start(String x_, String y_) {
		if (instance == null)
			instance = new Lasso_();
		int x = (int)Float.parseFloat(x_);
		int y = (int)Float.parseFloat(y_);
		instance.initDijkstra(x, y);
	}

	public synchronized static void move(String x_, String y_) {
		int x = (int)Float.parseFloat(x_);
		int y = (int)Float.parseFloat(y_);
		instance.getDijkstra(x, y);
		int[] xPoints = new int[instance.w * instance.h];
		int[] yPoints = new int[instance.w * instance.h];
		int i = 0;
		do {
			xPoints[i] = x;
			yPoints[i] = y;
			i++;
			int j = instance.previous[x + instance.w * y];
			x = j % instance.w;
			y = j / instance.w;
		} while (x != instance.startX || y != instance.startY);
		Roi roi = new PolygonRoi(xPoints, yPoints, i,
				PolygonRoi.POLYGON);
		ImagePlus image = WindowManager.getCurrentImage();
		image.setRoi(roi);
		image.updateAndDraw();
	}

	final private int get(int x, int y) {
		return pixels[x + w * y] & 0xff;
	}

	private class PixelCost implements Comparable {
		int x, y, cost;

		public PixelCost(int x, int y, int cost) {
			this.x = x;
			this.y = y;
			this.cost = cost;
		}

		public int compareTo(Object other) {
			PixelCost o = (PixelCost)other;
			if (cost != o.cost)
				return cost - o.cost;
			if (x != o.x)
				return x - o.x;
			return y - o.y;
		}
	}

	private class PixelQueue extends TreeMap {
		public int x, y, cost;

		public int getNextCost() {
			if (size() > 0) {
				PixelCost c = (PixelCost)firstKey();
				return c.cost;
			}
			return Integer.MAX_VALUE;
		}

		public void push(int x, int y, int cost) {
			PixelCost c = new PixelCost(x, y, cost);
			put(c, c);
		}

		public boolean pop() {
			while (size() > 0) {
				PixelCost c = (PixelCost)firstKey();
				remove(c);
				if (dijkstra[c.x + w * c.y] > c.cost) {
					x = c.x;
					y = c.y;
					cost = c.cost;
					return true;
				}
			}
			return false;
		}
	}

	final static int[] stepX = { -1, 0, 1, 1, 1, 0, -1, -1 };
	final static int[] stepY = { -1, -1, -1, 0, 1, 1, 1, 0 };
	final static int[] stepW = { 4, 3, 4, 3, 4, 3, 4, 3 };

	PixelQueue queue;
	int startX, startY;

	private void initDijkstra(int x, int y) {
		ImagePlus image = WindowManager.getCurrentImage();
		if (image.getType() != ImagePlus.GRAY8) {
			IJ.error("Need an 8-bit grey image!");
			return;
		}
		ImageProcessor ip = image.getProcessor();
		w = ip.getWidth();
		h = ip.getHeight();

		pixels = (byte[])ip.getPixels();
		previous = new int[w * h];
		dijkstra = new int[w * h];
		for (int i = 0; i < w * h; i++)
			dijkstra[i] = Integer.MAX_VALUE;

		queue = new PixelQueue();
		queue.push(x, y, 0);
		startX = x;
		startY = y;
	}

	private void getDijkstra(int x_, int y_) {
		while (queue.getNextCost() < dijkstra[x_ + w * y_]
				&& queue.pop()) {
			int x = queue.x;
			int y = queue.y;
			int cost = queue.cost;
			dijkstra[x + w * y] = cost;
			int value = get(x, y);

			for (int i = 0; i < stepW.length; i++) {
				int x2 = x + stepX[i];
				int y2 = y + stepY[i];

				if (x2 < 0 || y2 < 0 || x2 >= w || y2 >= h)
					continue;
				int newC = cost + stepW[i]
					* Math.abs(get(x2, y2) - value);
				if (dijkstra[x2 + w * y2] > newC) {
					queue.push(x2, y2, newC);
					previous[x2 + w * y2] = x + w * y;
				}
			}
		}
	}
}
