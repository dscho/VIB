import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.GenericDialog;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Toolbar;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import ij.plugin.MacroInstaller;
import ij.plugin.PlugIn;

import ij.plugin.filter.ThresholdToSelection;

import java.util.TreeMap;

public class Lasso_ implements PlugIn {
	private Difference difference;
	private double[] dijkstra;
	private int[] previous;

	private int w, h;
	private Roi originalRoi;

	/*
	 * Multiply the spatial distance with this factor before adding to
	 * the color distance.
	 */
	private double ratioSpaceColor = 1;

	public static final String MACRO_CMD =
		"var clicked = 0;\n" +
		"var spacePressed = 0;\n" +
		"var leftClick = 16;\n" +
		"var currentX = -1;\n" +
		"var currentY = -1;\n" +
		"\n" +
		"macro 'Lasso Tool - C000Pdaa79796a6c4c2a1613215276998a6a70' {\n" +
		"  tool = toolID();\n" +
		"  while (tool == toolID()) {\n" +
		"    if (!spacePressed) {\n" +
		"        if (isKeyDown('space'))\n" +
		"            spacePressed = 1;\n" +
		"    } else {\n" +
		"        if (!isKeyDown('space')) {\n" +
		"            spacePressed = 0;\n" +
		"            call('Lasso_.toggleMode');\n" +
		"        }\n" +
		"    }\n" +
		"    getCursorLoc(x, y, z, flags);\n" +
		"    if (!clicked) {\n" +
		"        if ((flags & leftClick) != 0) {\n" +
		"            clicked = 1;\n" +
		"            call('Lasso_.start', x, y);\n" +
		"        }\n" +
		"    } else {\n" +
		"        if ((flags & leftClick) == 0)\n" +
		"            clicked = 0;\n" +
		"        else if (x != currentX || y != currentY) {\n" +
		"            call('Lasso_.move', x, y);\n" +
		"            currentX = x;\n" +
		"            currentY = y;\n" +
		"        }\n" +
		"    }\n" +
		"    wait(100);\n" +
		"  }\n" +
		"}\n" +
		"\n" +
		"macro 'Lasso Tool Options' {\n" +
		"    call('Lasso_.callOptionDialog');\n" +
		"}\n";

	protected boolean macroInstalled = false;

	public void run(String arg){
		if (IJ.versionLessThan("1.37j"))
			return;

		if (macroInstalled)
			return;
		MacroInstaller installer = new MacroInstaller();
		installer.install(MACRO_CMD);
		Toolbar.getInstance().setTool(Toolbar.SPARE1);
		macroInstalled = true;
	}

	private static Lasso_ instance;

	private final static int BLOW = 0;
	private final static int LASSO = 1;
	private final static int MIN_LASSO = 2;
	private final static int MAX_TOOL = 2;
	private final static String[] modeTitles = {
		"Blow tool", "Lasso tool", "Lasso minimum tool"
	};
	private int mode = BLOW;

	public synchronized static void setMode(int mode) {
		if (instance == null)
			instance = new Lasso_();
		instance.mode = mode;
		IJ.showStatus(modeTitles[mode]);
	}

	public synchronized static void setMode(String mode) {
		if (mode.equals("lasso"))
			setMode(LASSO);
		else if (mode.equals("blow"))
			setMode(BLOW);
		else
			IJ.error("Unknown Lasso/Blow mode: " + mode);
	}

	public synchronized static void toggleMode() {
		int mode = instance == null ? 0 : instance.mode;
		setMode((mode + 1) % (MAX_TOOL + 1));
	}

	public synchronized static void callOptionDialog() {
		if (instance == null)
			instance = new Lasso_();
		instance.optionDialog();
	}

