import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.plugin.filter.PlugInFilter;

import java.util.TreeMap;

public class Lasso_ implements PlugInFilter{
	private byte[] pixels;
	private int[] dijkstra;

	private int w, h;

	public void run(ImageProcessor ip){
		w = ip.getWidth();
		h = ip.getHeight();

		pixels = (byte[])ip.getPixels();
		initDijkstra(100, 100);
		getDijkstra(120, 150);
		new ImagePlus("Dijkstra", new FloatProcessor(w, h, dijkstra)).show();
	}

	final int get(int x, int y) {
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

	private void initDijkstra(int x, int y) {
		dijkstra = new int[w * h];
		for (int i = 0; i < w * h; i++)
			dijkstra[i] = Integer.MAX_VALUE;

		queue = new PixelQueue();
		queue.push(x, y, 0);
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
				if (dijkstra[x2 + w * y2] > newC)
					queue.push(x2, y2, newC);
			}
		}
	}

	public int setup(String arg, ImagePlus img){
		return DOES_8G | NO_CHANGES;
	}
}
