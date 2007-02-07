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
import ij.plugin.filter.ThresholdToSelection;

import java.util.TreeMap;

public class Lasso_ implements PlugIn {
	private byte[] pixels;
	private int[] dijkstra, previous;

	private int w, h;

	public static final String MACRO_CMD =
		"var clicked = 0;\n" +
		"var leftClick = 16;\n" +
		"var currentX = -1;\n" +
		"var currentY = -1;\n" +
		"\n" +
		"macro 'Lasso Tool - C037T0d14<T7d14<' {\n" +
		"  while (true) {\n" +
		"    getCursorLoc(x, y, z, flags);\n" +
		"    if ((flags & leftClick) != 0) {\n" +
		"        clicked = 1;\n" +
		"        call('Lasso_.start', x, y);\n" +
		"      } else if (clicked && (x != currentX || y != currentY)) {\n" +
		"        call('Lasso_.move', x, y);\n" +
		"        currentX = x;\n" +
		"        currentY = y;\n" +
		"      }\n" +
		"    }\n" +
		"    wait(100);\n" +
		"  }\n" +
		"}";

	public void run(String arg){
		if (IJ.versionLessThan("1.37c"))
			return;

		MacroInstaller installer = new MacroInstaller();
		installer.install(MACRO_CMD);
	}

	private static Lasso_ instance;
	private static boolean doBlowToolInstead = true;

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
try {
		if (doBlowToolInstead)
			instance.moveBlow(x, y);
		else
			instance.moveLasso(x, y);
} catch (Throwable t) {
	System.err.println("Caught throwable " + t);
	t.printStackTrace();
}
	}

	private void moveLasso(int x, int y) {
		getDijkstra(x, y);
		int[] xPoints = new int[w * h];
		int[] yPoints = new int[w * h];
		int i = 0;
		do {
			if (i >= w * h)
				break;
			xPoints[i] = x;
			yPoints[i] = y;
			i++;
			int j = previous[x + w * y];
			x = j % w;
			y = j / w;
		} while (x != startX || y != startY);
		Roi roi = new PolygonRoi(xPoints, yPoints, i,
				PolygonRoi.POLYGON);
		ImagePlus image = WindowManager.getCurrentImage();
		image.setRoi(roi);
		image.updateAndDraw();
	}

	private void moveBlow(int x, int y) {
//System.err.println("move to " + x + ", " + y);
		getDijkstra(x, y);
		FloatProcessor fp = new FloatProcessor(w, h, dijkstra);
		fp.setThreshold(Double.MIN_VALUE, dijkstra[x + w * y] + 1,
				ImageProcessor.NO_LUT_UPDATE);
//fp.setMinAndMax(Double.MIN_VALUE, dijkstra[x + w * y]);
		ImagePlus blowImage = new ImagePlus("blow", fp);
		ThresholdToSelection t2s = new ThresholdToSelection();
		t2s.setup("", blowImage);
		t2s.run(fp);
		ImagePlus image = WindowManager.getCurrentImage();
//blowImage.show();
		image.setRoi(blowImage.getRoi());
		image.updateAndDraw();
//System.err.println("set selection at " + x + ", " + y + ": " + image.getRoi());
	}

	final private int get(int x, int y) {
		return pixels[x + w * y] & 0xff;
	}

	private class PixelCost {
		int x, y, cost;

		public PixelCost(int x, int y, int cost) {
			this.x = x;
			this.y = y;
			this.cost = cost;
		}

		public String toString() {
			return "(" + x + ", " + y + ": " + cost + ")";
		}
	}

	final static int[] stepX = { -1, 0, 1, 1, 1, 0, -1, -1 };
	final static int[] stepY = { -1, -1, -1, 0, 1, 1, 1, 0 };
	final static int[] stepW = { 4, 3, 4, 3, 4, 3, 4, 3 };

	FibonacciHeapInt queue;
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
		previous[x + w * y] = x + w * y;
		dijkstra = new int[w * h];
		for (int i = 0; i < w * h; i++)
			dijkstra[i] = Integer.MAX_VALUE;

		queue = new FibonacciHeapInt();
		queue.add(0, new PixelCost(x, y, 0));
		startX = x;
		startY = y;
	}

	private void getDijkstra(int x_, int y_) {
		PixelCost pixel;
		while (queue.compareTo(dijkstra[x_ + w * y_]) < 0
				&& (pixel = (PixelCost)queue.pop()) != null) {
			int x = pixel.x;
			int y = pixel.y;
			int cost = pixel.cost;
			if (dijkstra[x + w * y] <= cost)
				continue;

			dijkstra[x + w * y] = cost;
			int value = get(x, y);

			for (int i = 0; i < stepW.length; i++) {
				int x2 = x + stepX[i];
				int y2 = y + stepY[i];

				if (x2 < 0 || y2 < 0 || x2 >= w || y2 >= h)
					continue;
				int newC = cost + stepW[i]
					+ (1 + Math.abs(get(x2, y2) - value));
				if (dijkstra[x2 + w * y2] > newC) {
					queue.add(newC, new PixelCost(x2,
								y2, newC));
					previous[x2 + w * y2] = x + w * y;
				}
			}
		}
	}
}