	public void optionDialog() {
		GenericDialog gd = new GenericDialog("Lasso Tool Options");
		gd.addChoice("mode", modeTitles, modeTitles[mode]);
		gd.addNumericField("ratio space/color", ratioSpaceColor, 2);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		mode = gd.getNextChoiceIndex();
		ratioSpaceColor = gd.getNextNumber();
	}

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
		if (x < 0) x = 0;
		if (x >= instance.w) x = instance.w - 1;
		if (y < 0) y = 0;
		if (y >= instance.h) y = instance.h - 1;
		try {
				if (instance.mode == BLOW)
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
				PolygonRoi.FREELINE);
		if (originalRoi != null)
			roi = new ShapeRoi(originalRoi).or(new ShapeRoi(roi));
		ImagePlus image = WindowManager.getCurrentImage();
		image.setRoi(roi);
		image.updateAndDraw();
	}

	private void moveBlow(int x, int y) {
		getDijkstra(x, y);
		FloatProcessor fp = new FloatProcessor(w, h, dijkstra);
		fp.setThreshold(Double.MIN_VALUE, dijkstra[x + w * y] + 1,
				ImageProcessor.NO_LUT_UPDATE);
		ImagePlus blowImage = new ImagePlus("blow", fp);
		ThresholdToSelection t2s = new ThresholdToSelection();
		t2s.setup("", blowImage);
		t2s.run(fp);
		Roi roi = blowImage.getRoi();
		if (originalRoi != null)
			roi = new ShapeRoi(originalRoi).or(new ShapeRoi(roi));
		ImagePlus image = WindowManager.getCurrentImage();
		image.setRoi(roi);
		image.updateAndDraw();
	}

	Difference getDifference(ImageProcessor ip) {
		if (mode == MIN_LASSO) {
			if (ip instanceof ByteProcessor)
				return new ByteMinValue((byte[])ip.getPixels());
			if (ip instanceof ColorProcessor)
				return new ColorMinValue((int[])ip.getPixels());
			return new MinValue(ip);
		}

		if (ip instanceof ByteProcessor)
			return new ByteDifference((byte[])ip.getPixels());
		if (ip instanceof ColorProcessor)
			return new ColorDifference((int[])ip.getPixels());
		return new Difference(ip);
	}

	private class Difference {
		ImageProcessor ip;

		Difference(ImageProcessor ip) {
			this.ip = ip;
		}

		double difference(int x0, int y0, int x1, int y1) {
			return Math.abs(ip.getPixelValue(x0, y0)
				- ip.getPixelValue(x1, y1));
		}
	}

	private class ByteDifference extends Difference {
		byte[] pixels;

		ByteDifference(byte[] pixels) {
			super(null);
			this.pixels = pixels;
		}

		final double difference(int x0, int y0, int x1, int y1) {
			return Math.abs((pixels[x0 + w * y0] & 0xff)
				- (pixels[x1 + w * y1] & 0xff));
		}
	}

	private class ColorDifference extends Difference {
		int[] pixels;

		ColorDifference(int[] pixels) {
			super(null);
			this.pixels = pixels;
		}

		final double difference(int x0, int y0, int x1, int y1) {
			int v0 = pixels[x0 + w * y0];
			int v1 = pixels[x1 + w * y1];
			int r = ((v1 >> 16) & 0xff) - ((v0 >> 16) & 0xff);
			int g = ((v1 >> 8) & 0xff) - ((v0 >> 8) & 0xff);
			int b = (v1 & 0xff) - (v0 & 0xff);
			return Math.abs(r) + Math.abs(g) + Math.abs(b);
		}
	}

	private class MinValue extends Difference {
		ImageProcessor ip;

		MinValue(ImageProcessor ip) {
			super(null);
			this.ip = ip;
		}

		double difference(int x0, int y0, int x1, int y1) {
			return ip.getPixelValue(x1, y1);
		}
	}

	private class ByteMinValue extends Difference {
		byte[] pixels;

		ByteMinValue(byte[] pixels) {
			super(null);
			this.pixels = pixels;
		}

		final double difference(int x0, int y0, int x1, int y1) {
			return pixels[x1 + w * y1] & 0xff;
		}
	}

	private class ColorMinValue extends Difference {
		int[] pixels;

		ColorMinValue(int[] pixels) {
			super(null);
			this.pixels = pixels;
		}

		final double difference(int x0, int y0, int x1, int y1) {
			int v1 = pixels[x1 + w * y1];
			int r = (v1 >> 16) & 0xff;
			int g = (v1 >> 8) & 0xff;
			int b = v1 & 0xff;
			return r + g + b; // TODO: use correct weighting
		}
	}

	private class PixelCost {
		int x, y;
		double cost;

		public PixelCost(int x, int y, double cost) {
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

	FibonacciHeapDouble queue;
	int startX, startY;

	private void initDijkstra(int x, int y) {
		ImagePlus image = WindowManager.getCurrentImage();
		originalRoi = IJ.shiftKeyDown() ? image.getRoi() : null;

		ImageProcessor ip = image.getProcessor();
		w = ip.getWidth();
		h = ip.getHeight();

		difference = getDifference(ip);
		previous = new int[w * h];
		previous[x + w * y] = x + w * y;
		dijkstra = new double[w * h];
		for (int i = 0; i < w * h; i++)
			dijkstra[i] = Double.MAX_VALUE;

		queue = new FibonacciHeapDouble();
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
			double cost = pixel.cost;
			if (dijkstra[x + w * y] <= cost)
				continue;

			dijkstra[x + w * y] = cost;
			for (int i = 0; i < stepW.length; i++) {
				int x2 = x + stepX[i];
				int y2 = y + stepY[i];

				if (x2 < 0 || y2 < 0 || x2 >= w || y2 >= h)
					continue;
				double newC = cost + stepW[i] + (ratioSpaceColor
					 * difference.difference(x, y, x2, y2));
				if (dijkstra[x2 + w * y2] > newC) {
					queue.add(newC, new PixelCost(x2,
								y2, newC));
					previous[x2 + w * y2] = x + w * y;
				}
			}
		}
	}
}
